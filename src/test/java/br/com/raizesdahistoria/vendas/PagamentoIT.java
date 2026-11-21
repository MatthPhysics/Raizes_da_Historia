package br.com.raizesdahistoria.vendas;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;

import com.jayway.jsonpath.JsonPath;

import br.com.raizesdahistoria.AbstractIntegrationTest;
import br.com.raizesdahistoria.Fixtures;
import br.com.raizesdahistoria.usuarios.internal.security.TokensDeTeste;
import br.com.raizesdahistoria.vendas.internal.pedido.ExpiracaoPedidosJob;
import br.com.raizesdahistoria.vendas.internal.pix.MockPixGateway;

@RecordApplicationEvents
class PagamentoIT extends AbstractIntegrationTest {

    private static final String TOKEN_WEBHOOK = "test-webhook-secret";

    @Autowired
    ApplicationEvents eventos;

    @Autowired
    MockPixGateway psp;

    @Autowired
    ExpiracaoPedidosJob expiracaoJob;

    @Autowired
    TokensDeTeste tokens;

    private record Criado(String pedidoId, String txid) {
    }

    private Criado criarPedido(UUID plantaId, int quantidade) throws Exception {
        String resposta = mvc.perform(post("/api/v1/pedidos").contentType(MediaType.APPLICATION_JSON)
                        .content(CheckoutIT.checkout(List.of(Map.of("plantaId", plantaId, "quantidade", quantidade)))))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return new Criado(JsonPath.read(resposta, "$.pedidoId"), JsonPath.read(resposta, "$.pix.txid"));
    }

    private void webhook(String txid) throws Exception {
        mvc.perform(post("/api/v1/pagamentos/pix/webhook").header("X-Webhook-Token", TOKEN_WEBHOOK)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(Map.of("pix", List.of(Map.of("txid", txid, "valor", "79.80",
                                "endToEndId", "E123", "horario", "2026-11-18T10:00:00Z"))))))
                .andExpect(status().isOk());
    }

    private String statusDe(String pedidoId) {
        return jdbc.queryForObject("SELECT status_pagamento FROM pedidos WHERE id = ?::uuid", String.class, pedidoId);
    }

    @Test
    void webhookConfirmadoNoPspMarcaPagoEPublicaEvento() throws Exception {
        UUID jiboia = Fixtures.planta(jdbc, "Jibóia", "39.90", 5);
        var pedido = criarPedido(jiboia, 2);
        psp.simularPagamento(pedido.txid());

        webhook(pedido.txid());

        assertThat(statusDe(pedido.pedidoId())).isEqualTo("PAGO");
        assertThat(jdbc.queryForObject("SELECT pago_em FROM pedidos WHERE id = ?::uuid", java.sql.Timestamp.class,
                pedido.pedidoId())).isNotNull();
        var publicados = eventos.stream(PedidoPagoEvent.class).toList();
        assertThat(publicados).hasSize(1);
        assertThat(publicados.get(0).pedidoId()).isEqualTo(UUID.fromString(pedido.pedidoId()));
        assertThat(publicados.get(0).valorTotal()).isEqualByComparingTo("79.80");
        assertThat(publicados.get(0).pagoEm()).isNotNull();

        mvc.perform(get("/api/v1/pedidos/" + pedido.pedidoId()))
                .andExpect(jsonPath("$.statusPagamento").value("PAGO"))
                .andExpect(jsonPath("$.pix.qrCodeBase64").doesNotExist());
    }

    @Test
    void webhookDuplicadoEhIdempotente() throws Exception {
        UUID jiboia = Fixtures.planta(jdbc, "Jibóia", "39.90", 5);
        var pedido = criarPedido(jiboia, 2);
        psp.simularPagamento(pedido.txid());

        webhook(pedido.txid());
        webhook(pedido.txid());

        assertThat(eventos.stream(PedidoPagoEvent.class)).hasSize(1);
        assertThat(statusDe(pedido.pedidoId())).isEqualTo("PAGO");
    }

    @Test
    void webhookSemPagamentoNoPspNaoConfirma() throws Exception {
        UUID jiboia = Fixtures.planta(jdbc, "Jibóia", "39.90", 5);
        var pedido = criarPedido(jiboia, 2);

        webhook(pedido.txid()); // corpo diz que pagou, mas o PSP diz que a cobrança está ATIVA

        assertThat(statusDe(pedido.pedidoId())).isEqualTo("PENDENTE");
        assertThat(eventos.stream(PedidoPagoEvent.class)).isEmpty();
    }

    @Test
    void valorDivergenteNaoConfirma() throws Exception {
        UUID jiboia = Fixtures.planta(jdbc, "Jibóia", "39.90", 5);
        var pedido = criarPedido(jiboia, 2);
        psp.simularPagamentoComValor(pedido.txid(), new BigDecimal("0.01"));

        webhook(pedido.txid());

        assertThat(statusDe(pedido.pedidoId())).isEqualTo("PENDENTE");
        assertThat(eventos.stream(PedidoPagoEvent.class)).isEmpty();
    }

    @Test
    void webhookSemTokenValidoEhRejeitado() throws Exception {
        mvc.perform(post("/api/v1/pagamentos/pix/webhook").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"pix\": []}"))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/api/v1/pagamentos/pix/webhook").header("X-Webhook-Token", "errado")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"pix\": []}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void pedidoPendenteApos30MinutosExpiraEDevolveEstoque() throws Exception {
        UUID jiboia = Fixtures.planta(jdbc, "Jibóia", "39.90", 5);
        var pedido = criarPedido(jiboia, 2);
        assertThat(Fixtures.estoque(jdbc, jiboia)).isEqualTo(3);

        relogio.avancar(Duration.ofMinutes(29));
        expiracaoJob.executar();
        assertThat(statusDe(pedido.pedidoId())).isEqualTo("PENDENTE");

        relogio.avancar(Duration.ofMinutes(2));
        expiracaoJob.executar();
        assertThat(statusDe(pedido.pedidoId())).isEqualTo("EXPIRADO");
        assertThat(Fixtures.estoque(jdbc, jiboia)).isEqualTo(5);
        assertThat(jdbc.queryForList("SELECT status_entrega FROM itens_pedido", String.class)).containsOnly("CANCELADA");

        // Webhook tardio não ressuscita o pedido
        psp.simularPagamento(pedido.txid());
        webhook(pedido.txid());
        assertThat(statusDe(pedido.pedidoId())).isEqualTo("EXPIRADO");
        assertThat(eventos.stream(PedidoPagoEvent.class)).isEmpty();
    }

    @Test
    void jobConfirmaPagamentoSeWebhookFoiPerdido() throws Exception {
        UUID jiboia = Fixtures.planta(jdbc, "Jibóia", "39.90", 5);
        var pedido = criarPedido(jiboia, 2);
        psp.simularPagamento(pedido.txid()); // pagou, mas o webhook nunca chegou

        relogio.avancar(Duration.ofMinutes(31));
        expiracaoJob.executar();

        assertThat(statusDe(pedido.pedidoId())).isEqualTo("PAGO");
        assertThat(Fixtures.estoque(jdbc, jiboia)).isEqualTo(3);
        assertThat(eventos.stream(PedidoPagoEvent.class)).hasSize(1);
    }

    @Test
    void adminCancelaPedidoPendenteEDevolveEstoque() throws Exception {
        UUID jiboia = Fixtures.planta(jdbc, "Jibóia", "39.90", 5);
        var pedido = criarPedido(jiboia, 2);

        mvc.perform(patch("/api/v1/admin/pedidos/" + pedido.pedidoId() + "/cancelar")
                        .header("Authorization", tokens.bearerAdmin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusPagamento").value("CANCELADO"))
                .andExpect(jsonPath("$.itens[0].statusEntrega").value("CANCELADA"));
        assertThat(Fixtures.estoque(jdbc, jiboia)).isEqualTo(5);

        // Cobrança removida no PSP: pagamento posterior não é possível
        assertThat(psp.simularPagamento(pedido.txid())).isFalse();

        mvc.perform(patch("/api/v1/admin/pedidos/" + pedido.pedidoId() + "/cancelar")
                        .header("Authorization", tokens.bearerAdmin()))
                .andExpect(status().isConflict());
    }

    @Test
    void adminAcompanhaPedidosEAtualizaStatusDeEntrega() throws Exception {
        UUID jiboia = Fixtures.planta(jdbc, "Jibóia", "39.90", 5);
        var pedido = criarPedido(jiboia, 2);

        String detalhe = mvc.perform(get("/api/v1/admin/pedidos/" + pedido.pedidoId())
                        .header("Authorization", tokens.bearerAdmin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cliente.email").value("maria@email.com"))
                .andReturn().getResponse().getContentAsString();
        String itemId = JsonPath.read(detalhe, "$.itens[0].id");
        String rotaItem = "/api/v1/admin/pedidos/" + pedido.pedidoId() + "/itens/" + itemId + "/status-entrega";

        mvc.perform(patch(rotaItem).header("Authorization", tokens.bearerAdmin())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"statusEntrega\": \"PRONTA\"}"))
                .andExpect(status().isConflict()); // ainda não pago

        psp.simularPagamento(pedido.txid());
        webhook(pedido.txid());

        mvc.perform(patch(rotaItem).header("Authorization", tokens.bearerAdmin())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"statusEntrega\": \"PRONTA\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.itens[0].statusEntrega").value("PRONTA"));

        mvc.perform(get("/api/v1/admin/pedidos").header("Authorization", tokens.bearerAdmin())
                        .param("statusPagamento", "PAGO"))
                .andExpect(jsonPath("$.content[0].id").value(pedido.pedidoId()));
        mvc.perform(get("/api/v1/admin/pedidos").header("Authorization", tokens.bearerAdmin())
                        .param("statusPagamento", "PENDENTE"))
                .andExpect(jsonPath("$.page.totalElements").value(0));
    }
}
