package com.contaplus.api.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class SecurityHeadersFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        // Previne MIME type sniffing
        response.setHeader("X-Content-Type-Options", "nosniff");

        // Previne clickjacking
        response.setHeader("X-Frame-Options", "DENY");

        // Proteção XSS (legacy, mas ainda útil)
        response.setHeader("X-XSS-Protection", "1; mode=block");

        // Força HTTPS (HSTS) - 1 ano
        response.setHeader("Strict-Transport-Security", "max-age=31536000; includeSubDomains");

        // Controla referrer
        response.setHeader("Referrer-Policy", "strict-origin-when-cross-origin");

        // Content Security Policy para APIs
        response.setHeader("Content-Security-Policy", "default-src 'none'; frame-ancestors 'none'");

        // Previne cache de dados sensíveis
        response.setHeader("Cache-Control", "no-store, no-cache, must-revalidate, proxy-revalidate");
        response.setHeader("Pragma", "no-cache");
        response.setHeader("Expires", "0");

        // Permissions Policy (substitui Feature-Policy)
        response.setHeader("Permissions-Policy", "geolocation=(), microphone=(), camera=()");

        filterChain.doFilter(request, response);
    }
}
