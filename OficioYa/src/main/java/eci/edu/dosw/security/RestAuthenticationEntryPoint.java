package eci.edu.dosw.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private static final Logger log = LoggerFactory.getLogger(RestAuthenticationEntryPoint.class);

    @Override
    public void commence(HttpServletRequest req, HttpServletResponse res, AuthenticationException ex)
            throws IOException {
        // Nunca se registra el token ni la cabecera Authorization.
        log.warn("Acceso no autenticado (401): {} {}", req.getMethod(), req.getRequestURI());
        res.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        res.setHeader("WWW-Authenticate", "Bearer");
        res.setContentType("application/json;charset=UTF-8");
        res.getWriter().write(
                "{\"status\":401,\"error\":\"Unauthorized\",\"message\":\"Token ausente, invalido o vencido.\"}");
    }
}
