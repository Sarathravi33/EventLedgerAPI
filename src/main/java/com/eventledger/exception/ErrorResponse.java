package com.eventledger.exception;

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
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ErrorResponse {

    /** UTC timestamp at which the error was generated. */
    private Instant timestamp;

    /** HTTP status code (e.g. {@code 400}, {@code 404}, {@code 500}). */
    private int status;

    /** Short HTTP reason phrase corresponding to the status code (e.g. {@code "Bad Request"}). */
    private String error;

    /** Human-readable description of what went wrong. */
    private String message;

    /** The request URI that produced this error. */
    private String path;
}
