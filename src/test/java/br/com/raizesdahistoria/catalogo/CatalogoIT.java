package br.com.raizesdahistoria.catalogo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;

import com.jayway.jsonpath.JsonPath;

import br.com.raizesdahistoria.AbstractIntegrationTest;
import br.com.raizesdahistoria.Fixtures;
import br.com.raizesdahistoria.usuarios.internal.security.TokensDeTeste;

class CatalogoIT extends AbstractIntegrationTest {

    private static final byte[] PNG_1x1 = java.util.Base64.getDecoder().decode(
            "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNkYPhfDwAChwGA60e6kgAAAABJRU5ErkJggg==");

    @Autowired
    TokensDeTeste tokens;

    @Test
    void buscaPorNomeIgnoraAcentoECaixaEmNomePopularECientifico() throws Exception {
        Fixtures.planta(jdbc, "Jibóia", "Epipremnum aureum", "39.90", "SEMANAL", "MEIA_SOMBRA", 5, true, "Folhagens");
        Fixtures.planta(jdbc, "Babosa", "Aloe vera", "25.00", "QUINZENAL", "PLENO", 5, true, "Medicinais");

        mvc.perform(get("/api/v1/plantas").param("nome", "jiboia"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].nomePopular").value("Jibóia"))
                .andExpect(header().string("Cache-Control", "max-age=60, public"));
        mvc.perform(get("/api/v1/plantas").param("nome", "ALOE"))
                .andExpect(jsonPath("$.content[0].nomePopular").value("Babosa"));
        mvc.perform(get("/api/v1/plantas").param("nome", "100%"))
                .andExpect(jsonPath("$.content", hasSize(0)));
    }

    @Test
    void filtrosCombinadosEPaginacao() throws Exception {
        Fixtures.planta(jdbc, "Jibóia", null, "39.90", "SEMANAL", "MEIA_SOMBRA", 5, true, "Folhagens");
        Fixtures.planta(jdbc, "Costela-de-adão", null, "59.90", "SEMANAL", "SOMBRA", 5, true, "Folhagens");
        Fixtures.planta(jdbc, "Cacto", null, "15.00", "QUINZENAL", "PLENO", 5, true, "Suculentas");
        Integer folhagens = jdbc.queryForObject("SELECT id FROM tipos_planta WHERE nome = 'Folhagens'", Integer.class);

        mvc.perform(get("/api/v1/plantas").param("tipoId", folhagens.toString())
                        .param("frequenciaRega", "SEMANAL").param("exposicaoSol", "SOMBRA"))
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].nomePopular").value("Costela-de-adão"));

        mvc.perform(get("/api/v1/plantas").param("size", "2").param("sort", "preco,desc"))
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.content[0].preco").value(59.90))
                .andExpect(jsonPath("$.page.totalElements").value(3));

        mvc.perform(get("/api/v1/plantas").param("size", "500"))
                .andExpect(jsonPath("$.page.size").value(50));
        mvc.perform(get("/api/v1/plantas").param("frequenciaRega", "TODO_DIA"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void inativaNaoApareceEEsgotadaApareceIndisponivel() throws Exception {
        UUID inativa = Fixtures.planta(jdbc, "Inativa", null, "10.00", "DIARIA", "PLENO", 5, false, "Suculentas");
        UUID esgotada = Fixtures.planta(jdbc, "Esgotada", null, "10.00", "DIARIA", "PLENO", 0, true, "Suculentas");

        mvc.perform(get("/api/v1/plantas"))
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].id").value(esgotada.toString()))
                .andExpect(jsonPath("$.content[0].disponivel").value(false));
        mvc.perform(get("/api/v1/plantas/" + inativa)).andExpect(status().isNotFound());
        mvc.perform(get("/api/v1/plantas/" + esgotada)).andExpect(status().isOk());
    }

    @Test
    void crudAdministrativoDePlantasComSoftDelete() throws Exception {
        Integer tipo = jdbc.queryForObject("SELECT id FROM tipos_planta WHERE nome = 'Folhagens'", Integer.class);
        var corpo = Map.of("tipoId", tipo, "nomePopular", "Jibóia", "nomeCientifico", "Epipremnum aureum",
                "preco", "39.90", "frequenciaRega", "SEMANAL", "exposicaoSol", "MEIA_SOMBRA",
                "quantidadeProntaVenda", 10);
        String criada = mvc.perform(post("/api/v1/admin/plantas").header("Authorization", tokens.bearerAdmin())
                        .contentType(MediaType.APPLICATION_JSON).content(toJson(corpo)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.ativo").value(true))
                .andReturn().getResponse().getContentAsString();
        String id = JsonPath.read(criada, "$.id");

        var edicao = new java.util.HashMap<String, Object>(corpo);
        edicao.put("preco", "45.00");
        mvc.perform(put("/api/v1/admin/plantas/" + id).header("Authorization", tokens.bearerAdmin())
                        .contentType(MediaType.APPLICATION_JSON).content(toJson(edicao)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.preco").value(45.00))
                .andExpect(jsonPath("$.quantidadeProntaVenda").value(10));

        mvc.perform(patch("/api/v1/admin/plantas/" + id + "/estoque").header("Authorization", tokens.bearerAdmin())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"quantidadeProntaVenda\": 3}"))
                .andExpect(jsonPath("$.quantidadeProntaVenda").value(3));

        mvc.perform(delete("/api/v1/admin/plantas/" + id).header("Authorization", tokens.bearerAdmin()))
                .andExpect(status().isNoContent());
        assertThat(jdbc.queryForObject("SELECT ativo FROM plantas WHERE id = ?::uuid", Boolean.class, id)).isFalse();
        mvc.perform(get("/api/v1/plantas/" + id)).andExpect(status().isNotFound());
        mvc.perform(get("/api/v1/admin/plantas").header("Authorization", tokens.bearerAdmin()).param("ativo", "false"))
                .andExpect(jsonPath("$.content", hasSize(1)));

        mvc.perform(patch("/api/v1/admin/plantas/" + id + "/ativo").header("Authorization", tokens.bearerAdmin())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"ativo\": true}"))
                .andExpect(jsonPath("$.ativo").value(true));
    }

    @Test
    void validacaoDePlanta() throws Exception {
        mvc.perform(post("/api/v1/admin/plantas").header("Authorization", tokens.bearerAdmin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(Map.of("tipoId", 9999, "nomePopular", "X", "preco", "-1",
                                "frequenciaRega", "SEMANAL", "exposicaoSol", "PLENO"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros.preco").value("O preço deve ser maior que zero."));
        mvc.perform(post("/api/v1/admin/plantas").header("Authorization", tokens.bearerAdmin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(Map.of("tipoId", 9999, "nomePopular", "X", "preco", "10",
                                "frequenciaRega", "SEMANAL", "exposicaoSol", "PLENO"))))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void tiposCrudEExclusaoBloqueadaComPlantasVinculadas() throws Exception {
        String criado = mvc.perform(post("/api/v1/admin/tipos-planta").header("Authorization", tokens.bearerAdmin())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"nome\": \"Orquídeas\"}"))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        Integer id = JsonPath.read(criado, "$.id");

        mvc.perform(post("/api/v1/admin/tipos-planta").header("Authorization", tokens.bearerAdmin())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"nome\": \"orquídeas\"}"))
                .andExpect(status().isConflict());

        Fixtures.planta(jdbc, "Phalaenopsis", null, "80.00", "SEMANAL", "MEIA_SOMBRA", 1, true, "Orquídeas");
        mvc.perform(delete("/api/v1/admin/tipos-planta/" + id).header("Authorization", tokens.bearerAdmin()))
                .andExpect(status().isConflict());

        jdbc.update("DELETE FROM plantas WHERE tipo_id = ?", id);
        mvc.perform(delete("/api/v1/admin/tipos-planta/" + id).header("Authorization", tokens.bearerAdmin()))
                .andExpect(status().isNoContent());
    }

    @Test
    void uploadDeImagemValidaConteudoNaoExtensao() throws Exception {
        UUID id = Fixtures.planta(jdbc, "Jibóia", "39.90", 5);

        var pdfDisfarcado = new MockMultipartFile("arquivo", "foto.png", "image/png", "%PDF-1.7 falso".getBytes());
        mvc.perform(multipart("/api/v1/admin/plantas/" + id + "/imagem").file(pdfDisfarcado)
                        .header("Authorization", tokens.bearerAdmin()))
                .andExpect(status().isUnsupportedMediaType());

        var png = new MockMultipartFile("arquivo", "qualquer-nome.bin", "application/octet-stream", PNG_1x1);
        String resposta = mvc.perform(multipart("/api/v1/admin/plantas/" + id + "/imagem").file(png)
                        .header("Authorization", tokens.bearerAdmin()))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String url = JsonPath.read(resposta, "$.urlImagem");
        assertThat(url).matches("/imagens/plantas/[0-9a-f-]{36}\\.png");

        mvc.perform(get(url)).andExpect(status().isOk());
    }
}
