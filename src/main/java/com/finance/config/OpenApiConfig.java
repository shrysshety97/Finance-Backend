package com.finance.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * SpringDoc OpenAPI configuration.
 *
 * Swagger UI is available at:  http://localhost:8080/swagger-ui.html
 * Raw OpenAPI JSON spec:       http://localhost:8080/v3/api-docs
 *
 * To authenticate in Swagger UI:
 *   1. POST /api/auth/login  copy the accessToken from the response
 *   2. Click "Authorize"  paste:  Bearer <your_token>
 */
@Configuration
public class OpenApiConfig {

    private static final String SECURITY_SCHEME_NAME = "BearerAuth";

    @Bean
    public OpenAPI financeOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Finance Dashboard API")
                        .description("RESTful backend for the Finance Dashboard system.\n\n" +
                                "**Authentication:** All endpoints (except `/api/auth/**`) require a " +
                                "Bearer JWT token in the `Authorization` header.\n\n" +
                                "**Roles:**\n" +
                                "- `VIEWER`  â€” Dashboard summary & category totals\n" +
                                "- `ANALYST` â€” All of VIEWER + individual records + trends\n" +
                                "- `ADMIN`   â€” Full access including user management\n\n" +
                                "**Default test credentials:**\n" +
                                "| Username | Password   | Role    |\n" +
                                "|----------|------------|---------|\n" +
                                "| admin    | admin123   | ADMIN   |\n" +
                                "| analyst  | analyst123 | ANALYST |\n" +
                                "| viewer   | viewer123  | VIEWER  |")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Finance Backend")
                                .email("admin@finance.com"))
                        .license(new License()
                                .name("MIT")
                                .url("https://opensource.org/licenses/MIT")))
                // Global Bearer token security requirement
                .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME_NAME))
                .components(new Components()
                        .addSecuritySchemes(SECURITY_SCHEME_NAME,
                                new SecurityScheme()
                                        .name(SECURITY_SCHEME_NAME)
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("Paste your JWT token here (without 'Bearer ' prefix)")));
    }
}
