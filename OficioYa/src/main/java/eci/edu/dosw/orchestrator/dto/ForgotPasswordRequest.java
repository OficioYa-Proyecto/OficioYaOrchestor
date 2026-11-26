package eci.edu.dosw.orchestrator.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Petición para solicitar código OTP de recuperación de contraseña")
public record ForgotPasswordRequest(
        @Schema(description = "Correo electrónico asociado a la cuenta registrada", example = "usuario@gmail.com", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank @Email String email) {
}
