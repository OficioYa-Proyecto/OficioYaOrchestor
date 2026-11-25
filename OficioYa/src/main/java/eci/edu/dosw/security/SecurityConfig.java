package eci.edu.dosw.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfigurationSource;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http, JwtService jwtService,
            RestAuthenticationEntryPoint entryPoint,
            RestAccessDeniedHandler deniedHandler,
            CorsConfigurationSource corsConfigurationSource) throws Exception {
        http.cors(cors -> cors.configurationSource(corsConfigurationSource))
            .csrf(csrf -> csrf.disable())
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .httpBasic(b -> b.disable())
            .formLogin(f -> f.disable())
            .exceptionHandling(e -> e
                .authenticationEntryPoint(entryPoint)
                .accessDeniedHandler(deniedHandler))
            .authorizeHttpRequests(a -> a
                .requestMatchers(HttpMethod.POST,
                    "/api/v1/auth/login", "/api/v1/auth/register", "/api/v1/auth/register/**",
                    "/api/v1/auth/forgot", "/api/v1/auth/reset").permitAll()
                .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html", "/swagger-resources/**").permitAll()
                .requestMatchers("/error").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/v1/images/by-root/**").authenticated()
                .requestMatchers(HttpMethod.GET, "/api/v1/images/{id}", "/api/v1/images/{id}/metadata").permitAll()
                .requestMatchers("/api/v1/images/**").authenticated()
                .requestMatchers(HttpMethod.GET, "/api/v1/localities").permitAll()
                .requestMatchers("/api/v1/localities/**").authenticated()
                .requestMatchers(HttpMethod.GET, "/api/v1/zones/**", "/api/v1/trades/**", "/api/v1/trade-categories/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/v1/workers/me", "/api/v1/workers/me/**").authenticated()
                .requestMatchers(HttpMethod.GET, "/api/v1/workers/**", "/api/workers/**").permitAll()
                .requestMatchers("/api/v1/workers/**", "/api/workers/**", "/api/v1/trades/**", "/api/v1/trade-categories/**").authenticated()
                .requestMatchers(HttpMethod.GET, "/api/v1/search/**").permitAll()
                .requestMatchers("/api/v1/search/**").authenticated()
                .requestMatchers(HttpMethod.POST, "/api/v1/reputation/eventos/**").permitAll()
                .requestMatchers("/api/v1/reputation/**").authenticated()
                .requestMatchers("/api/v1/reporting/**").authenticated()
                .requestMatchers("/api/v1/requests/**", "/api/v1/services/**", "/api/v1/service/**").authenticated()
                .requestMatchers(HttpMethod.POST, "/api/v1/notification", "/api/v1/notification/review-reported", "/api/v1/notification/email").permitAll()
                .requestMatchers("/api/v1/notification/**", "/api/v1/notifications/**").authenticated()
                .requestMatchers("/api/v1/admin/**", "/api/admin/**").hasRole("ADMIN")
                .requestMatchers("/api/v1/worker/**", "/api/worker/**").hasRole("TRABAJADOR")
                .requestMatchers("/api/v1/users/**", "/api/v1/zones/**").authenticated()
                .anyRequest().authenticated())
            .addFilterBefore(new JwtAuthenticationFilter(jwtService),
                UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

}