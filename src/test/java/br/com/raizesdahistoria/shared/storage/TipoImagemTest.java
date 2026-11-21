package br.com.raizesdahistoria.shared.storage;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

class TipoImagemTest {

    @Test
    void detectaPorMagicBytes() {
        assertThat(TipoImagem.detectar(new byte[] {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 0x00})).contains(TipoImagem.JPEG);
        assertThat(TipoImagem.detectar(new byte[] {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A})).contains(TipoImagem.PNG);
        assertThat(TipoImagem.detectar("RIFF\0\0\0\0WEBPVP8 ".getBytes(StandardCharsets.ISO_8859_1))).contains(TipoImagem.WEBP);
    }

    @Test
    void rejeitaOutrosFormatosMesmoComExtensaoDeImagem() {
        assertThat(TipoImagem.detectar("%PDF-1.7 ...".getBytes(StandardCharsets.US_ASCII))).isEmpty();
        assertThat(TipoImagem.detectar("GIF89a".getBytes(StandardCharsets.US_ASCII))).isEmpty();
        assertThat(TipoImagem.detectar(new byte[0])).isEmpty();
        assertThat(TipoImagem.detectar(null)).isEmpty();
    }
}
