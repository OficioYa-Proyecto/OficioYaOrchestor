package eci.edu.dosw.orchestrator.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Petición para solicitar el envío del código OTP de verificación antes de registrar una cuenta")
public record SendRegisterOtpRequest(
        @Schema(description = "Correo electrónico real donde se enviará el código de seguridad de 6 dígitos", example = "usuario@gmail.com", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "El correo es obligatorio")
        @Email(message = "El formato de correo no es válido")
        String email) {
}
