package br.com.raizesdahistoria.shared.exception;

import org.springframework.http.HttpStatus;

/** Base das exceções de negócio. Mapeada para RFC 7807 pelo {@link GlobalExceptionHandler}. */
public abstract class ApiException extends RuntimeException {

    private final HttpStatus status;
    private final String titulo;

    protected ApiException(HttpStatus status, String titulo, String mensagem) {
        super(mensagem);
        this.status = status;
        this.titulo = titulo;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getTitulo() {
        return titulo;
    }
}
