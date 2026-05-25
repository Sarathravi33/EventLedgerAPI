package com.eventledger.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.servers.Server;
import org.springframework.context.annotation.Configuration;

/**
 * Registers the global OpenAPI 3.0 metadata that appears in the Swagger UI
 * header — title, version, description, contact, and server list.
 *
 * <p>Endpoint: {@code GET /swagger-ui.html} for the interactive UI,
 * {@code GET /api-docs} for the raw JSON spec.
 *
 * @author Sarathkumar Ravi
 */
@OpenAPIDefinition(
        info = @Info(
                title = "Event Ledger API",
                version = "1.0.0",
                description = "Idempotent financial transaction event store. "
                        + "Submit CREDIT/DEBIT events, query history with cursor-based pagination, "
                        + "and compute account balances.",
                contact = @Contact(name = "Sarathkumar Ravi", email = "sarathshadow@gmail.com")
        ),
        servers = @Server(url = "http://localhost:8081", description = "Local development server")
)
@Configuration
public class OpenApiConfig {}
