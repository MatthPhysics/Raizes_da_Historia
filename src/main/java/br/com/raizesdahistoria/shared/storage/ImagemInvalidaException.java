package br.com.raizesdahistoria.shared.storage;

import br.com.raizesdahistoria.shared.exception.ApiException;
import org.springframework.http.HttpStatus;

public class ImagemInvalidaException extends ApiException {

    public ImagemInvalidaException(String mensagem) {
        super(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Imagem inválida", mensagem);
    }
}
