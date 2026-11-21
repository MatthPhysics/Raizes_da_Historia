package br.com.raizesdahistoria.shared.storage;

import java.util.Optional;

/** Detecção do tipo de imagem pelo conteúdo (magic bytes), não pela extensão nem pelo Content-Type informado. */
public enum TipoImagem {
    JPEG("jpg", "image/jpeg"),
    PNG("png", "image/png"),
    WEBP("webp", "image/webp");

    private final String extensao;
    private final String mediaType;

    TipoImagem(String extensao, String mediaType) {
        this.extensao = extensao;
        this.mediaType = mediaType;
    }

    public String extensao() {
        return extensao;
    }

    public String mediaType() {
        return mediaType;
    }

    public static Optional<TipoImagem> detectar(byte[] b) {
        if (b == null) {
            return Optional.empty();
        }
        if (b.length >= 3 && (b[0] & 0xFF) == 0xFF && (b[1] & 0xFF) == 0xD8 && (b[2] & 0xFF) == 0xFF) {
            return Optional.of(JPEG);
        }
        if (b.length >= 8 && (b[0] & 0xFF) == 0x89 && b[1] == 'P' && b[2] == 'N' && b[3] == 'G'
                && b[4] == 0x0D && b[5] == 0x0A && b[6] == 0x1A && b[7] == 0x0A) {
            return Optional.of(PNG);
        }
        if (b.length >= 12 && b[0] == 'R' && b[1] == 'I' && b[2] == 'F' && b[3] == 'F'
                && b[8] == 'W' && b[9] == 'E' && b[10] == 'B' && b[11] == 'P') {
            return Optional.of(WEBP);
        }
        return Optional.empty();
    }
}
