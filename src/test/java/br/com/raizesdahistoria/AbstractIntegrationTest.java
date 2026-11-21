package br.com.raizesdahistoria;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.PostgreSQLContainer;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Base para testes de integração com PostgreSQL 16 real — as migrations Flyway rodam de verdade.
 * <ul>
 * <li>Padrão: Testcontainers ({@code postgres:16-alpine}), um container compartilhado por toda a suíte.</li>
 * <li>Alternativa sem Docker (ex.: CI com serviço PostgreSQL): {@code -Dit.db.url=jdbc:postgresql://...}
 * {@code -Dit.db.user=... -Dit.db.password=...} — use um banco descartável.</li>
 * </ul>
 * Sem Docker e sem banco externo, os testes de integração são pulados.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestClockConfig.class)
public abstract class AbstractIntegrationTest {

    private static final String URL_EXTERNA = propriedade("it.db.url");
    private static final PostgreSQLContainer<?> POSTGRES = iniciarContainer();

    @Autowired
    protected MockMvc mvc;

    @Autowired
    protected ObjectMapper json;

    @Autowired
    protected JdbcTemplate jdbc;

    @Autowired
    protected MutableClock relogio;

    private static String propriedade(String nome) {
        String valor = System.getProperty(nome);
        return valor == null || valor.isBlank() || valor.startsWith("${") ? null : valor;
    }

    private static String propriedadeOu(String nome, String padrao) {
        String valor = propriedade(nome);
        return valor == null ? padrao : valor;
    }

    static boolean bancoDisponivel() {
        return URL_EXTERNA != null || POSTGRES != null;
    }

    private static PostgreSQLContainer<?> iniciarContainer() {
        if (URL_EXTERNA != null) {
            return null;
        }
        try {
            if (!DockerClientFactory.instance().isDockerAvailable()) {
                return null;
            }
        } catch (RuntimeException e) {
            return null;
        }
        var container = new PostgreSQLContainer<>("postgres:16-alpine");
        container.start();
        return container;
    }

    /** Roda antes do contexto Spring subir: sem banco, a classe inteira é pulada (não falha). */
    @BeforeAll
    static void exigirBanco() {
        Assumptions.assumeTrue(bancoDisponivel(),
                "Sem Docker e sem -Dit.db.url: testes de integração pulados.");
    }

    @DynamicPropertySource
    static void banco(DynamicPropertyRegistry registry) {
        if (URL_EXTERNA != null) {
            registry.add("spring.datasource.url", () -> URL_EXTERNA);
            registry.add("spring.datasource.username", () -> propriedadeOu("it.db.user", "raizes"));
            registry.add("spring.datasource.password", () -> propriedadeOu("it.db.password", "raizes_dev"));
        } else if (POSTGRES != null) {
            registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
            registry.add("spring.datasource.username", POSTGRES::getUsername);
            registry.add("spring.datasource.password", POSTGRES::getPassword);
        }
    }

    /** Isola os testes: limpa vendas, clientes e plantas; mantém os tipos do seed (V3) e os administradores. */
    @BeforeEach
    void limparBanco() {
        jdbc.execute("TRUNCATE itens_pedido, pedidos, clientes, plantas CASCADE");
        jdbc.update("DELETE FROM tipos_planta WHERE nome NOT IN ('Suculentas', 'Folhagens', 'Frutíferas', 'Medicinais')");
        relogio.resetar();
    }

    protected String toJson(Object o) throws Exception {
        return json.writeValueAsString(o);
    }
}
