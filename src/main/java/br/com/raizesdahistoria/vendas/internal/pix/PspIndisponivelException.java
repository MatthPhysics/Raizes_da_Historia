package br.com.raizesdahistoria.vendas.internal.pix;

import org.springframework.http.HttpStatus;

import br.com.raizesdahistoria.shared.exception.ApiException;

public class PspIndisponivelException extends ApiException {

    public PspIndisponivelException(String detalheInterno, Throwable causa) {
        super(HttpStatus.BAD_GATEWAY, "Pagamento indisponível",
                "Não foi possível gerar a cobrança Pix no momento. Tente novamente em instantes.");
        if (causa != null) {
            initCause(causa);
        }
    }
}
