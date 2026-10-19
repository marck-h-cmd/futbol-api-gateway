package com.liga.equipos.config;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;

@Component
@Order(1)
public class RateLimitFilter extends OncePerRequestFilter {

    // Capacidad de 2 tokens que se reponen cada 1 minuto (Caso A: límite de 2 solicitudes)
    private final Bucket bucket = Bucket.builder()
            .addLimit(Bandwidth.builder()
                    .capacity(2)
                    .refillIntervally(2, Duration.ofMinutes(1))
                    .build())
            .build();

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String path = request.getRequestURI();

        // Aplicar límite de 2 solicitudes a los endpoints de la API de equipos
        if (path.startsWith("/api/equipos")) {
            if (bucket.tryConsume(1)) {
                filterChain.doFilter(request, response);
            } else {
                response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value()); // HTTP 429 Too Many Requests
                response.setContentType("application/json");
                response.setCharacterEncoding("UTF-8");
                response.getWriter().write("{\"status\": 429, \"error\": \"Too Many Requests\", \"message\": \"Límite de 2 solicitudes excedido para el servicio de equipos. Intente de nuevo más tarde.\"}");
            }
        } else {
            filterChain.doFilter(request, response);
        }
    }
}
