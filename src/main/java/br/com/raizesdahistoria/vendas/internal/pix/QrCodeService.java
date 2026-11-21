package br.com.raizesdahistoria.vendas.internal.pix;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;

/** Gera o QR Code PNG (Base64) a partir do copia-e-cola. Gerado sob demanda; a imagem nunca é persistida. */
@Service
public class QrCodeService {

    private static final int TAMANHO_PX = 300;

    public String gerarPngBase64(String conteudo) {
        try {
            var matriz = new QRCodeWriter().encode(conteudo, BarcodeFormat.QR_CODE, TAMANHO_PX, TAMANHO_PX,
                    Map.of(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M,
                            EncodeHintType.CHARACTER_SET, "UTF-8",
                            EncodeHintType.MARGIN, 2));
            var out = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(matriz, "PNG", out);
            return Base64.getEncoder().encodeToString(out.toByteArray());
        } catch (WriterException | IOException e) {
            throw new IllegalStateException("Falha ao gerar QR Code", e);
        }
    }
}
