package br.com.raizesdahistoria.usuarios;

import java.time.Instant;
import java.util.UUID;

public record AdministradorDto(UUID id, String nome, String email, Instant criadoEm) {
}
