package com.contaplus.api.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Value("${server.port:8081}")
    private int serverPort;

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
            .info(apiInfo())
            .servers(List.of(
                new Server()
                    .url("http://localhost:" + serverPort)
                    .description("Servidor Local")
            ))
            .components(new Components()
                .addSecuritySchemes("bearer-jwt", new SecurityScheme()
                    .type(SecurityScheme.Type.HTTP)
                    .scheme("bearer")
                    .bearerFormat("JWT")
                    .description("Token JWT obtido via /v1/auth/login ou /v1/auth/google")
                )
            )
            .addSecurityItem(new SecurityRequirement().addList("bearer-jwt"));
    }

    private Info apiInfo() {
        return new Info()
            .title("ContaPlus API")
            .description("""
                API REST para gestão de vendas, estoque e finanças de pequenos comércios.

                ## Autenticação

                A maioria dos endpoints requer autenticação JWT. Obtenha um token via:
                - `POST /v1/auth/register` - Criar conta
                - `POST /v1/auth/login` - Login com email/senha
                - `POST /v1/auth/google` - Login com Google

                Use o token no header: `Authorization: Bearer <token>`

                ## Valores Monetários

                Todos os valores monetários são expressos em **centavos** (Integer).
                Exemplo: R$ 10,50 = 1050 centavos

                ## Rate Limiting

                - 60 requisições/minuto para endpoints autenticados
                - 10 requisições/minuto para endpoints de autenticação
                """)
            .version("1.0.0")
            .contact(new Contact()
                .name("ContaPlus")
                .email("contato@contaplus.com.br")
            )
            .license(new License()
                .name("Proprietário")
            );
    }
}
