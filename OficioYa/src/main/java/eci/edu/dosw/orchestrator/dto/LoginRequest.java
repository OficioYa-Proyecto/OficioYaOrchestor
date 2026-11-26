package eci.edu.dosw.orchestrator.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Petición de inicio de sesión con credenciales de usuario")
public record LoginRequest(
        @Schema(description = "Correo electrónico registrado del usuario", example = "usuario@gmail.com", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank @Email String email,

        @Schema(description = "Contraseña de la cuenta", example = "Password123*", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank String password) {
}
