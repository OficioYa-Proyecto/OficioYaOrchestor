package eci.edu.dosw.orchestrator.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.UUID;

@Schema(description = "Respuesta de autenticación exitosa con token JWT y roles")
public record LoginResponse(
        @Schema(description = "Token de acceso JWT en formato Bearer", example = "eyJhbGciOiJIUzM4NCIsInR5cCI6IkpXVCJ9...")
        String token,

        @Schema(description = "Tiempo de validez del token en segundos (por defecto 900 = 15 minutos)", example = "900")
        long expiresInSeconds,

        @Schema(description = "Identificador único (UUID) del usuario autenticado", example = "717cfb45-903b-4825-8c97-fb54e98b5bd9")
        UUID userId,

        @Schema(description = "Lista de roles asignados al usuario en la plataforma", example = "[\"CONTRATANTE\"]")
        List<String> roles) {
}
