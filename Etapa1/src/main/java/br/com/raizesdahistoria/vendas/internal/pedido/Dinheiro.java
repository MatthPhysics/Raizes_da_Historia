package br.com.raizesdahistoria.vendas.internal.pedido;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Regras monetárias: {@code BigDecimal}, escala 2, {@code HALF_EVEN}. Nunca {@code double}. */
public final class Dinheiro {

    private Dinheiro() {
    }

    public static BigDecimal arredondar(BigDecimal valor) {
        return valor.setScale(2, RoundingMode.HALF_EVEN);
    }

    public static BigDecimal multiplicar(BigDecimal preco, int quantidade) {
        return arredondar(preco.multiply(BigDecimal.valueOf(quantidade)));
    }

    public static boolean iguais(BigDecimal a, BigDecimal b) {
        return a != null && b != null && arredondar(a).compareTo(arredondar(b)) == 0;
    }
}
