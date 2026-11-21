package br.com.raizesdahistoria.usuarios.internal.web;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import br.com.raizesdahistoria.shared.config.OpenApiConfig;
import br.com.raizesdahistoria.usuarios.AdministradorDto;
import br.com.raizesdahistoria.usuarios.AdministradorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@RestController
@RequestMapping("/api/v1/admin/administradores")
@Tag(name = "Admin · Administradores")
@SecurityRequirement(name = OpenApiConfig.BEARER)
class AdministradorController {

    private final AdministradorService service;

    AdministradorController(AdministradorService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Cadastra um novo administrador (somente administradores autenticados)")
    AdministradorDto cadastrar(@Valid @RequestBody NovoAdministradorRequest request) {
        return service.cadastrar(request.nome(), request.email(), request.senha());
    }

    @GetMapping("/me")
    @Operation(summary = "Dados do administrador logado")
    AdministradorDto me(@AuthenticationPrincipal Jwt jwt) {
        return service.buscar(UUID.fromString(jwt.getSubject()));
    }

    record NovoAdministradorRequest(
            @NotBlank(message = "O nome é obrigatório.")
            @Size(max = 120, message = "O nome deve ter no máximo 120 caracteres.")
            String nome,
            @NotBlank(message = "O e-mail é obrigatório.")
            @Email(message = "E-mail inválido.")
            @Size(max = 160, message = "O e-mail deve ter no máximo 160 caracteres.")
            String email,
            @NotBlank(message = "A senha é obrigatória.")
            @Size(min = 8, max = 72, message = "A senha deve ter entre 8 e 72 caracteres.")
            String senha) {
    }
}
