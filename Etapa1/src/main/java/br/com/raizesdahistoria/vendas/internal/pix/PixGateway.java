package br.com.raizesdahistoria.vendas.internal.pix;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.Optional;

/**
 * Abstração do PSP (padrão API Pix do Bacen). Implementações: {@link MockPixGateway} (dev/test) e
 * {@link PspPixGateway} (PSP real, a definir). Falhas de comunicação devem lançar {@link PspIndisponivelException}.
 */
public interface PixGateway {

    /** Cria cobrança imediata ({@code PUT /cob/{txid}}). */
    CobrancaCriada criarCobranca(String txid, BigDecimal valor, Duration expiracao, Devedor devedor);

    /** Consulta a cobrança no PSP ({@code GET /cob/{txid}}). Vazio se o PSP não conhece o txid. */
    Optional<CobrancaConsultada> consultarCobranca(String txid);

    /** Remove a cobrança para impedir pagamento (pedido cancelado). Melhor esforço. */
    void removerCobranca(String txid);

    record Devedor(String nome, String cpf) {
    }

    record CobrancaCriada(String txid, String copiaECola) {
    }

    record CobrancaConsultada(String txid, StatusCobranca status, BigDecimal valor) {
    }

    /** Status conforme a API Pix do Bacen. */
    enum StatusCobranca {
        ATIVA, CONCLUIDA, REMOVIDA_PELO_USUARIO_RECEBEDOR, REMOVIDA_PELO_PSP
    }
}
