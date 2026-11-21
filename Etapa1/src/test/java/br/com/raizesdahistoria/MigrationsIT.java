package br.com.raizesdahistoria;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

class MigrationsIT extends AbstractIntegrationTest {

    @Test
    void aplicaV1aV4EmBancoVazio() {
        var versoes = jdbc.queryForList(
                "SELECT version FROM flyway_schema_history WHERE success ORDER BY installed_rank", String.class);
        assertThat(versoes).containsExactly("1", "2", "3", "4");

        var tabelas = jdbc.queryForList(
                "SELECT table_name FROM information_schema.tables WHERE table_schema = 'public'", String.class);
        assertThat(tabelas).contains("administradores", "clientes", "tipos_planta", "plantas", "pedidos", "itens_pedido");
    }

    @Test
    void carregaTiposIniciais() {
        assertThat(jdbc.queryForList("SELECT nome FROM tipos_planta ORDER BY id", String.class))
                .containsExactly("Suculentas", "Folhagens", "Frutíferas", "Medicinais");
    }

    @Test
    void funcaoSemAcentoEhImutavelEFunciona() {
        assertThat(jdbc.queryForObject("SELECT f_unaccent(lower('Jibóia Ação'))", String.class)).isEqualTo("jiboia acao");
        assertThat(jdbc.queryForObject("SELECT provolatile FROM pg_proc WHERE proname = 'f_unaccent'", String.class))
                .isEqualTo("i");
    }

    @Test
    void constraintsProtegemOsDados() {
        Integer tipo = jdbc.queryForObject("SELECT id FROM tipos_planta LIMIT 1", Integer.class);
        assertThatThrownBy(() -> jdbc.update("INSERT INTO plantas (tipo_id, nome_popular, preco, frequencia_rega, exposicao_sol) "
                + "VALUES (?, 'X', 0, 'DIARIA', 'PLENO')", tipo)).isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> jdbc.update("INSERT INTO plantas (tipo_id, nome_popular, preco, frequencia_rega, exposicao_sol) "
                + "VALUES (?, 'X', 10, 'TODO_DIA', 'PLENO')", tipo)).isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> jdbc.update("INSERT INTO clientes (nome, email, telefone_whatsapp, cpf) "
                + "VALUES ('X', 'x@x.com', '11999998888', '123')")).isInstanceOf(DataIntegrityViolationException.class);
    }
}
