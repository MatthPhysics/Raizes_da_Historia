package br.com.raizesdahistoria;

import java.math.BigDecimal;
import java.util.UUID;

import org.springframework.jdbc.core.JdbcTemplate;

/** Inserções diretas para preparar cenários de teste. */
public final class Fixtures {

    private Fixtures() {
    }

    public static UUID planta(JdbcTemplate jdbc, String nomePopular, String nomeCientifico, String preco,
            String rega, String sol, int quantidade, boolean ativo, String tipo) {
        Integer tipoId = jdbc.queryForObject("SELECT id FROM tipos_planta WHERE nome = ?", Integer.class, tipo);
        return jdbc.queryForObject("""
                INSERT INTO plantas (tipo_id, nome_popular, nome_cientifico, preco, frequencia_rega, exposicao_sol,
                                     quantidade_pronta_venda, ativo)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?) RETURNING id
                """, UUID.class, tipoId, nomePopular, nomeCientifico, new BigDecimal(preco), rega, sol, quantidade, ativo);
    }

    public static UUID planta(JdbcTemplate jdbc, String nomePopular, String preco, int quantidade) {
        return planta(jdbc, nomePopular, null, preco, "SEMANAL", "MEIA_SOMBRA", quantidade, true, "Folhagens");
    }

    public static int estoque(JdbcTemplate jdbc, UUID plantaId) {
        return jdbc.queryForObject("SELECT quantidade_pronta_venda FROM plantas WHERE id = ?", Integer.class, plantaId);
    }
}
