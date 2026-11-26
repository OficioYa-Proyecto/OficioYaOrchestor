package eci.edu.dosw.orchestrator.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Petición para cambio voluntario de contraseña de usuario autenticado")
public record ChangePasswordRequest(
        @Schema(description = "Contraseña actual del usuario", example = "ActualPassword123*", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank String currentPassword,

        @Schema(description = "Nueva contraseña deseada", example = "NuevaPassword#2026!Ab", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank String newPassword) {
}
