package eci.edu.dosw.orchestrator.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Configuración OpenAPI / Swagger para el Orquestador y API Gateway de OficioYa.
 * Define metadatos, servidores disponibles y el esquema de seguridad Bearer JWT.
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        final String securitySchemeName = "bearerAuth";
        return new OpenAPI()
                .info(new Info()
                        .title("OficioYa - API Gateway & Saga Orchestrator")
                        .version("1.0.0")
                        .description("""
                                API Gateway y Orquestador de Sagas para la plataforma OficioYa.
                                
                                Provee los endpoints de:
                                - **Autenticación y Registro:** Login, refresh de sesión, envío de código OTP y registro transaccional.
                                - **Gestión de Credenciales:** Cambio de contraseña y recuperación por OTP.
                                - **Reverse Proxy Gateway:** Enrutamiento transparente hacia los microservicios de dominio (Usuarios, Workers, Imágenes, Servicios, Búsqueda, etc.).
                                """)
                        .contact(new Contact()
                                .name("Equipo OficioYa - Escuela Colombiana de Ingeniería")
                                .email("soporte@oficioya.com"))
                        .license(new License().name("MIT").url("https://opensource.org/licenses/MIT")))
                .servers(List.of(
                        new Server().url("https://oficioya-orchestor-api-ghaxfaacfabxh4bv.chilecentral-01.azurewebsites.net").description("Entorno de Producción en Azure"),
                        new Server().url("http://localhost:8081").description("Entorno de Desarrollo Local")
                ))
                .addSecurityItem(new SecurityRequirement().addList(securitySchemeName))
                .components(new Components()
                        .addSecuritySchemes(securitySchemeName,
                                new SecurityScheme()
                                        .name(securitySchemeName)
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("Ingrese su token JWT (sin el prefijo 'Bearer ')")));
    }
}
