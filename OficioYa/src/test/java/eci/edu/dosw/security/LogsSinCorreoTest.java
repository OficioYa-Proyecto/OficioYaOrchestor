package eci.edu.dosw.security;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import eci.edu.dosw.notification.HttpNotificationService;
import eci.edu.dosw.notification.NotificationService;
import eci.edu.dosw.orchestrator.AuthOrchestrator;
import eci.edu.dosw.orchestrator.exception.UsersServiceUnavailableException;
import eci.edu.dosw.orchestrator.identity.IdentityClient;
import eci.edu.dosw.user.HttpUserService;
import eci.edu.dosw.user.UserService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/** Los logs del orquestador no deben contener correos completos (dato personal). */
class LogsSinCorreoTest {

    private static final String CORREO = "ana@x.com";

    private ListAppender<ILoggingEvent> appender;
    private final List<Logger> loggers = new ArrayList<>();

    @BeforeEach
    void capturarLogs() {
        appender = new ListAppender<>();
        appender.start();
        for (Class<?> tipo : List.of(HttpNotificationService.class, HttpUserService.class,
                AuthOrchestrator.class)) {
            Logger logger = (Logger) LoggerFactory.getLogger(tipo);
            logger.addAppender(appender);
            loggers.add(logger);
        }
    }

    @AfterEach
    void soltarLogs() {
        loggers.forEach(logger -> logger.detachAppender(appender));
    }

    private String todoElLog() {
        return appender.list.stream().map(ILoggingEvent::getFormattedMessage)
                .collect(Collectors.joining("\n"));
    }

    // --- el ayudante que oculta el correo ---
    @Test
    void enmascaraUnCorreoNormal() {
        assertEquals("a***@x.com", LogMask.email(CORREO));
    }

    @Test
    void correoNuloOVacioDevuelveUnTextoNeutro() {
        assertEquals("(sin correo)", LogMask.email(null));
        assertEquals("(sin correo)", LogMask.email("  "));
    }

    @Test
    void textoSinArrobaSeOcultaPorCompleto() {
        assertEquals("***", LogMask.email("sinarroba"));
    }

    // --- los tres sitios que hoy escriben el correo ---
    @Test
    void enviarCorreoExitosoNoEscribeElCorreoEnElLog() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://localhost:8088");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("http://localhost:8088/api/v1/notification/email"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess());

        new HttpNotificationService(builder.build(), null).sendPasswordChangedNotice(CORREO);

        assertFalse(appender.list.isEmpty());
        assertFalse(todoElLog().contains(CORREO));
    }

    @Test
    void enviarCorreoFallidoNoEscribeElCorreoEnElLog() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://localhost:8088");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("http://localhost:8088/api/v1/notification/email"))
                .andRespond(withServerError());

        new HttpNotificationService(builder.build(), null).sendPasswordChangedNotice(CORREO);

        assertFalse(appender.list.isEmpty());
        assertFalse(todoElLog().contains(CORREO));
    }

    @Test
    void consultarUsuarioPorCorreoFallidoNoEscribeElCorreoEnElLog() {
        com.sun.net.httpserver.HttpServer ignorar = null; // sin uso: solo se usa el servidor simulado
        RestClient.Builder builder = RestClient.builder().baseUrl("http://localhost:8080");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo(containsString("/api/v1/users/by-email")))
                .andRespond(withServerError());
        JwtService jwt = mock(JwtService.class);
        when(jwt.generateServiceToken()).thenReturn("token-de-prueba");

        HttpUserService servicio = new HttpUserService(builder.build(), jwt);

        assertThrows(UsersServiceUnavailableException.class, () -> servicio.findByEmail(CORREO));
        assertFalse(appender.list.isEmpty());
        assertFalse(todoElLog().contains(CORREO));
    }

    @Test
    void enviarCodigoDeRegistroNoEscribeElCorreoEnElLog() {
        UserService users = mock(UserService.class);
        when(users.findByEmail(CORREO)).thenReturn(Optional.empty());
        AuthOrchestrator orquestador = new AuthOrchestrator(users,
                mock(NotificationService.class), mock(IdentityClient.class));

        orquestador.sendRegistrationCode(CORREO);

        assertFalse(appender.list.isEmpty());
        assertFalse(todoElLog().contains(CORREO));
    }
}
