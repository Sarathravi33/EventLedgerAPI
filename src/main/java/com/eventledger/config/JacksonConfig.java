package com.eventledger.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Jackson {@link ObjectMapper} configuration for the application.
 *
 * <p>Registers the {@link JavaTimeModule} so that Java 8 date/time types
 * (e.g. {@link java.time.Instant}) are serialised as ISO-8601 strings
 * instead of numeric timestamps.
 *
 * @author Sarathkumar Ravi
 */
@Configuration
public class JacksonConfig {

    /**
     * Produces a globally shared {@link ObjectMapper} bean with Java-time
     * support and human-readable date serialisation.
     *
     * @return configured {@link ObjectMapper} instance
     */
    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }
}
