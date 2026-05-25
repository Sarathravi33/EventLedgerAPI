package com.eventledger;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entry point for the Event Ledger API application.
 *
 * <p>Bootstraps the Spring Boot context and starts the embedded web server.
 * The application exposes REST endpoints for submitting and querying
 * idempotent financial transaction events.
 *
 * @author Sarathkumar Ravi
 */
@SpringBootApplication
public class EventLedgerApplication {

    /**
     * Application entry point.
     *
     * @param args command-line arguments passed to the Spring application context
     */
    public static void main(String[] args) {
        SpringApplication.run(EventLedgerApplication.class, args);
    }
}
