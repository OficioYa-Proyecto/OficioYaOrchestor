package eci.edu.dosw.orchestrator.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(description = "Información de teléfono del usuario")
public record PhoneDTO(
        @Schema(description = "Número telefónico (10 dígitos en Colombia)", example = "3104567890", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "El número de teléfono es obligatorio") String number,

        @Schema(description = "Indica si es el teléfono de contacto principal", example = "true", defaultValue = "false")
        Boolean principal) {

    public PhoneDTO {
        if (principal == null) {
            principal = false;
        }
    }
}
