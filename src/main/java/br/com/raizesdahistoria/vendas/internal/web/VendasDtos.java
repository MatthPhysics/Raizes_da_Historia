package br.com.raizesdahistoria.vendas.internal.web;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import br.com.raizesdahistoria.usuarios.ClienteDto;
import br.com.raizesdahistoria.usuarios.NovoCliente;
import br.com.raizesdahistoria.vendas.internal.pedido.CheckoutService;
import br.com.raizesdahistoria.vendas.internal.pedido.StatusEntrega;
import br.com.raizesdahistoria.vendas.internal.pedido.StatusPagamento;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

final class VendasDtos {

    private VendasDtos() {
    }

    // ---------- Carrinho ----------

    record ItemCarrinhoRequest(
            @NotNull(message = "plantaId é obrigatório.") UUID plantaId,
            @NotNull(message = "quantidade é obrigatória.")
            @Min(value = 1, message = "A quantidade mínima é 1.")
            @Max(value = CheckoutService.QUANTIDADE_MAXIMA_POR_ITEM, message = "A quantidade máxima é 99.")
            Integer quantidade,
            @Schema(description = "Opcional: preço exibido no front, para detectar alteração de preço.", nullable = true)
            BigDecimal precoReferencia) {
    }

    // ---------- Checkout ----------

    record ItemCheckoutRequest(
            @NotNull(message = "plantaId é obrigatório.") UUID plantaId,
            @NotNull(message = "quantidade é obrigatória.")
            @Min(value = 1, message = "A quantidade mínima é 1.")
            @Max(value = CheckoutService.QUANTIDADE_MAXIMA_POR_ITEM, message = "A quantidade máxima é 99.")
            Integer quantidade) {
    }

    /** O payload nunca traz preço: qualquer campo de preço/total enviado é ignorado. */
    record CheckoutRequest(
            @NotNull(message = "Os dados do cliente são obrigatórios.") @Valid NovoCliente cliente,
            @NotEmpty(message = "O pedido deve ter ao menos um item.")
            @Size(max = 50, message = "O pedido pode ter no máximo 50 itens diferentes.")
            List<@Valid @NotNull ItemCheckoutRequest> itens) {
    }

    record PixResponse(String txid, String copiaECola, String qrCodeBase64, Instant expiraEm) {
    }

    record ItemResponse(UUID plantaId, String nomePopular, int quantidade, BigDecimal precoUnitario) {
    }

    record PedidoCriadoResponse(UUID pedidoId, BigDecimal valorTotal, StatusPagamento statusPagamento,
            PixResponse pix, List<ItemResponse> itens) {
    }

    /** Visão pública (polling do front): sem dados pessoais do cliente. */
    record PedidoPublicoResponse(UUID pedidoId, StatusPagamento statusPagamento, BigDecimal valorTotal,
            Instant criadoEm, Instant pagoEm, PixResponse pix, List<ItemResponse> itens) {
    }

    // ---------- Webhook (formato API Pix do Bacen) ----------

    record WebhookPixRequest(List<PixRecebido> pix) {
        record PixRecebido(String endToEndId, String txid, String valor, String horario) {
        }
    }

    // ---------- Admin ----------

    record PedidoResumoAdmin(UUID id, UUID clienteId, BigDecimal valorTotal, StatusPagamento statusPagamento,
            String pixTxid, Instant criadoEm, Instant pagoEm) {
    }

    record ItemAdminResponse(UUID id, UUID plantaId, String nomePopular, int quantidade, BigDecimal precoUnitario,
            StatusEntrega statusEntrega) {
    }

    record PedidoAdminResponse(UUID id, StatusPagamento statusPagamento, BigDecimal valorTotal, String pixTxid,
            Instant criadoEm, Instant pagoEm, ClienteDto cliente, List<ItemAdminResponse> itens) {
    }

    record StatusEntregaRequest(@NotNull(message = "O status de entrega é obrigatório.") StatusEntrega statusEntrega) {
    }
}
