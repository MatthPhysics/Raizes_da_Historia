package br.com.raizesdahistoria.catalogo.internal;

import br.com.raizesdahistoria.shared.exception.RecursoNaoEncontradoException;

public class PlantaNaoEncontradaException extends RecursoNaoEncontradoException {

    public PlantaNaoEncontradaException() {
        super("Planta não encontrada.");
    }
}
