package br.com.raizesdahistoria.vendas.internal.pix;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashSet;

import org.junit.jupiter.api.Test;

class GeradorTxidTest {

    @Test
    void geraTxidAlfanumericoEntre26E35CaracteresEUnico() {
        var gerador = new GeradorTxid();
        var vistos = new HashSet<String>();
        for (int i = 0; i < 1000; i++) {
            String txid = gerador.gerar();
            assertThat(txid).matches("^[A-Za-z0-9]{26,35}$");
            assertThat(vistos.add(txid)).isTrue();
        }
    }
}
