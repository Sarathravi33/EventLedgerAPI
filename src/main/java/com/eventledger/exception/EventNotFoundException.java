package com.eventledger.exception;

/**
 * Thrown when a transaction event cannot be found by its event identifier.
 *
 * <p>Mapped to HTTP {@code 404 Not Found} by
 * {@link GlobalExceptionHandler#handleEventNotFound}.
 *
 * @author Sarathkumar Ravi
 */
public class EventNotFoundException extends RuntimeException {

    /**
     * Constructs an {@code EventNotFoundException} for the specified event ID.
     *
     * @param eventId the event identifier that could not be found
     */
    public EventNotFoundException(String eventId) {
        super("Event not found: " + eventId);
    }
}
