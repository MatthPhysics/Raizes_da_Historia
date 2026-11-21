package br.com.raizesdahistoria.catalogo.internal.dominio;

import org.springframework.data.jpa.domain.Specification;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;

/** Filtros do catálogo. Cada método devolve {@code null} quando o filtro não foi informado (ignorado). */
public final class PlantaSpecifications {

    private PlantaSpecifications() {
    }

    public static Specification<Planta> ativa(Boolean ativo) {
        return ativo == null ? null : (root, q, cb) -> cb.equal(root.get("ativo"), ativo);
    }

    public static Specification<Planta> doTipo(Integer tipoId) {
        return tipoId == null ? null : (root, q, cb) -> cb.equal(root.get("tipo").get("id"), tipoId);
    }

    public static Specification<Planta> comRega(FrequenciaRega rega) {
        return rega == null ? null : (root, q, cb) -> cb.equal(root.get("frequenciaRega"), rega);
    }

    public static Specification<Planta> comSol(ExposicaoSol sol) {
        return sol == null ? null : (root, q, cb) -> cb.equal(root.get("exposicaoSol"), sol);
    }

    /**
     * Busca parcial, sem acento e sem caixa, em nome popular e científico:
     * {@code f_unaccent(lower(coluna)) LIKE f_unaccent(lower('%termo%'))} — mesma expressão dos índices trigram (V3).
     */
    public static Specification<Planta> nomeContem(String termo) {
        if (termo == null || termo.isBlank()) {
            return null;
        }
        String padrao = "%" + escaparLike(termo.trim()) + "%";
        return (root, q, cb) -> {
            Expression<String> busca = semAcento(cb, cb.literal(padrao));
            return cb.or(
                    cb.like(semAcento(cb, root.get("nomePopular")), busca, '\\'),
                    cb.like(semAcento(cb, root.get("nomeCientifico")), busca, '\\'));
        };
    }

    private static Expression<String> semAcento(CriteriaBuilder cb, Expression<String> expr) {
        return cb.function("f_unaccent", String.class, cb.lower(expr));
    }

    private static String escaparLike(String s) {
        return s.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}
