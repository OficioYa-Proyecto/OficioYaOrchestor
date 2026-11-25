package eci.edu.dosw.orchestrator;

import eci.edu.dosw.orchestrator.exception.InvalidOtpException;
import eci.edu.dosw.orchestrator.exception.OtpRateLimitException;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Gestiona la emisión y validación de códigos OTP en memoria para el proceso de registro.
 * - Código de 6 dígitos numérico criptográficamente seguro.
 * - Válido por 10 minutos.
 * - Almacena únicamente el hash SHA-256 en memoria (nunca el texto plano).
 * - Máximo 3 intentos fallidos por código antes de invalidarlo.
 * - Rate limiting: mínimo 60 segundos entre solicitudes para un mismo correo.
 */
@Service
public class RegistrationOtpService {

    public static final int VALID_MINUTES = 10;
    public static final int MAX_ATTEMPTS = 3;
    public static final Duration MIN_REQUEST_INTERVAL = Duration.ofSeconds(60);

    record OtpEntry(String codeHash, Instant expiresAt, int attempts, Instant createdAt) {}

    private final Clock clock;
    private final SecureRandom random = new SecureRandom();
    private final Map<String, OtpEntry> otpStore = new ConcurrentHashMap<>();

    public RegistrationOtpService() {
        this(Clock.systemUTC());
    }

    public RegistrationOtpService(Clock clock) {
        this.clock = clock;
    }

    public String generateCode(String email) {
        String normalizedEmail = normalizeEmail(email);
        Instant now = clock.instant();

        OtpEntry existing = otpStore.get(normalizedEmail);
        if (existing != null && Duration.between(existing.createdAt(), now).compareTo(MIN_REQUEST_INTERVAL) < 0) {
            throw new OtpRateLimitException("Debe esperar 60 segundos antes de solicitar otro código.");
        }

        int number = random.nextInt(1_000_000);
        String code = String.format("%06d", number);
        String hash = sha256(code);

        Instant expiresAt = now.plus(Duration.ofMinutes(VALID_MINUTES));
        otpStore.put(normalizedEmail, new OtpEntry(hash, expiresAt, 0, now));
        return code;
    }

    public void verifyCode(String email, String code) {
        if (code == null || code.trim().isEmpty()) {
            throw new InvalidOtpException("El código de verificación es obligatorio.");
        }
        String normalizedEmail = normalizeEmail(email);
        Instant now = clock.instant();

        OtpEntry entry = otpStore.get(normalizedEmail);
        if (entry == null || now.isAfter(entry.expiresAt())) {
            otpStore.remove(normalizedEmail);
            throw new InvalidOtpException("El código de verificación ha expirado o no existe.");
        }

        if (entry.attempts() >= MAX_ATTEMPTS) {
            otpStore.remove(normalizedEmail);
            throw new InvalidOtpException("Número máximo de intentos excedido. Solicita un nuevo código.");
        }

        String inputHash = sha256(code.trim());
        if (!MessageDigest.isEqual(entry.codeHash().getBytes(StandardCharsets.UTF_8), inputHash.getBytes(StandardCharsets.UTF_8))) {
            otpStore.put(normalizedEmail, new OtpEntry(entry.codeHash(), entry.expiresAt(), entry.attempts() + 1, entry.createdAt()));
            throw new InvalidOtpException("Código de verificación incorrecto.");
        }

        // Código validado exitosamente -> se elimina para evitar reutilización
        otpStore.remove(normalizedEmail);
    }

    public void clear() {
        otpStore.clear();
    }

    private String normalizeEmail(String email) {
        return email != null ? email.trim().toLowerCase() : "";
    }

    private String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Algoritmo SHA-256 no disponible", e);
        }
    }
}
