package br.com.raizesdahistoria.usuarios;

import java.time.Instant;
import java.util.UUID;

/** Dados pessoais do cliente. Expor apenas em rotas administrativas (LGPD). */
public record ClienteDto(UUID id, String nome, String email, String telefoneWhatsapp, String cpf, Instant criadoEm) {
}
