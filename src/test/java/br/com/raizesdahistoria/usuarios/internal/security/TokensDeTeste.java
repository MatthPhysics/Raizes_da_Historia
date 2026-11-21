package br.com.raizesdahistoria.usuarios.internal.security;

import org.springframework.stereotype.Component;

import br.com.raizesdahistoria.usuarios.internal.dominio.AdministradorRepository;

/** Utilitário de testes: emite JWT para o admin inicial sem passar pelo login (que tem limite por IP). */
@Component
public class TokensDeTeste {

    private final TokenService tokenService;
    private final AdministradorRepository repository;
    private final LoginRateLimiter rateLimiter;

    TokensDeTeste(TokenService tokenService, AdministradorRepository repository, LoginRateLimiter rateLimiter) {
        this.tokenService = tokenService;
        this.repository = repository;
        this.rateLimiter = rateLimiter;
    }

    public String bearerAdmin() {
        var admin = repository.findByEmail("admin@teste.dev").orElseThrow();
        return "Bearer " + tokenService.emitirParaAdmin(admin.getId(), admin.getEmail()).token();
    }

    public void liberarLogin() {
        rateLimiter.limpar();
    }
}
