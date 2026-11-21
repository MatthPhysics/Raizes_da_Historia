package br.com.raizesdahistoria.usuarios.internal.dominio;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AdministradorRepository extends JpaRepository<Administrador, UUID> {

    Optional<Administrador> findByEmail(String email);

    boolean existsByEmail(String email);
}
