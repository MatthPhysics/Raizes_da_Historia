package br.com.raizesdahistoria.vendas.internal.pix;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.ssl.SslBundles;
import org.springframework.boot.http.client.ClientHttpRequestFactoryBuilder;
import org.springframework.boot.http.client.ClientHttpRequestFactorySettings;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Integração com PSP real seguindo a API Pix do Bacen ({@code PUT/GET/PATCH /v2/cob/{txid}}, OAuth2
 * client-credentials e mTLS via SSL bundle). Ativada com {@code pix.provedor=psp}.
 * <p>
 * <b>Esqueleto:</b> o PSP ainda não foi definido; revisar URLs, escopos e formato de erro contra a documentação
 * do PSP escolhido antes de habilitar em produção.
 */
@Component
@ConditionalOnProperty(name = "pix.provedor", havingValue = "psp")
class PspPixGateway implements PixGateway {

    private static final Logger log = LoggerFactory.getLogger(PspPixGateway.class);
    private static final Duration TIMEOUT = Duration.ofSeconds(5);

    private final RestClient client;
    private final PspProperties psp;
    private final PixProperties pix;
    private volatile String token;
    private volatile Instant tokenExpiraEm = Instant.EPOCH;

    PspPixGateway(RestClient.Builder builder, PspProperties psp, PixProperties pix, SslBundles sslBundles) {
        this.psp = psp;
        this.pix = pix;
        // Timeouts curtos: a cobrança é criada dentro da transação do checkout, que segura locks de estoque
        var settings = ClientHttpRequestFactorySettings.defaults()
                .withConnectTimeout(TIMEOUT)
                .withReadTimeout(TIMEOUT);
        if (psp.sslBundle() != null && !psp.sslBundle().isBlank()) {
            settings = settings.withSslBundle(sslBundles.getBundle(psp.sslBundle()));
        }
        this.client = builder.baseUrl(psp.baseUrl())
                .requestFactory(ClientHttpRequestFactoryBuilder.detect().build(settings))
                .build();
    }

    @Override
    public CobrancaCriada criarCobranca(String txid, BigDecimal valor, Duration expiracao, Devedor devedor) {
        var corpo = new java.util.LinkedHashMap<String, Object>();
        corpo.put("calendario", Map.of("expiracao", expiracao.toSeconds()));
        if (devedor.cpf() != null) {
            corpo.put("devedor", Map.of("cpf", devedor.cpf(), "nome", devedor.nome()));
        }
        corpo.put("valor", Map.of("original", valor.setScale(2, RoundingMode.HALF_EVEN).toPlainString()));
        corpo.put("chave", pix.chave());
        corpo.put("solicitacaoPagador", "Pedido Raízes da História");
        try {
            var resposta = client.put().uri("/v2/cob/{txid}", txid)
                    .headers(h -> h.setBearerAuth(token()))
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(corpo)
                    .retrieve()
                    .body(CobResponse.class);
            if (resposta == null || resposta.pixCopiaECola() == null) {
                throw new PspIndisponivelException("Resposta do PSP sem pixCopiaECola", null);
            }
            return new CobrancaCriada(txid, resposta.pixCopiaECola());
        } catch (RestClientException e) {
            log.error("Falha ao criar cobrança no PSP (txid {})", txid, e);
            throw new PspIndisponivelException("Falha ao criar cobrança", e);
        }
    }

    @Override
    public Optional<CobrancaConsultada> consultarCobranca(String txid) {
        try {
            var resposta = client.get().uri("/v2/cob/{txid}", txid)
                    .headers(h -> h.setBearerAuth(token()))
                    .retrieve()
                    .body(CobResponse.class);
            if (resposta == null) {
                return Optional.empty();
            }
            return Optional.of(new CobrancaConsultada(txid, StatusCobranca.valueOf(resposta.status()),
                    new BigDecimal(resposta.valor().original())));
        } catch (RestClientResponseException e) {
            if (e.getStatusCode().isSameCodeAs(HttpStatus.NOT_FOUND)) {
                return Optional.empty();
            }
            throw new PspIndisponivelException("Falha ao consultar cobrança", e);
        } catch (RestClientException e) {
            throw new PspIndisponivelException("Falha ao consultar cobrança", e);
        }
    }

    @Override
    public void removerCobranca(String txid) {
        try {
            client.patch().uri("/v2/cob/{txid}", txid)
                    .headers(h -> h.setBearerAuth(token()))
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("status", StatusCobranca.REMOVIDA_PELO_USUARIO_RECEBEDOR.name()))
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientException e) {
            log.warn("Não foi possível remover a cobrança {} no PSP", txid, e);
        }
    }

    private synchronized String token() {
        if (token != null && Instant.now().isBefore(tokenExpiraEm.minusSeconds(30))) {
            return token;
        }
        var form = new LinkedMultiValueMap<String, String>();
        form.add("grant_type", "client_credentials");
        var resposta = client.post().uri(psp.tokenPath())
                .headers(h -> h.setBasicAuth(psp.clientId(), psp.clientSecret()))
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form)
                .retrieve()
                .body(TokenResponse.class);
        if (resposta == null || resposta.accessToken() == null) {
            throw new PspIndisponivelException("PSP não retornou token", null);
        }
        token = resposta.accessToken();
        tokenExpiraEm = Instant.now().plusSeconds(resposta.expiresIn());
        return token;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record CobResponse(String txid, String status, String pixCopiaECola, Valor valor) {
        @JsonIgnoreProperties(ignoreUnknown = true)
        record Valor(String original) {
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record TokenResponse(@JsonProperty("access_token") String accessToken,
            @JsonProperty("expires_in") long expiresIn) {
    }

    @ConfigurationProperties("pix.psp")
    record PspProperties(String baseUrl, String tokenPath, String clientId, String clientSecret, String sslBundle) {
    }
}
