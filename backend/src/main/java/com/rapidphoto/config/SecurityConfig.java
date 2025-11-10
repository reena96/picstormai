package com.rapidphoto.config;

import com.rapidphoto.security.JwtAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableReactiveMethodSecurity;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;

/**
 * Spring Security configuration for JWT authentication.
 * Configures public endpoints and JWT filter for protected endpoints.
 */
@Configuration
@EnableWebFluxSecurity
@EnableReactiveMethodSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        return http
            // Disable CSRF (using JWT, stateless authentication)
            .csrf(ServerHttpSecurity.CsrfSpec::disable)

            // Configure authorization rules
            .authorizeExchange(exchanges -> exchanges
                // Public endpoints (no authentication required)
                .pathMatchers("/api/auth/login").permitAll()
                .pathMatchers("/api/auth/refresh").permitAll()
                .pathMatchers("/api/auth/register").permitAll()
                .pathMatchers("/actuator/health").permitAll()

                // All other endpoints require authentication
                .anyExchange().authenticated()
            )

            // Add JWT authentication filter
            .addFilterAt(jwtAuthenticationFilter, SecurityWebFiltersOrder.AUTHENTICATION)

            // Build security filter chain
            .build();
    }
}
