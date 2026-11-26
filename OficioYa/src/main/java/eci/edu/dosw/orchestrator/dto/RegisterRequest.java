package eci.edu.dosw.orchestrator.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

@Schema(description = "Petición de registro de un nuevo usuario en la plataforma (Saga Orquestada)")
public record RegisterRequest(
        @Schema(description = "Nombre completo del usuario", example = "Carlos López", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "El nombre es obligatorio") String name,

        @Schema(description = "Correo electrónico del usuario", example = "carlos.lopez@gmail.com", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "El correo es obligatorio") @Email(message = "El formato de correo no es válido") String email,

        @Schema(description = "Contraseña segura (mínimo 8 caracteres, mayúscula, minúscula, número y símbolo)", example = "Segura#2026!Ab", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "La contraseña es obligatoria") String password,

        @Schema(description = "Lista de teléfonos de contacto del usuario", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotEmpty(message = "Debe ingresar al menos un teléfono") List<@Valid PhoneDTO> phones,

        @Schema(description = "Roles iniciales a asignar (ej. CONTRATANTE o TRABAJADOR)", example = "[\"CONTRATANTE\"]", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotEmpty(message = "Debe seleccionar al menos un rol") List<String> roles,

        @Schema(description = "Fecha de nacimiento en formato ISO (YYYY-MM-DD)", example = "2000-05-15")
        String birthDate,

        @Schema(description = "Identificador de localidad de residencia (Bogotá)", example = "1")
        Integer localityId,

        @Schema(description = "Código de verificación OTP de 6 dígitos recibido por correo", example = "123456")
        String code) {

    public RegisterRequest(
            String name,
            String email,
            String password,
            List<PhoneDTO> phones,
            List<String> roles,
            String birthDate,
            Integer localityId) {
        this(name, email, password, phones, roles, birthDate, localityId, null);
    }

    public RegisterRequest(
            String name,
            String email,
            String password,
            List<PhoneDTO> phones,
            List<String> roles,
            String birthDate) {
        this(name, email, password, phones, roles, birthDate, null);
    }

    public RegisterRequest(
            String name,
            String email,
            String password,
            List<PhoneDTO> phones,
            List<String> roles) {
        this(name, email, password, phones, roles, null, null);
    }

    public RegisterRequest {
        if (roles != null) {
            roles = roles.stream()
                    .map(r -> switch (r.toUpperCase()) {
                        case "CLIENT", "CONTRATANTE" -> "CONTRATANTE";
                        case "WORKER", "TRABAJADOR" -> "TRABAJADOR";
                        default -> r.toUpperCase();
                    })
                    .toList();
        }
    }
}
