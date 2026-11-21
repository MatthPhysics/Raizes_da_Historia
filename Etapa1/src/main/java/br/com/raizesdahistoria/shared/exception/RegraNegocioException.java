package br.com.raizesdahistoria.shared.exception;

import java.util.List;

import org.springframework.http.HttpStatus;

/** Violação de regra de negócio (422). Pode carregar uma lista de problemas detalhados. */
public class RegraNegocioException extends ApiException {

    private final List<String> problemas;

    public RegraNegocioException(String mensagem) {
        this(mensagem, List.of());
    }

    public RegraNegocioException(String mensagem, List<String> problemas) {
        super(HttpStatus.UNPROCESSABLE_ENTITY, "Regra de negócio violada", mensagem);
        this.problemas = List.copyOf(problemas);
    }

    public List<String> getProblemas() {
        return problemas;
    }
}
