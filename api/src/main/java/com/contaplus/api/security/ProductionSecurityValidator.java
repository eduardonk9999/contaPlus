package com.contaplus.api.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@Component
public class ProductionSecurityValidator {

    private static final Logger log = LoggerFactory.getLogger(ProductionSecurityValidator.class);

    private static final String DEFAULT_SECRET = "chave-secreta-contaplus-desenvolvimento-min-256-bits-ok";
    private static final int MIN_SECRET_LENGTH = 32;

    private final Environment environment;

    @Value("${jwt.secret}")
    private String jwtSecret;

    public ProductionSecurityValidator(Environment environment) {
        this.environment = environment;
    }

    @EventListener(ContextRefreshedEvent.class)
    public void validateSecurityConfiguration() {
        boolean isProduction = Arrays.asList(environment.getActiveProfiles()).contains("prod");

        if (isProduction) {
            validateProductionJwtSecret();
            validateProductionGoogleOAuth();
        } else {
            warnDevelopmentConfiguration();
        }
    }

    private void validateProductionJwtSecret() {
        if (jwtSecret == null || jwtSecret.isBlank()) {
            throw new SecurityConfigurationException(
                "JWT_SECRET é obrigatório em produção. Configure a variável de ambiente JWT_SECRET."
            );
        }

        if (jwtSecret.equals(DEFAULT_SECRET)) {
            throw new SecurityConfigurationException(
                "JWT_SECRET não pode usar o valor padrão de desenvolvimento em produção. " +
                "Configure uma chave segura via variável de ambiente JWT_SECRET."
            );
        }

        if (jwtSecret.length() < MIN_SECRET_LENGTH) {
            throw new SecurityConfigurationException(
                "JWT_SECRET deve ter no mínimo " + MIN_SECRET_LENGTH + " caracteres em produção. " +
                "Atual: " + jwtSecret.length() + " caracteres."
            );
        }

        log.info("Configuração JWT validada para produção");
    }

    private void validateProductionGoogleOAuth() {
        String googleClientId = environment.getProperty("spring.security.oauth2.client.registration.google.client-id", "");
        String googleClientSecret = environment.getProperty("spring.security.oauth2.client.registration.google.client-secret", "");

        if (googleClientId.isBlank() || googleClientSecret.isBlank()) {
            log.warn("Google OAuth não configurado. Login com Google estará desabilitado.");
        } else {
            log.info("Google OAuth configurado para produção");
        }
    }

    private void warnDevelopmentConfiguration() {
        if (jwtSecret.equals(DEFAULT_SECRET)) {
            log.warn("⚠️  Usando JWT_SECRET padrão de desenvolvimento. NÃO use em produção!");
        }

        if (jwtSecret.length() < MIN_SECRET_LENGTH) {
            log.warn("⚠️  JWT_SECRET com menos de {} caracteres. Recomendado aumentar para produção.", MIN_SECRET_LENGTH);
        }
    }

    public static class SecurityConfigurationException extends RuntimeException {
        public SecurityConfigurationException(String message) {
            super(message);
        }
    }
}
