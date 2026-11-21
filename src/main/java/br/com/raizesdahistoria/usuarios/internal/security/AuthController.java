package br.com.raizesdahistoria.usuarios.internal.security;

import java.time.Instant;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Autenticação")
class AuthController {

    private final LoginService loginService;
    private final LoginRateLimiter rateLimiter;

    AuthController(LoginService loginService, LoginRateLimiter rateLimiter) {
        this.loginService = loginService;
        this.rateLimiter = rateLimiter;
    }

    @PostMapping("/login")
    @Operation(summary = "Login de administrador", description = "Retorna um JWT Bearer. Limite: 5 tentativas por minuto por IP.")
    LoginResponse login(@Valid @RequestBody LoginRequest request, HttpServletRequest http) {
        rateLimiter.registrarTentativa(http.getRemoteAddr());
        var token = loginService.autenticar(request.email(), request.senha());
        return new LoginResponse(token.token(), "Bearer", token.expiraEm());
    }

    record LoginRequest(
            @NotBlank(message = "O e-mail é obrigatório.") @Schema(example = "admin@raizes.dev") String email,
            @NotBlank(message = "A senha é obrigatória.") @Schema(example = "admin12345") String senha) {
    }

    record LoginResponse(String accessToken, String tipo, Instant expiraEm) {
    }
}
