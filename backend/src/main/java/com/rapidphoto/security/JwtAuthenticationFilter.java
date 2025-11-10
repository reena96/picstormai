package com.rapidphoto.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.UUID;

/**
 * JWT Authentication Filter for Spring Security WebFlux.
 * Extracts JWT from Authorization header, validates it,
 * and sets authentication in SecurityContext.
 */
@Component
public class JwtAuthenticationFilter implements WebFilter {

    private static final String BEARER_PREFIX = "Bearer ";
    private final JwtUtil jwtUtil;

    public JwtAuthenticationFilter(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);

        // If no Authorization header or doesn't start with "Bearer ", continue without authentication
        if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
            return chain.filter(exchange);
        }

        // Extract token
        String token = authHeader.substring(BEARER_PREFIX.length());

        try {
            // Validate token and extract claims
            Claims claims = jwtUtil.validateToken(token);
            UUID userId = UUID.fromString(claims.getSubject());
            String email = claims.get("email", String.class);

            // Create authentication object
            Authentication authentication = new UsernamePasswordAuthenticationToken(
                userId, // Principal (user ID)
                null, // Credentials (not needed after authentication)
                List.of(new SimpleGrantedAuthority("ROLE_USER")) // Authorities
            );

            // Set authentication in SecurityContext and continue
            return chain.filter(exchange)
                .contextWrite(ReactiveSecurityContextHolder.withAuthentication(authentication));

        } catch (JwtException | IllegalArgumentException e) {
            // Invalid token - continue without authentication
            // (Spring Security will reject if endpoint requires authentication)
            return chain.filter(exchange);
        }
    }
}
