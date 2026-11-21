package br.com.raizesdahistoria.vendas.internal.pedido;

import br.com.raizesdahistoria.shared.exception.ConflitoException;

public class TransicaoStatusInvalidaException extends ConflitoException {

    public TransicaoStatusInvalidaException(String mensagem) {
        super(mensagem);
    }
}
