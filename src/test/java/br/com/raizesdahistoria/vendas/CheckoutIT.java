package br.com.raizesdahistoria.vendas;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.startsWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import com.jayway.jsonpath.JsonPath;

import br.com.raizesdahistoria.AbstractIntegrationTest;
import br.com.raizesdahistoria.Fixtures;
import br.com.raizesdahistoria.vendas.internal.pix.PixGateway;
import br.com.raizesdahistoria.vendas.internal.pix.PspIndisponivelException;

class CheckoutIT extends AbstractIntegrationTest {

    @MockitoSpyBean
    PixGateway pixGateway;

    static Map<String, Object> cliente() {
        var c = new java.util.HashMap<String, Object>();
        c.put("nome", "Maria Silva");
        c.put("email", "maria@email.com");
        c.put("telefoneWhatsapp", "11999998888");
        c.put("cpf", null);
        return c;
    }

    static String checkout(Object itens) throws Exception {
        return new com.fasterxml.jackson.databind.ObjectMapper()
                .writeValueAsString(Map.of("cliente", cliente(), "itens", itens));
    }

    @Test
    void checkoutCalculaTotalNoServidorReservaEstoqueEGeraPix() throws Exception {
        UUID jiboia = Fixtures.planta(jdbc, "Jibóia", "39.90", 5);
        // O cliente tenta mandar preço e total: devem ser ignorados
        String corpo = new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(Map.of(
                "cliente", cliente(),
                "valorTotal", 0.01,
                "itens", List.of(Map.of("plantaId", jiboia, "quantidade", 2, "precoUnitario", 0.01))));

        String resposta = mvc.perform(post("/api/v1/pedidos").contentType(MediaType.APPLICATION_JSON).content(corpo))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.valorTotal").value(79.80))
                .andExpect(jsonPath("$.statusPagamento").value("PENDENTE"))
                .andExpect(jsonPath("$.pix.txid", startsWith("RAIZES")))
                .andExpect(jsonPath("$.pix.copiaECola", startsWith("000201")))
                .andExpect(jsonPath("$.pix.qrCodeBase64", startsWith("iVBORw0KGgo")))
                .andExpect(jsonPath("$.pix.expiraEm").exists())
                .andExpect(jsonPath("$.itens[0].nomePopular").value("Jibóia"))
                .andExpect(jsonPath("$.itens[0].precoUnitario").value(39.90))
                .andReturn().getResponse().getContentAsString();

        assertThat(Fixtures.estoque(jdbc, jiboia)).isEqualTo(3);
        String pedidoId = JsonPath.read(resposta, "$.pedidoId");
        assertThat(jdbc.queryForObject("SELECT valor_total FROM pedidos WHERE id = ?::uuid", java.math.BigDecimal.class, pedidoId))
                .isEqualByComparingTo("79.80");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM clientes", Integer.class)).isEqualTo(1);
    }

    @Test
    void itensRepetidosSaoSomados() throws Exception {
        UUID jiboia = Fixtures.planta(jdbc, "Jibóia", "10.00", 5);
        mvc.perform(post("/api/v1/pedidos").contentType(MediaType.APPLICATION_JSON).content(checkout(List.of(
                        Map.of("plantaId", jiboia, "quantidade", 1), Map.of("plantaId", jiboia, "quantidade", 2)))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.itens", hasSize(1)))
                .andExpect(jsonPath("$.itens[0].quantidade").value(3))
                .andExpect(jsonPath("$.valorTotal").value(30.00));
        assertThat(Fixtures.estoque(jdbc, jiboia)).isEqualTo(2);
    }

    @Test
    void estoqueInsuficienteRetorna422ListandoProblemas() throws Exception {
        UUID jiboia = Fixtures.planta(jdbc, "Jibóia", "39.90", 1);
        mvc.perform(post("/api/v1/pedidos").contentType(MediaType.APPLICATION_JSON)
                        .content(checkout(List.of(Map.of("plantaId", jiboia, "quantidade", 2)))))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.problemas[0]").value("'Jibóia': solicitado 2, disponível 1."));
        assertThat(Fixtures.estoque(jdbc, jiboia)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM pedidos", Integer.class)).isZero();
    }

    @Test
    void plantaInativaOuInexistenteRetorna422() throws Exception {
        UUID inativa = Fixtures.planta(jdbc, "Inativa", null, "10.00", "DIARIA", "PLENO", 5, false, "Suculentas");
        mvc.perform(post("/api/v1/pedidos").contentType(MediaType.APPLICATION_JSON).content(checkout(List.of(
                        Map.of("plantaId", inativa, "quantidade", 1),
                        Map.of("plantaId", UUID.randomUUID(), "quantidade", 1)))))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.problemas", hasSize(2)));
    }

    @Test
    void validacaoDeEntrada() throws Exception {
        UUID jiboia = Fixtures.planta(jdbc, "Jibóia", "10.00", 500);
        var clienteInvalido = Map.of("nome", "", "email", "x", "telefoneWhatsapp", "999", "cpf", "11111111111");
        mvc.perform(post("/api/v1/pedidos").contentType(MediaType.APPLICATION_JSON).content(toJson(Map.of(
                        "cliente", clienteInvalido, "itens", List.of(Map.of("plantaId", jiboia, "quantidade", 1))))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros['cliente.cpf']").value("CPF inválido."));
        mvc.perform(post("/api/v1/pedidos").contentType(MediaType.APPLICATION_JSON)
                        .content(checkout(List.of(Map.of("plantaId", jiboia, "quantidade", 100)))))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/v1/pedidos").contentType(MediaType.APPLICATION_JSON).content(checkout(List.of())))
                .andExpect(status().isBadRequest());
        // 60 + 60 da mesma planta: cada item é válido, mas a soma excede 99
        mvc.perform(post("/api/v1/pedidos").contentType(MediaType.APPLICATION_JSON).content(checkout(List.of(
                        Map.of("plantaId", jiboia, "quantidade", 60), Map.of("plantaId", jiboia, "quantidade", 60)))))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void falhaNoPspFazRollbackERetorna502() throws Exception {
        UUID jiboia = Fixtures.planta(jdbc, "Jibóia", "39.90", 5);
        doThrow(new PspIndisponivelException("simulado", null)).when(pixGateway).criarCobranca(any(), any(), any(), any());

        mvc.perform(post("/api/v1/pedidos").contentType(MediaType.APPLICATION_JSON)
                        .content(checkout(List.of(Map.of("plantaId", jiboia, "quantidade", 2)))))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.detail").value(
                        "Não foi possível gerar a cobrança Pix no momento. Tente novamente em instantes."));

        assertThat(Fixtures.estoque(jdbc, jiboia)).isEqualTo(5);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM pedidos", Integer.class)).isZero();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM clientes", Integer.class)).isZero();
    }

    @Test
    void consultaPublicaDoPedidoNaoExpoeDadosPessoais() throws Exception {
        UUID jiboia = Fixtures.planta(jdbc, "Jibóia", "39.90", 5);
        String resposta = mvc.perform(post("/api/v1/pedidos").contentType(MediaType.APPLICATION_JSON)
                        .content(checkout(List.of(Map.of("plantaId", jiboia, "quantidade", 1)))))
                .andReturn().getResponse().getContentAsString();
        String pedidoId = JsonPath.read(resposta, "$.pedidoId");

        String publico = mvc.perform(get("/api/v1/pedidos/" + pedidoId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusPagamento").value("PENDENTE"))
                .andExpect(jsonPath("$.pix.qrCodeBase64").exists())
                .andReturn().getResponse().getContentAsString();
        assertThat(publico).doesNotContain("maria", "Maria", "11999998888", "clienteId");

        mvc.perform(get("/api/v1/pedidos/" + UUID.randomUUID())).andExpect(status().isNotFound());
    }

    @Test
    void validacaoDoCarrinhoTrazPrecosAtuaisEAvisos() throws Exception {
        UUID jiboia = Fixtures.planta(jdbc, "Jibóia", "39.90", 1);
        UUID inativa = Fixtures.planta(jdbc, "Inativa", null, "10.00", "DIARIA", "PLENO", 5, false, "Suculentas");
        UUID cacto = Fixtures.planta(jdbc, "Cacto", "15.00", 10);

        mvc.perform(post("/api/v1/carrinho/validar").contentType(MediaType.APPLICATION_JSON).content(toJson(List.of(
                        Map.of("plantaId", jiboia, "quantidade", 2),
                        Map.of("plantaId", inativa, "quantidade", 1),
                        Map.of("plantaId", cacto, "quantidade", 2, "precoReferencia", 12.00)))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.itens", hasSize(2)))
                .andExpect(jsonPath("$.total").value(109.80))
                .andExpect(jsonPath("$.podeFinalizar").value(false))
                .andExpect(jsonPath("$.avisos[?(@.tipo == 'ESTOQUE_INSUFICIENTE')]", hasSize(1)))
                .andExpect(jsonPath("$.avisos[?(@.tipo == 'PLANTA_INATIVA')]", hasSize(1)))
                .andExpect(jsonPath("$.avisos[?(@.tipo == 'PRECO_ALTERADO')]", hasSize(1)));
        assertThat(Fixtures.estoque(jdbc, jiboia)).isEqualTo(1); // validar não reserva
    }
}
