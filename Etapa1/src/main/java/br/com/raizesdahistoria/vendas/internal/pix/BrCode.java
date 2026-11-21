package br.com.raizesdahistoria.vendas.internal.pix;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;

/**
 * Montagem de BR Code (EMV® QRCPS-MPM) para Pix "copia e cola".
 * Usado pelo {@link MockPixGateway}; em produção o copia-e-cola vem do PSP.
 */
public final class BrCode {

    private static final String GUI_PIX = "br.gov.bcb.pix";

    private BrCode() {
    }

    /** BR Code dinâmico: aponta para a URL de location da cobrança (campo 26-25). */
    public static String dinamico(String location, BigDecimal valor, String nomeRecebedor, String cidade) {
        String contaPix = campo("00", GUI_PIX) + campo("25", location);
        String payload = campo("00", "01")
                + campo("01", "12")
                + campo("26", contaPix)
                + campo("52", "0000")
                + campo("53", "986")
                + campo("54", valor.setScale(2, RoundingMode.HALF_EVEN).toPlainString())
                + campo("58", "BR")
                + campo("59", ascii(nomeRecebedor, 25))
                + campo("60", ascii(cidade, 15))
                + campo("62", campo("05", "***"))
                + "6304";
        return payload + crc16(payload);
    }

    static String campo(String id, String valor) {
        int tamanho = valor.length();
        if (tamanho > 99) {
            throw new IllegalArgumentException("Campo " + id + " excede 99 caracteres");
        }
        return id + String.format("%02d", tamanho) + valor;
    }

    /** CRC16-CCITT-FALSE (polinômio 0x1021, valor inicial 0xFFFF), em hexadecimal maiúsculo com 4 dígitos. */
    static String crc16(String payload) {
        int crc = 0xFFFF;
        for (byte b : payload.getBytes(StandardCharsets.UTF_8)) {
            crc ^= (b & 0xFF) << 8;
            for (int i = 0; i < 8; i++) {
                crc = (crc & 0x8000) != 0 ? (crc << 1) ^ 0x1021 : crc << 1;
                crc &= 0xFFFF;
            }
        }
        return String.format("%04X", crc);
    }

    private static String ascii(String s, int max) {
        String semAcento = Normalizer.normalize(s, Normalizer.Form.NFD).replaceAll("\\p{M}", "")
                .replaceAll("[^A-Za-z0-9 ]", "").toUpperCase();
        return semAcento.length() > max ? semAcento.substring(0, max) : semAcento;
    }
}
