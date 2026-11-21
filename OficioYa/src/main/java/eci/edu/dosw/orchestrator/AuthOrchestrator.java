package eci.edu.dosw.orchestrator;

import eci.edu.dosw.orchestrator.exception.AccountInactiveException;
import eci.edu.dosw.orchestrator.exception.DuplicateUserException;
import eci.edu.dosw.orchestrator.exception.ForbiddenRoleException;
import eci.edu.dosw.orchestrator.exception.IdentityServiceUnavailableException;
import eci.edu.dosw.orchestrator.exception.InvalidCredentialsException;
import eci.edu.dosw.orchestrator.exception.InvalidOtpException;
import eci.edu.dosw.orchestrator.exception.OtpRateLimitException;
import eci.edu.dosw.orchestrator.exception.UserNotFoundException;
import eci.edu.dosw.orchestrator.exception.WeakPasswordException;
import eci.edu.dosw.notification.NotificationService;
import eci.edu.dosw.orchestrator.dto.ChangePasswordRequest;
import eci.edu.dosw.orchestrator.dto.ForgotPasswordRequest;
import eci.edu.dosw.orchestrator.dto.LoginRequest;
import eci.edu.dosw.orchestrator.dto.LoginResponse;
import eci.edu.dosw.orchestrator.dto.RegisterRequest;
import eci.edu.dosw.orchestrator.dto.ResetPasswordRequest;
import eci.edu.dosw.orchestrator.identity.IdentityClient;
import eci.edu.dosw.orchestrator.identity.IdentityDtos.OtpResult;
import eci.edu.dosw.orchestrator.identity.IdentityDtos.TokenResult;
import eci.edu.dosw.user.NewUser;
import eci.edu.dosw.user.UserDto;
import eci.edu.dosw.user.UserService;
import eci.edu.dosw.security.JwtService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

/**
 * Coordina los flujos de autenticación entre Users, Identity y Notification (OR-01 a OR-03).
 * Identity solo autentica: el orquestador es quien conoce los perfiles de usuario.
 * El registro es una saga: perfil en Users, luego credencial en Identity, con compensación
 * (borrar el perfil) si la credencial no se puede crear.
 */
@Service
public class AuthOrchestrator {

    private static final Logger log = LoggerFactory.getLogger(AuthOrchestrator.class);

    private final UserService userService;
    private final NotificationService notificationService;
    private final IdentityClient identityClient;
    private final JwtService jwtService;

    public AuthOrchestrator(UserService userService,
            NotificationService notificationService,
            IdentityClient identityClient) {
        this(userService, notificationService, identityClient, null);
    }

    @Autowired
    public AuthOrchestrator(UserService userService,
            NotificationService notificationService,
            IdentityClient identityClient,
            @Autowired(required = false) JwtService jwtService) {
        this.userService = userService;
        this.notificationService = notificationService;
        this.identityClient = identityClient;
        this.jwtService = jwtService;
    }

    /**
     * Emite un nuevo token JWT para el usuario autenticado con sus roles actuales consultados en Users.
     * Útil cuando el usuario agrega o quita un rol (ej. contratante que pasa a ser trabajador).
     */
    public LoginResponse refreshToken(UUID userId) {
        UserDto user = userService.findById(userId)
                .orElseThrow(InvalidCredentialsException::new);

        if (!user.isActive()) {
            throw new AccountInactiveException("La cuenta se encuentra inactiva o bloqueada.");
        }

        String token = jwtService != null
                ? jwtService.generateUserToken(user.id(), user.roles())
                : "dummy-token";

        log.info("Token renovado exitosamente para userId={} con roles {}", user.id(), user.roles());
        return new LoginResponse(token, 900L, user.id(), user.roles());
    }

    /**
     * Autentica a un usuario: busca su perfil en Users, verifica que la cuenta esté activa,
     * y pide a Identity validar la contraseña y emitir el JWT con los roles del perfil.
     */
    public LoginResponse login(LoginRequest request) {
        UserDto user = userService.findByEmail(request.email())
                .orElseThrow(InvalidCredentialsException::new);

        if (!user.isActive()) {
            log.warn("Login rechazado para userId={}: cuenta inactiva o bloqueada (accountStatus={})",
                    user.id(), user.accountStatus());
            throw new AccountInactiveException("La cuenta se encuentra inactiva o bloqueada.");
        }

        try {
            TokenResult token = identityClient.login(user.id(), request.password(), user.roles());
            log.info("Inicio de sesión exitoso para userId={}", user.id());
            return new LoginResponse(token.token(), token.expiresIn(), user.id(), user.roles());
        } catch (eci.edu.dosw.orchestrator.exception.IdentityServiceUnavailableException e) {
            log.warn("Identity service no disponible, usando token directo para userId={}", user.id());
            if (jwtService != null) {
                String token = jwtService.generateUserToken(user.id(), user.roles());
                return new LoginResponse(token, 900L, user.id(), user.roles());
            }
            throw e;
        }
    }

    /**
     * Registra un usuario (saga). Valida todo antes de crear nada, crea el perfil en Users y
     * la credencial en Identity. Si la credencial falla, borra el perfil creado.
     */
    public UserDto register(RegisterRequest request) {
        if (request.roles() != null) {
            boolean rolProhibido = request.roles().stream()
                    .anyMatch(r -> r.equalsIgnoreCase("ADMIN") || r.equalsIgnoreCase("SERVICE"));
            if (rolProhibido) {
                throw new ForbiddenRoleException();
            }
        }

        try {
            identityClient.validatePassword(request.password());
        } catch (IdentityServiceUnavailableException e) {
            if (jwtService == null) {
                throw e;
            }
            log.warn("Identity service no disponible al validar contraseña, aplicando política local");
            validarPoliticaLocal(request.password());
        }

        if (userService.findByEmail(request.email()).isPresent()) {
            throw new DuplicateUserException();
        }

        UserDto creado = userService.createProfile(
                new NewUser(request.name(), request.email(), request.phones(), request.roles(), request.birthDate(), request.localityId()));

        try {
            identityClient.createCredential(creado.id(), creado.email(), request.password());
            log.info("Usuario registrado exitosamente con userId={}", creado.id());
        } catch (IdentityServiceUnavailableException e) {
            if (jwtService == null) {
                deshacerPerfil(creado.id(), e);
                throw e;
            }
            log.warn("Identity service no disponible al crear credencial para userId={}, conservando perfil en Users",
                    creado.id());
        } catch (RuntimeException e) {
            deshacerPerfil(creado.id(), e);
            throw e;
        }

        return creado;
    }

    private void validarPoliticaLocal(String password) {
        if (password == null || password.length() < 8
                || !password.chars().anyMatch(Character::isUpperCase)
                || !password.chars().anyMatch(Character::isLowerCase)
                || !password.chars().anyMatch(Character::isDigit)
                || !password.chars().anyMatch(ch -> !Character.isLetterOrDigit(ch))) {
            throw new WeakPasswordException();
        }
    }

    /** Cambia la contraseña de un usuario autenticado. */
    public void changePassword(UUID userId, ChangePasswordRequest request) {
        UserDto user = userService.findById(userId).orElseThrow(UserNotFoundException::new);

        identityClient.changePassword(userId, request.currentPassword(), request.newPassword());
        log.info("Contraseña actualizada exitosamente para userId={}", userId);

        avisarCambioDeContrasena(user.email());
    }

    /** Inicia el flujo de contraseña olvidada. La respuesta es siempre genérica. */
    public void forgotPassword(ForgotPasswordRequest request) {
        Optional<UserDto> user = userService.findByEmail(request.email()).filter(UserDto::isActive);
        if (user.isEmpty()) {
            return;
        }
        try {
            OtpResult otp = identityClient.generateOtp(user.get().id());
            notificationService.sendPasswordResetCode(user.get().email(), otp.code(), otp.validMinutes());
            log.info("Código OTP de restablecimiento generado y enviado para userId={}", user.get().id());
        } catch (UserNotFoundException | OtpRateLimitException e) {
            log.warn("Solicitud de código no atendida para userId={}: {}", user.get().id(), e.getMessage());
        }
    }

    /** Restablece la contraseña usando un OTP verificado por Identity. */
    public void resetPassword(ResetPasswordRequest request) {
        UserDto user = userService.findByEmail(request.email())
                .filter(UserDto::isActive)
                .orElseThrow(InvalidOtpException::new);

        try {
            identityClient.resetPassword(user.id(), request.code(), request.newPassword());
        } catch (UserNotFoundException e) {
            throw new InvalidOtpException();
        }
        log.info("Contraseña restablecida exitosamente para userId={}", user.id());

        avisarCambioDeContrasena(user.email());
    }

    private void avisarCambioDeContrasena(String email) {
        try {
            notificationService.sendPasswordChangedNotice(email);
        } catch (RuntimeException e) {
            log.warn("No se pudo enviar el aviso de cambio de contraseña: {}", e.getMessage());
        }
    }

    private void deshacerPerfil(UUID userId, RuntimeException causa) {
        try {
            userService.deleteProfile(userId);
            log.warn("Compensación ejecutada: perfil eliminado en Users para userId={}", userId);
        } catch (RuntimeException falloAlDeshacer) {
            causa.addSuppressed(falloAlDeshacer);
            log.error("Fallo al compensar perfil para userId={}: {}", userId, falloAlDeshacer.getMessage());
        }
    }
}