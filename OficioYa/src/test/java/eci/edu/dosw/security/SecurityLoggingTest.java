package eci.edu.dosw.security;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import eci.edu.dosw.orchestrator.AuthOrchestrator;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Los rechazos 401/403 deben quedar en el log (WARN) sin escribir nunca el
 * token.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SecurityLoggingTest {

    private static final UUID USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000042");
    private static final String TOKEN_FALSO = "token.falso.abc123";

    @Autowired
    private MockMvc mvc;
    @Value("${jwt.secret}")
    private String secret;
    @MockitoBean
    private AuthOrchestrator authOrchestrator;

    private ListAppender<ILoggingEvent> appender;
    private Logger entryPointLogger;
    private Logger deniedLogger;

    @BeforeEach
    void capturarLogs() {
        appender = new ListAppender<>();
        appender.start();
        entryPointLogger = (Logger) LoggerFactory.getLogger(RestAuthenticationEntryPoint.class);
        deniedLogger = (Logger) LoggerFactory.getLogger(RestAccessDeniedHandler.class);
        entryPointLogger.addAppender(appender);
        deniedLogger.addAppender(appender);
    }

    @AfterEach
    void soltarLogs() {
        entryPointLogger.detachAppender(appender);
        deniedLogger.detachAppender(appender);
    }

    private List<ILoggingEvent> advertencias() {
        return appender.list.stream().filter(e -> e.getLevel() == Level.WARN).toList();
    }

    private String tokenValido(String... roles) {
        SecretKey key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
        Instant ahora = Instant.now();
        return Jwts.builder()
                .subject(USER_ID.toString())
                .claim("roles", List.of(roles))
                .issuedAt(Date.from(ahora))
                .expiration(Date.from(ahora.plusMillis(900_000L)))
                .signWith(key)
                .compact();
    }

    @Test
    void sinTokenRegistraWarnConMetodoYRuta() throws Exception {
        mvc.perform(MockMvcRequestBuilders.get("/api/probe")).andExpect(status().isUnauthorized());

        assertEquals(1, advertencias().size());
        String mensaje = advertencias().get(0).getFormattedMessage();
        assertTrue(mensaje.contains("401"));
        assertTrue(mensaje.contains("GET /api/probe"));
    }

    @Test
    void tokenInvalidoRegistraWarnSinEscribirElToken() throws Exception {
        mvc.perform(MockMvcRequestBuilders.get("/api/probe")
                .header("Authorization", "Bearer " + TOKEN_FALSO))
                .andExpect(status().isUnauthorized());

        assertEquals(1, advertencias().size());
        assertFalse(advertencias().get(0).getFormattedMessage().contains(TOKEN_FALSO));
    }

    @Test
    void sinPermisoRegistraWarn403ConElIdDelUsuario() throws Exception {
        mvc.perform(MockMvcRequestBuilders.get("/api/v1/admin/probe")
                .header("Authorization", "Bearer " + tokenValido("CONTRATANTE")))
                .andExpect(status().isForbidden());

        assertEquals(1, advertencias().size());
        String mensaje = advertencias().get(0).getFormattedMessage();
        assertTrue(mensaje.contains("403"));
        assertTrue(mensaje.contains("/api/v1/admin/probe"));
        assertTrue(mensaje.contains(USER_ID.toString()));
    }
}
