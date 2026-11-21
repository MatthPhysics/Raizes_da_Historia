package br.com.raizesdahistoria.usuarios;

import br.com.raizesdahistoria.shared.validation.Cpf;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Dados do comprador informados no checkout rápido. */
public record NovoCliente(
        @NotBlank(message = "O nome é obrigatório.")
        @Size(max = 120, message = "O nome deve ter no máximo 120 caracteres.")
        @Schema(example = "Maria Silva")
        String nome,

        @NotBlank(message = "O e-mail é obrigatório.")
        @Email(message = "E-mail inválido.")
        @Size(max = 160, message = "O e-mail deve ter no máximo 160 caracteres.")
        @Schema(example = "maria@email.com")
        String email,

        @NotBlank(message = "O telefone (WhatsApp) é obrigatório.")
        @Pattern(regexp = "^[1-9]{2}[0-9]{8,9}$",
                message = "Telefone deve conter DDD + número, somente dígitos (10 ou 11 dígitos).")
        @Schema(example = "11999998888")
        String telefoneWhatsapp,

        @Cpf
        @Schema(description = "Opcional. Somente dígitos.", example = "52998224725", nullable = true)
        String cpf) {
}
