package br.com.raizesdahistoria.vendas;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Publicado (dentro da transação) quando um pedido passa a {@code PAGO}.
 * Consumidores devem usar {@code @ApplicationModuleListener} / {@code @TransactionalEventListener(AFTER_COMMIT)}.
 * Na Etapa 5 alimentará {@code movimentacoes_financeiras} (origem {@code VENDA_CATALOGO}).
 */
public record PedidoPagoEvent(UUID pedidoId, UUID clienteId, BigDecimal valorTotal, Instant pagoEm) {
}
