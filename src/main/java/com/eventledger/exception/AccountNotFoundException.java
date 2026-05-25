package com.eventledger.exception;

public class AccountNotFoundException extends RuntimeException {

    public AccountNotFoundException(String accountId) {
        super("No events found for account: " + accountId);
    }
}
