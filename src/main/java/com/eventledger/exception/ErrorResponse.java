package com.eventledger.exception;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.time.Instant;

/**
 * Standardised error response body returned by the API for all error conditions.
 *
 * <p>Every error response carries a UTC timestamp, the HTTP status code,
 * a short reason phrase, a human-readable message, and the request path that
 * triggered the error — providing clients with enough context for diagnostics.
 *
 * @author Sarathkumar Ravi
 */
@Schema(description = "Standardised error response returned for all error conditions")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ErrorResponse {

    @Schema(description = "UTC timestamp at which the error was generated", example = "2026-05-15T14:02:11Z")
    private Instant timestamp;

    @Schema(description = "HTTP status code", example = "404")
    private int status;

    @Schema(description = "Short HTTP reason phrase", example = "Not Found")
    private String error;

    @Schema(description = "Human-readable description of what went wrong", example = "Event not found: evt-999")
    private String message;

    @Schema(description = "Request URI that produced this error", example = "/events/evt-999")
    private String path;
}
