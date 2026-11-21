package br.com.raizesdahistoria.shared.util;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class MascaramentoTest {

    @Test
    void mascaraDadosPessoais() {
        assertThat(Mascaramento.cpf("52998224725")).isEqualTo("***.***.***-25");
        assertThat(Mascaramento.email("maria@email.com")).isEqualTo("m***@email.com");
        assertThat(Mascaramento.telefone("11999998888")).isEqualTo("*******8888");
        assertThat(Mascaramento.cpf(null)).isEqualTo("***");
        assertThat(Mascaramento.email("invalido")).isEqualTo("***");
    }
}
