package br.com.raizesdahistoria.vendas.internal.pedido;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import br.com.raizesdahistoria.vendas.internal.pix.PixProperties;

/** A cada minuto expira pedidos pendentes vencidos (30 min) e devolve o estoque. Cada pedido em sua transação. */
@Component
public class ExpiracaoPedidosJob {

    private static final Logger log = LoggerFactory.getLogger(ExpiracaoPedidosJob.class);
    private static final int LOTE = 100;

    private final PedidoRepository pedidoRepository;
    private final PagamentoService pagamentoService;
    private final PixProperties pixProperties;
    private final Clock clock;

    public ExpiracaoPedidosJob(PedidoRepository pedidoRepository, PagamentoService pagamentoService, PixProperties pixProperties, Clock clock) {
        this.pedidoRepository = pedidoRepository;
        this.pagamentoService = pagamentoService;
        this.pixProperties = pixProperties;
        this.clock = clock;
    }

    @Scheduled(fixedDelayString = "${pix.expiracao-job-intervalo-ms:60000}",
            initialDelayString = "${pix.expiracao-job-intervalo-ms:60000}")
    public void executar() {
        Instant limite = Instant.now(clock).minus(Duration.ofMinutes(pixProperties.expiracaoMinutos()));
        List<UUID> vencidos = pedidoRepository.idsPorStatusCriadosAntesDe(StatusPagamento.PENDENTE, limite,
                PageRequest.of(0, LOTE));
        for (UUID id : vencidos) {
            try {
                pagamentoService.expirarSeVencido(id);
            } catch (RuntimeException e) {
                log.error("Falha ao expirar o pedido {}", id, e);
            }
        }
    }

    @Configuration
    @EnableScheduling
    static class AgendamentoConfig {
    }
}
