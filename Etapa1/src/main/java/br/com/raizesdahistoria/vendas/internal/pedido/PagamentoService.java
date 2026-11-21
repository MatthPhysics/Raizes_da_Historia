package br.com.raizesdahistoria.vendas.internal.pedido;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.raizesdahistoria.catalogo.CatalogoService;
import br.com.raizesdahistoria.shared.exception.RecursoNaoEncontradoException;
import br.com.raizesdahistoria.vendas.PedidoPagoEvent;
import br.com.raizesdahistoria.vendas.internal.pix.PixGateway;
import br.com.raizesdahistoria.vendas.internal.pix.PixGateway.CobrancaConsultada;
import br.com.raizesdahistoria.vendas.internal.pix.PixGateway.StatusCobranca;
import br.com.raizesdahistoria.vendas.internal.pix.PixProperties;
import br.com.raizesdahistoria.vendas.internal.pix.PspIndisponivelException;

/**
 * Ciclo de vida do pagamento (§7.2): PENDENTE → PAGO | EXPIRADO | CANCELADO.
 * Toda transição bloqueia o pedido e só ocorre a partir de PENDENTE (idempotência).
 */
@Service
public class PagamentoService {

    private static final Logger log = LoggerFactory.getLogger(PagamentoService.class);

    private final PedidoRepository pedidoRepository;
    private final PixGateway pixGateway;
    private final CatalogoService catalogoService;
    private final ApplicationEventPublisher eventos;
    private final PixProperties pixProperties;
    private final Clock clock;

    public PagamentoService(PedidoRepository pedidoRepository, PixGateway pixGateway, CatalogoService catalogoService, ApplicationEventPublisher eventos, PixProperties pixProperties, Clock clock) {
        this.pedidoRepository = pedidoRepository;
        this.pixGateway = pixGateway;
        this.catalogoService = catalogoService;
        this.eventos = eventos;
        this.pixProperties = pixProperties;
        this.clock = clock;
    }

    /**
     * Processa uma notificação de pagamento. Nunca confia no corpo do webhook: consulta a cobrança no PSP e
     * confere txid <b>e</b> valor antes de marcar como pago.
     *
     * @return {@code true} se o pedido passou a PAGO nesta chamada
     */
    @Transactional
    public boolean confirmarPagamento(String txid) {
        Optional<Pedido> encontrado = pedidoRepository.bloquearPorTxid(txid);
        if (encontrado.isEmpty()) {
            log.warn("Notificação Pix para txid desconhecido");
            return false;
        }
        Pedido pedido = encontrado.get();
        if (!pedido.isPendente()) {
            if (pedido.getStatusPagamento() == StatusPagamento.PAGO) {
                log.info("Notificação repetida para o pedido {} (já pago): ignorada", pedido.getId());
            } else {
                log.warn("Pagamento notificado para o pedido {} em status {}: requer conferência/estorno manual",
                        pedido.getId(), pedido.getStatusPagamento());
            }
            return false;
        }
        var cobranca = pixGateway.consultarCobranca(txid);
        return cobranca.isPresent() && marcarPagoSeConferir(pedido, cobranca.get());
    }

    /**
     * Expira um pedido pendente vencido e devolve o estoque. Antes, consulta o PSP: se a cobrança já foi paga
     * (webhook perdido), confirma o pagamento em vez de expirar.
     */
    @Transactional
    public void expirarSeVencido(UUID pedidoId) {
        var pedido = pedidoRepository.bloquearPorId(pedidoId).orElse(null);
        if (pedido == null || !pedido.isPendente()) {
            return;
        }
        Instant limite = pedido.getCriadoEm().plus(Duration.ofMinutes(pixProperties.expiracaoMinutos()));
        if (Instant.now(clock).isBefore(limite)) {
            return;
        }
        if (pedido.getPixTxid() != null) {
            Optional<CobrancaConsultada> cobranca;
            try {
                cobranca = pixGateway.consultarCobranca(pedido.getPixTxid());
            } catch (PspIndisponivelException e) {
                log.warn("PSP indisponível ao verificar o pedido {}: expiração adiada", pedidoId);
                return;
            }
            if (cobranca.isPresent() && cobranca.get().status() == StatusCobranca.CONCLUIDA
                    && marcarPagoSeConferir(pedido, cobranca.get())) {
                return;
            }
        }
        if (pedido.expirar()) {
            catalogoService.devolverEstoque(pedido.quantidadesPorPlanta());
            log.info("Pedido {} expirado; estoque devolvido", pedidoId);
        }
    }

    /** Cancelamento pelo administrador: só pedidos pendentes; devolve estoque e remove a cobrança no PSP. */
    @Transactional
    public Pedido cancelar(UUID pedidoId) {
        var pedido = pedidoRepository.bloquearPorId(pedidoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Pedido não encontrado."));
        if (!pedido.cancelar()) {
            throw new TransicaoStatusInvalidaException(
                    "Só pedidos pendentes podem ser cancelados (status atual: " + pedido.getStatusPagamento() + ").");
        }
        catalogoService.devolverEstoque(pedido.quantidadesPorPlanta());
        if (pedido.getPixTxid() != null) {
            pixGateway.removerCobranca(pedido.getPixTxid());
        }
        log.info("Pedido {} cancelado pelo administrador; estoque devolvido", pedidoId);
        return pedido;
    }

    private boolean marcarPagoSeConferir(Pedido pedido, CobrancaConsultada cobranca) {
        if (cobranca.status() != StatusCobranca.CONCLUIDA) {
            log.info("Cobrança do pedido {} ainda não concluída no PSP ({})", pedido.getId(), cobranca.status());
            return false;
        }
        if (!cobranca.txid().equals(pedido.getPixTxid()) || !Dinheiro.iguais(cobranca.valor(), pedido.getValorTotal())) {
            log.warn("Pagamento do pedido {} NÃO confirmado: txid ou valor divergente do PSP", pedido.getId());
            return false;
        }
        Instant agora = Instant.now(clock);
        if (!pedido.marcarPago(agora)) {
            return false;
        }
        eventos.publishEvent(new PedidoPagoEvent(pedido.getId(), pedido.getClienteId(), pedido.getValorTotal(), agora));
        log.info("Pedido {} pago", pedido.getId());
        return true;
    }
}
