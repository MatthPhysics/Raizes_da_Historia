package br.com.raizesdahistoria.vendas.internal.pix;

import java.security.SecureRandom;

import org.springframework.stereotype.Component;

/** txid Pix: 26–35 caracteres alfanuméricos ({@code [a-zA-Z0-9]}), gerado pelo back-end. Aqui: 32 caracteres. */
@Component
public class GeradorTxid {

    static final String PREFIXO = "RAIZES";
    static final int TAMANHO = 32;
    private static final char[] ALFABETO = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789".toCharArray();

    private final SecureRandom random = new SecureRandom();

    public String gerar() {
        var sb = new StringBuilder(TAMANHO).append(PREFIXO);
        while (sb.length() < TAMANHO) {
            sb.append(ALFABETO[random.nextInt(ALFABETO.length)]);
        }
        return sb.toString();
    }
}
