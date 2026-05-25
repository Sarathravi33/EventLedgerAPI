package com.eventledger.exception;

/**
 * Thrown when no transaction events are found for a given account identifier.
 *
 * <p>Mapped to HTTP {@code 404 Not Found} by
 * {@link GlobalExceptionHandler#handleAccountNotFound}.
 *
 * @author Sarathkumar Ravi
 */
public class AccountNotFoundException extends RuntimeException {

    /**
     * Constructs an {@code AccountNotFoundException} for the specified account.
     *
     * @param accountId the account identifier for which no events were found
     */
    public AccountNotFoundException(String accountId) {
        super("No events found for account: " + accountId);
    }
}
