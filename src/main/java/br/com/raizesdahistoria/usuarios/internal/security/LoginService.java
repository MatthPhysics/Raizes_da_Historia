package br.com.raizesdahistoria.usuarios.internal.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.raizesdahistoria.shared.exception.NaoAutorizadoException;
import br.com.raizesdahistoria.usuarios.AdministradorService;
import br.com.raizesdahistoria.usuarios.internal.dominio.AdministradorRepository;

@Service
class LoginService {

    private static final Logger log = LoggerFactory.getLogger(LoginService.class);
    private static final String MENSAGEM_GENERICA = "E-mail ou senha inválidos.";

    private final AdministradorRepository repository;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;
    /** Hash fictício para que e-mail inexistente custe o mesmo tempo que senha errada (evita enumeração). */
    private final String hashFicticio;

    LoginService(AdministradorRepository repository, PasswordEncoder passwordEncoder, TokenService tokenService) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
        this.hashFicticio = passwordEncoder.encode("senha-ficticia-para-tempo-constante");
    }

    @Transactional(readOnly = true)
    public TokenService.TokenEmitido autenticar(String email, String senha) {
        var admin = repository.findByEmail(AdministradorService.normalizarEmail(email));
        if (admin.isEmpty()) {
            passwordEncoder.matches(senha, hashFicticio);
            log.info("Falha de login: credenciais inválidas");
            throw new NaoAutorizadoException(MENSAGEM_GENERICA);
        }
        if (!passwordEncoder.matches(senha, admin.get().getSenhaHash())) {
            log.info("Falha de login: credenciais inválidas");
            throw new NaoAutorizadoException(MENSAGEM_GENERICA);
        }
        log.info("Login do administrador {}", admin.get().getId());
        return tokenService.emitirParaAdmin(admin.get().getId(), admin.get().getEmail());
    }
}
