package br.com.raizesdahistoria.usuarios;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.raizesdahistoria.shared.exception.ConflitoException;
import br.com.raizesdahistoria.shared.exception.RecursoNaoEncontradoException;
import br.com.raizesdahistoria.shared.util.Mascaramento;
import br.com.raizesdahistoria.usuarios.internal.dominio.Administrador;
import br.com.raizesdahistoria.usuarios.internal.dominio.AdministradorRepository;

/** API pública do módulo para administradores. Não há cadastro público: só um admin autenticado cadastra outro. */
@Service
public class AdministradorService {

    private static final Logger log = LoggerFactory.getLogger(AdministradorService.class);

    private final AdministradorRepository repository;
    private final PasswordEncoder passwordEncoder;
    private final Clock clock;

    public AdministradorService(AdministradorRepository repository, PasswordEncoder passwordEncoder, Clock clock) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
        this.clock = clock;
    }

    @Transactional
    public AdministradorDto cadastrar(String nome, String email, String senha) {
        String emailNormalizado = normalizarEmail(email);
        if (repository.existsByEmail(emailNormalizado)) {
            throw new ConflitoException("Já existe um administrador com este e-mail.");
        }
        var admin = repository.save(new Administrador(nome.trim(), emailNormalizado,
                passwordEncoder.encode(senha), Instant.now(clock)));
        log.info("Administrador {} cadastrado ({})", admin.getId(), Mascaramento.email(admin.getEmail()));
        return toDto(admin);
    }

    @Transactional(readOnly = true)
    public AdministradorDto buscar(UUID id) {
        return repository.findById(id).map(AdministradorService::toDto)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Administrador não encontrado."));
    }

    @Transactional(readOnly = true)
    public boolean existeAlgum() {
        return repository.count() > 0;
    }

    public static String normalizarEmail(String email) {
        return email == null ? null : email.trim().toLowerCase();
    }

    private static AdministradorDto toDto(Administrador a) {
        return new AdministradorDto(a.getId(), a.getNome(), a.getEmail(), a.getCriadoEm());
    }
}
