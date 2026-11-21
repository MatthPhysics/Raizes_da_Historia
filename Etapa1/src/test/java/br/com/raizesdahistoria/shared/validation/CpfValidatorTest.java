package br.com.raizesdahistoria.shared.validation;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class CpfValidatorTest {

    @ParameterizedTest
    @ValueSource(strings = {"52998224725", "11144477735", "39053344705"})
    void aceitaCpfsValidos(String cpf) {
        assertThat(CpfValidator.valido(cpf)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"52998224724", "11111111111", "00000000000", "1234567890", "529.982.247-25", "abcdefghijk"})
    void rejeitaCpfsInvalidos(String cpf) {
        assertThat(CpfValidator.valido(cpf)).isFalse();
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "  "})
    void cpfVazioEhOpcional(String cpf) {
        assertThat(new CpfValidator().isValid(cpf, null)).isTrue();
        assertThat(new CpfValidator().isValid(null, null)).isTrue();
    }
}
