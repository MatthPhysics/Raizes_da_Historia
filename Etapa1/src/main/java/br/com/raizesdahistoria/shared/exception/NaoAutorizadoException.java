package br.com.raizesdahistoria.shared.exception;

import org.springframework.http.HttpStatus;

public class NaoAutorizadoException extends ApiException {

    public NaoAutorizadoException(String mensagem) {
        super(HttpStatus.UNAUTHORIZED, "Não autorizado", mensagem);
    }
}
