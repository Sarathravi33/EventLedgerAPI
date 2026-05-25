package com.eventledger.exception;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.Instant;
import java.util.stream.Collectors;

/**
 * Centralised exception handler for all REST controllers.
 *
 * <p>Intercepts exceptions thrown within {@code @RestController} methods and
 * translates them into a consistent {@link ErrorResponse} structure, removing
 * the need for try/catch blocks in individual controllers.
 *
 * <p>Handled cases:
 * <ul>
 *   <li>{@link MethodArgumentNotValidException} — Bean Validation failures → 400</li>
 *   <li>{@link HttpMessageNotReadableException} — Malformed JSON body → 400</li>
 *   <li>{@link MethodArgumentTypeMismatchException} — Wrong path/query param type → 400</li>
 *   <li>{@link EventNotFoundException} — Unknown event ID → 404</li>
 *   <li>{@link AccountNotFoundException} — Unknown account ID → 404</li>
 *   <li>{@link Exception} — All other unhandled exceptions → 500</li>
 * </ul>
 *
 * @author Sarathkumar Ravi
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Handles Bean Validation failures on request body fields.
     *
     * <p>All field-level error messages are joined into a single string
     * separated by {@code "; "} for easy client consumption.
     *
     * @param ex      the validation exception containing field-level error details
     * @param request the current HTTP request
     * @return {@code 400 Bad Request} with a descriptive message
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationErrors(
            MethodArgumentNotValidException ex, HttpServletRequest request) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("; "));
        return buildResponse(HttpStatus.BAD_REQUEST, message, request);
    }

    /**
     * Handles constraint violations on query/path parameters (e.g. negative page index,
     * zero page size) triggered by {@code @Validated} on the controller.
     *
     * @param ex      the exception containing one or more constraint violations
     * @param request the current HTTP request
     * @return {@code 400 Bad Request} with a message listing each violation
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolation(
            ConstraintViolationException ex, HttpServletRequest request) {
        String message = ex.getConstraintViolations().stream()
                .map(cv -> cv.getPropertyPath() + ": " + cv.getMessage())
                .collect(Collectors.joining("; "));
        return buildResponse(HttpStatus.BAD_REQUEST, message, request);
    }

    /**
     * Handles requests whose JSON body cannot be parsed (e.g. syntax errors,
     * unrecognised enum values).
     *
     * @param ex      the exception describing the parse failure
     * @param request the current HTTP request
     * @return {@code 400 Bad Request} with the root-cause message
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleUnreadable(
            HttpMessageNotReadableException ex, HttpServletRequest request) {
        String message = "Malformed request body: " + ex.getMostSpecificCause().getMessage();
        return buildResponse(HttpStatus.BAD_REQUEST, message, request);
    }

    /**
     * Handles type mismatches for path variables and query parameters
     * (e.g. a non-numeric value where a number is expected).
     *
     * @param ex      the exception containing the parameter name and invalid value
     * @param request the current HTTP request
     * @return {@code 400 Bad Request} with a descriptive message
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(
            MethodArgumentTypeMismatchException ex, HttpServletRequest request) {
        String message = "Invalid value '" + ex.getValue() + "' for parameter '" + ex.getName() + "'";
        return buildResponse(HttpStatus.BAD_REQUEST, message, request);
    }

    /**
     * Handles requests for a transaction event that does not exist.
     *
     * @param ex      the exception containing the unknown event ID
     * @param request the current HTTP request
     * @return {@code 404 Not Found}
     */
    @ExceptionHandler(EventNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleEventNotFound(
            EventNotFoundException ex, HttpServletRequest request) {
        return buildResponse(HttpStatus.NOT_FOUND, ex.getMessage(), request);
    }

    /**
     * Handles requests for an account that has no recorded events.
     *
     * @param ex      the exception containing the unknown account ID
     * @param request the current HTTP request
     * @return {@code 404 Not Found}
     */
    @ExceptionHandler(AccountNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleAccountNotFound(
            AccountNotFoundException ex, HttpServletRequest request) {
        return buildResponse(HttpStatus.NOT_FOUND, ex.getMessage(), request);
    }

    /**
     * Catch-all handler for any exception not covered by more specific handlers.
     *
     * <p>The full stack trace is logged at ERROR level for diagnostics while
     * a generic message is returned to the client to avoid leaking internals.
     *
     * @param ex      the unhandled exception
     * @param request the current HTTP request
     * @return {@code 500 Internal Server Error}
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneral(Exception ex, HttpServletRequest request) {
        log.error("Unexpected error", ex);
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred", request);
    }

    /**
     * Builds a standardised {@link ErrorResponse} and wraps it in a
     * {@link ResponseEntity} with the given HTTP status.
     *
     * @param status  the HTTP status to return
     * @param message the human-readable error description
     * @param request the current HTTP request (used to extract the URI)
     * @return a {@link ResponseEntity} containing the {@link ErrorResponse}
     */
    private ResponseEntity<ErrorResponse> buildResponse(
            HttpStatus status, String message, HttpServletRequest request) {
        ErrorResponse body = ErrorResponse.builder()
                .timestamp(Instant.now())
                .status(status.value())
                .error(status.getReasonPhrase())
                .message(message)
                .path(request.getRequestURI())
                .build();
        return ResponseEntity.status(status).body(body);
    }
}
