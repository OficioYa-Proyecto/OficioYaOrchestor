package eci.edu.dosw.orchestrator;

import eci.edu.dosw.orchestrator.exception.InvalidOtpException;
import eci.edu.dosw.orchestrator.exception.OtpRateLimitException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Pruebas unitarias de RegistrationOtpService")
class RegistrationOtpServiceTest {

    private static final String EMAIL = "usuario.real@gmail.com";
    private MutableClock clock;
    private RegistrationOtpService otpService;

    static class MutableClock extends Clock {
        private Instant currentInstant;
        private final ZoneId zone;

        MutableClock(Instant start, ZoneId zone) {
            this.currentInstant = start;
            this.zone = zone;
        }

        @Override
        public ZoneId getZone() {
            return zone;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return new MutableClock(currentInstant, zone);
        }

        @Override
        public Instant instant() {
            return currentInstant;
        }

        public void advance(Duration duration) {
            currentInstant = currentInstant.plus(duration);
        }
    }

    @BeforeEach
    void setUp() {
        clock = new MutableClock(Instant.parse("2026-11-25T12:00:00Z"), ZoneId.of("UTC"));
        otpService = new RegistrationOtpService(clock);
    }

    @Test
    @DisplayName("Debe generar un código de 6 dígitos válido")
    void debeGenerarCodigoSeisDigitos() {
        String code = otpService.generateCode(EMAIL);
        assertNotNull(code);
        assertEquals(6, code.length());
        assertTrue(code.matches("\\d{6}"));
    }

    @Test
    @DisplayName("Debe verificar exitosamente el código generado y consumirlo")
    void debeVerificarExitosamenteYConsumirCodigo() {
        String code = otpService.generateCode(EMAIL);
        assertDoesNotThrow(() -> otpService.verifyCode(EMAIL, code));

        // Segundo intento con el mismo código debe fallar porque ya fue consumido
        assertThrows(InvalidOtpException.class, () -> otpService.verifyCode(EMAIL, code));
    }

    @Test
    @DisplayName("Debe fallar si el código es incorrecto")
    void debeFallarSiCodigoEsIncorrecto() {
        otpService.generateCode(EMAIL);
        InvalidOtpException ex = assertThrows(InvalidOtpException.class, () -> otpService.verifyCode(EMAIL, "000000"));
        assertEquals("Código de verificación incorrecto.", ex.getMessage());
    }

    @Test
    @DisplayName("Debe bloquear tras 3 intentos fallidos")
    void debeBloquearTrasTresIntentosFallidos() {
        otpService.generateCode(EMAIL);

        assertThrows(InvalidOtpException.class, () -> otpService.verifyCode(EMAIL, "111111"));
        assertThrows(InvalidOtpException.class, () -> otpService.verifyCode(EMAIL, "222222"));
        assertThrows(InvalidOtpException.class, () -> otpService.verifyCode(EMAIL, "333333"));

        // El 4to intento debe indicar que se excedió el número máximo de intentos
        InvalidOtpException ex = assertThrows(InvalidOtpException.class, () -> otpService.verifyCode(EMAIL, "444444"));
        assertTrue(ex.getMessage().contains("Número máximo de intentos excedido"));
    }

    @Test
    @DisplayName("Debe fallar si el código ha expirado (más de 10 minutos)")
    void debeFallarSiCodigoHaExpirado() {
        String code = otpService.generateCode(EMAIL);
        clock.advance(Duration.ofMinutes(11));

        InvalidOtpException ex = assertThrows(InvalidOtpException.class, () -> otpService.verifyCode(EMAIL, code));
        assertTrue(ex.getMessage().contains("ha expirado o no existe"));
    }

    @Test
    @DisplayName("Debe aplicar rate limiting si se solicita otro código antes de 60 segundos")
    void debeAplicarRateLimitingSiSolicitaAntesDe60Segundos() {
        otpService.generateCode(EMAIL);

        clock.advance(Duration.ofSeconds(30));
        assertThrows(OtpRateLimitException.class, () -> otpService.generateCode(EMAIL));

        clock.advance(Duration.ofSeconds(31));
        assertDoesNotThrow(() -> otpService.generateCode(EMAIL));
    }

    @Test
    @DisplayName("Debe rechazar código nulo o vacío")
    void debeRechazarCodigoNuloOVacio() {
        assertThrows(InvalidOtpException.class, () -> otpService.verifyCode(EMAIL, null));
        assertThrows(InvalidOtpException.class, () -> otpService.verifyCode(EMAIL, "   "));
    }

    @Test
    @DisplayName("Debe limpiar el almacén con clear")
    void debeLimpiarAlmacenConClear() {
        String code = otpService.generateCode(EMAIL);
        otpService.clear();
        assertThrows(InvalidOtpException.class, () -> otpService.verifyCode(EMAIL, code));
    }
}
