package br.com.raizesdahistoria.usuarios;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import com.jayway.jsonpath.JsonPath;

import br.com.raizesdahistoria.AbstractIntegrationTest;
import br.com.raizesdahistoria.usuarios.internal.security.TokensDeTeste;

class AutenticacaoIT extends AbstractIntegrationTest {

    @Autowired
    TokensDeTeste tokens;

    @BeforeEach
    void setUp() {
        tokens.liberarLogin();
    }

    private String login(String email, String senha) throws Exception {
        return toJson(Map.of("email", email, "senha", senha));
    }

    @Test
    void adminInicialCriadoPorSeedFazLoginERecebeJwt() throws Exception {
        var resposta = mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(login("admin@teste.dev", "senhaTeste123")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tipo").value("Bearer"))
                .andExpect(jsonPath("$.expiraEm").exists())
                .andReturn().getResponse().getContentAsString();
        String token = JsonPath.read(resposta, "$.accessToken");

        mvc.perform(get("/api/v1/admin/administradores/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("admin@teste.dev"))
                .andExpect(content().string(not(containsString("senha"))));
    }

    @Test
    void loginInvalidoRetorna401ComMensagemGenerica() throws Exception {
        mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(login("admin@teste.dev", "errada")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value("E-mail ou senha inválidos."));
        mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(login("naoexiste@teste.dev", "qualquer")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value("E-mail ou senha inválidos."));
    }

    @Test
    void limitaTentativasDeLogin() throws Exception {
        for (int i = 0; i < 5; i++) {
            mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                    .content(login("admin@teste.dev", "errada"))).andExpect(status().isUnauthorized());
        }
        mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(login("admin@teste.dev", "senhaTeste123")))
                .andExpect(status().isTooManyRequests());
    }

    @Test
    void rotasAdminExigemToken() throws Exception {
        mvc.perform(get("/api/v1/admin/plantas"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
        mvc.perform(get("/api/v1/admin/plantas").header("Authorization", "Bearer token.invalido.x"))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/admin/plantas").header("Authorization", tokens.bearerAdmin()))
                .andExpect(status().isOk());
    }

    @Test
    void rotasPublicasNaoExigemToken() throws Exception {
        mvc.perform(get("/api/v1/tipos-planta")).andExpect(status().isOk());
        mvc.perform(get("/api/v1/plantas")).andExpect(status().isOk());
    }

    @Test
    void adminCadastraOutroAdminEEmailDuplicadoRetorna409() throws Exception {
        String corpo = toJson(Map.of("nome", "Everaldo", "email", "Everaldo@Raizes.dev", "senha", "senhaForte1"));
        mvc.perform(post("/api/v1/admin/administradores").header("Authorization", tokens.bearerAdmin())
                        .contentType(MediaType.APPLICATION_JSON).content(corpo))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("everaldo@raizes.dev"))
                .andExpect(content().string(not(containsString("senha"))));
        mvc.perform(post("/api/v1/admin/administradores").header("Authorization", tokens.bearerAdmin())
                        .contentType(MediaType.APPLICATION_JSON).content(corpo))
                .andExpect(status().isConflict());

        mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(login("everaldo@raizes.dev", "senhaForte1")))
                .andExpect(status().isOk());
        jdbc.update("DELETE FROM administradores WHERE email = 'everaldo@raizes.dev'");
    }

    @Test
    void cadastroDeAdminSemTokenEhNegado() throws Exception {
        mvc.perform(post("/api/v1/admin/administradores").contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(Map.of("nome", "X", "email", "x@x.dev", "senha", "senhaForte1"))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void validacaoRetornaErrosEmPortugues() throws Exception {
        mvc.perform(post("/api/v1/admin/administradores").header("Authorization", tokens.bearerAdmin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(Map.of("nome", "", "email", "invalido", "senha", "123"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros.email").value("E-mail inválido."))
                .andExpect(jsonPath("$.erros.senha").value("A senha deve ter entre 8 e 72 caracteres."));
    }
}
