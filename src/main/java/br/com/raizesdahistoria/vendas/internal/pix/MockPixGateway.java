package br.com.raizesdahistoria.vendas.internal.pix;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * PSP simulado (profiles dev/test): gera BR Code de teste e guarda as cobranças em memória.
 * O pagamento é simulado via {@code POST /api/v1/dev/pix/{txid}/simular-pagamento} (somente profile dev).
 */
@Component
@ConditionalOnProperty(name = "pix.provedor", havingValue = "mock", matchIfMissing = true)
public class MockPixGateway implements PixGateway {

    private static final Logger log = LoggerFactory.getLogger(MockPixGateway.class);

    private final PixProperties props;
    private final Map<String, CobrancaConsultada> cobrancas = new ConcurrentHashMap<>();

    public MockPixGateway(PixProperties props) {
        this.props = props;
        log.warn("PSP Pix SIMULADO ativo (pix.provedor=mock). Não use em produção.");
    }

    @Override
    public CobrancaCriada criarCobranca(String txid, BigDecimal valor, Duration expiracao, Devedor devedor) {
        cobrancas.put(txid, new CobrancaConsultada(txid, StatusCobranca.ATIVA, valor));
        String location = "pix.mock.raizesdahistoria.local/v2/cob/" + txid;
        return new CobrancaCriada(txid,
                BrCode.dinamico(location, valor, props.recebedorNome(), props.recebedorCidade()));
    }

    @Override
    public Optional<CobrancaConsultada> consultarCobranca(String txid) {
        return Optional.ofNullable(cobrancas.get(txid));
    }

    @Override
    public void removerCobranca(String txid) {
        cobrancas.computeIfPresent(txid, (k, c) -> c.status() == StatusCobranca.ATIVA
                ? new CobrancaConsultada(k, StatusCobranca.REMOVIDA_PELO_USUARIO_RECEBEDOR, c.valor())
                : c);
    }

    /** Simula o pagamento pelo cliente. Devolve {@code false} se o txid não existir ou não estiver ativo. */
    public boolean simularPagamento(String txid) {
        var atualizada = cobrancas.computeIfPresent(txid, (k, c) -> c.status() == StatusCobranca.ATIVA
                ? new CobrancaConsultada(k, StatusCobranca.CONCLUIDA, c.valor())
                : c);
        return atualizada != null && atualizada.status() == StatusCobranca.CONCLUIDA;
    }

    /** Apenas para testes: simula pagamento com valor diferente do cobrado. */
    public void simularPagamentoComValor(String txid, BigDecimal valorPago) {
        cobrancas.put(txid, new CobrancaConsultada(txid, StatusCobranca.CONCLUIDA, valorPago));
    }
}
