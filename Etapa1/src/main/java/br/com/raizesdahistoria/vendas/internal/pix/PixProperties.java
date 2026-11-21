package br.com.raizesdahistoria.vendas.internal.pix;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("pix")
public record PixProperties(
        String provedor,
        String chave,
        int expiracaoMinutos,
        String webhookSecret,
        String recebedorNome,
        String recebedorCidade) {

    public PixProperties {
        if (expiracaoMinutos <= 0) {
            expiracaoMinutos = 30;
        }
        if (recebedorNome == null || recebedorNome.isBlank()) {
            recebedorNome = "RAIZES DA HISTORIA";
        }
        if (recebedorCidade == null || recebedorCidade.isBlank()) {
            recebedorCidade = "SAO PAULO";
        }
    }
}
