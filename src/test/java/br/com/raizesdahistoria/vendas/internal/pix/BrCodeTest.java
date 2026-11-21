package br.com.raizesdahistoria.vendas.internal.pix;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

class BrCodeTest {

    @Test
    void crc16CcittFalseVetorPadrao() {
        // Vetor de verificação conhecido do CRC-16/CCITT-FALSE
        assertThat(BrCode.crc16("123456789")).isEqualTo("29B1");
    }

    @Test
    void montaBrCodeDinamicoComCrcValido() {
        String codigo = BrCode.dinamico("pix.exemplo.com/v2/cob/ABC", new BigDecimal("79.8"), "Raízes da História", "São Paulo");

        assertThat(codigo).startsWith("000201010212");
        assertThat(codigo).contains("0014br.gov.bcb.pix");
        assertThat(codigo).contains("540579.80");
        assertThat(codigo).contains("5918RAIZES DA HISTORIA");
        assertThat(codigo).contains("6009SAO PAULO");
        String semCrc = codigo.substring(0, codigo.length() - 4);
        assertThat(semCrc).endsWith("6304");
        assertThat(codigo.substring(codigo.length() - 4)).isEqualTo(BrCode.crc16(semCrc));
    }
}
