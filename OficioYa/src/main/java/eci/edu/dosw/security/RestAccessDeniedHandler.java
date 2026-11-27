package eci.edu.dosw.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class RestAccessDeniedHandler implements AccessDeniedHandler {

    private static final Logger log = LoggerFactory.getLogger(RestAccessDeniedHandler.class);

    @Override
    public void handle(HttpServletRequest req, HttpServletResponse res, AccessDeniedException ex)
            throws IOException {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String userId = auth != null ? String.valueOf(auth.getName()) : "desconocido";
        log.warn("Acceso denegado (403): {} {} userId={}", req.getMethod(), req.getRequestURI(), userId);
        res.setStatus(HttpServletResponse.SC_FORBIDDEN);
        res.setContentType("application/json;charset=UTF-8");
        res.getWriter().write(
                "{\"status\":403,\"error\":\"Forbidden\",\"message\":\"No tienes permisos para esta accion.\"}");
    }
}
