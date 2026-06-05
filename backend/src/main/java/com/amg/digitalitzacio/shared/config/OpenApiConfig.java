package com.amg.digitalitzacio.shared.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Value("${app.cors.allowed-origins:http://localhost:3000}")
    private String allowedOrigins;

    @Bean
    public OpenAPI amgOpenAPI() {
        var serverUrl = allowedOrigins.contains("amgdl.com")
            ? "https://api.amgdl.com"
            : "http://localhost:8080";

        return new OpenAPI()
            .info(new Info()
                .title("AMG Digitalització API")
                .description("API REST de la plataforma SaaS multi-tenant per a negocis locals. " +
                    "Mòduls: Auth, Vault, Engine, Factory, Billing, FinOps, Payments, Agents, Leads, etc.")
                .version("0.1.0")
                .contact(new Contact()
                    .name("AMG Digitalització")
                    .email("info@amg.digital")
                    .url("https://amgdl.com"))
                .license(new License()
                    .name("Propietari — AMG Digitalització")
                    .url("https://amgdl.com/legal/avis-legal")))
            .servers(List.of(
                new Server().url(serverUrl).description("Servidor actiu"),
                new Server().url("http://localhost:8080").description("Desenvolupament local")));
    }
}
