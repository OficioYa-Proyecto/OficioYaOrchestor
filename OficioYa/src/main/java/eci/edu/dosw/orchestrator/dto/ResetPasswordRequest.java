package eci.edu.dosw.orchestrator.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Petición para restablecer la contraseña usando el código OTP recibido")
public record ResetPasswordRequest(
        @Schema(description = "Correo electrónico de la cuenta", example = "usuario@gmail.com", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank @Email String email,

        @Schema(description = "Código OTP numérico de 6 dígitos recibido por correo", example = "123456", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank String code,

        @Schema(description = "Nueva contraseña segura", example = "NuevaSegura#2026!Ab", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank String newPassword) {
}
