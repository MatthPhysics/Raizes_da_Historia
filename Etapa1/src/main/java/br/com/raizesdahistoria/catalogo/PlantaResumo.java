package br.com.raizesdahistoria.catalogo;

import java.math.BigDecimal;
import java.util.UUID;

/** Visão mínima de uma planta para outros módulos (carrinho e checkout). */
public record PlantaResumo(UUID id, String nomePopular, BigDecimal preco, int quantidadeProntaVenda, boolean ativo) {
}
