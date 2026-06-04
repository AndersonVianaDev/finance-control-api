package com.andersonvianadev.finance_control_api.infra.config;

import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenAiConfig {

    @Bean
    public OpenAPI customOpenAi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Nix Finance API")
                        .description(
                                """
                                   NIX FINANCE API, an API for financial management
                                   personal.

                                   - User registration
                                   - Transaction registration (incoming/outgoing)
                                   - Investment registration
                                   - Financial report
                                   - AI support
                                   
                                   Project developed in Java 21 with Spring Boot
                                """
                        ).contact(new Contact()
                                .name("Anderson Palmerim Viana")
                                .email("anderson.viana.dev@gmail.com"))
                ).externalDocs(new ExternalDocumentation()
                        .description("Project Documentation")
                        .url("https://github.com/AndersonVianaDev/finance-control-api")
                );
    }

}
