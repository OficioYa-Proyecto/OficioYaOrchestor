package eci.edu.dosw.notification;

import eci.edu.dosw.security.JwtService;
import eci.edu.dosw.security.LogMask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

/**
 * Cliente HTTP hacia el microservicio de Notificaciones (oficioya-notification)
 * para envío de correos de recuperación (OTP) y aviso de cambio de contraseña.
 */
@Service
@Primary
@Profile("!test & !local")
public class HttpNotificationService implements NotificationService {

    private static final Logger log = LoggerFactory.getLogger(HttpNotificationService.class);

    public record EmailPayload(String to, String subject, String body, String type) {
    }

    private final RestClient restClient;
    private final JwtService jwtService;

    @Autowired
    public HttpNotificationService(
            @Value("${notification.service.url:https://oficioya-notificationapi-apffc2eygzgcbghp.brazilsouth-01.azurewebsites.net}") String notificationServiceUrl,
            @Autowired(required = false) JwtService jwtService) {
        this(RestClient.builder().baseUrl(notificationServiceUrl).build(), jwtService);
    }

    public HttpNotificationService(RestClient restClient, JwtService jwtService) {
        this.restClient = restClient;
        this.jwtService = jwtService;
    }

    @Override
    public void sendPasswordResetCode(String email, String code, int validMinutes) {
        String subject = "Código de recuperación de contraseña - OficioYa";
        String body = "Tu código de recuperación es: " + code + " (válido por " + validMinutes + " minutos).";
        sendEmail(new EmailPayload(email, subject, body, "RESET_CODE"));
    }

    @Override
    public void sendRegistrationVerificationCode(String email, String code, int validMinutes) {
        String subject = "Código de activación de cuenta - OficioYa";
        String body = "Tu código de activación para OficioYa es: " + code + " (válido por " + validMinutes + " minutos).";
        sendEmail(new EmailPayload(email, subject, body, "REGISTRATION_CODE"));
    }

    @Override
    public void sendPasswordChangedNotice(String email) {
        String subject = "Aviso de cambio de contraseña - OficioYa";
        String body = "Tu contraseña en OficioYa fue actualizada exitosamente.";
        sendEmail(new EmailPayload(email, subject, body, "PASSWORD_CHANGED"));
    }

    private void sendEmail(EmailPayload payload) {
        try {
            var spec = restClient.post()
                    .uri("/api/v1/notification/email")
                    .contentType(MediaType.APPLICATION_JSON);

            if (jwtService != null) {
                spec.header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtService.generateServiceToken());
            }

            spec.body(payload)
                    .retrieve()
                    .toBodilessEntity();

            log.info("Notificación por correo ({}) enviada a {}", payload.type(), LogMask.email(payload.to()));
        } catch (Exception e) {
            log.warn("No se pudo enviar correo ({}) a {} vía Notification service: {}",
                    payload.type(), LogMask.email(payload.to()), e.getMessage());
                    
        }
    }
}
