package com.contaplus.api.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
public class CorsConfig {

    @Value("${cors.allowed-origins:http://localhost:3000,http://localhost:5173}")
    private String allowedOrigins;

    @Value("${cors.allowed-methods:GET,POST,PUT,DELETE,PATCH,OPTIONS}")
    private String allowedMethods;

    @Value("${cors.allowed-headers:Authorization,Content-Type,X-Requested-With,Accept,Origin,X-Idempotency-Key}")
    private String allowedHeaders;

    @Value("${cors.exposed-headers:X-Total-Count,X-Page-Size,X-Current-Page}")
    private String exposedHeaders;

    @Value("${cors.allow-credentials:true}")
    private boolean allowCredentials;

    @Value("${cors.max-age:3600}")
    private long maxAge;

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // Origins permitidas (configuráveis via env)
        configuration.setAllowedOrigins(parseList(allowedOrigins));

        // Métodos HTTP permitidos
        configuration.setAllowedMethods(parseList(allowedMethods));

        // Headers permitidos
        configuration.setAllowedHeaders(parseList(allowedHeaders));

        // Headers expostos ao cliente
        configuration.setExposedHeaders(parseList(exposedHeaders));

        // Permitir credentials (cookies, Authorization header)
        configuration.setAllowCredentials(allowCredentials);

        // Cache de preflight (segundos)
        configuration.setMaxAge(maxAge);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);

        return source;
    }

    private List<String> parseList(String value) {
        return Arrays.asList(value.split(","));
    }
}
