package br.com.raizesdahistoria.usuarios.internal.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("raizes.jwt")
public record JwtProperties(String secret, long expiracaoHoras) {
}
