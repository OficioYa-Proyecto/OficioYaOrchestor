package eci.edu.dosw.orchestrator.controller;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Standardized Data Transfer Object (DTO) for API error responses.
 * Used globally to format HTTP error payloads consistently across the application,
 * providing the frontend with predictable 'error' and 'message' fields.
 */
@Schema(description = "Estructura estandarizada de respuesta de error en la plataforma")
public record ErrorResponse(
        @Schema(description = "Tipo o nombre del error", example = "InvalidCredentialsException")
        String error,

        @Schema(description = "Mensaje detallado del error para el cliente", example = "Usuario o contraseña incorrectos, por favor rectifique sus credenciales.")
        String message) {

    public ErrorResponse(String message) {
        this(message, message);
    }
}
