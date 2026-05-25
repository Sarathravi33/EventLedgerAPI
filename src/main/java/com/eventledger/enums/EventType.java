package com.eventledger.enums;

/**
 * Enumeration of supported financial transaction event types.
 *
 * <p>Used to classify each {@code TransactionEvent} and drive balance
 * calculation logic: CREDIT amounts are added and DEBIT amounts are
 * subtracted when computing an account balance.
 *
 * @author Sarathkumar Ravi
 */
public enum EventType {

    /** Represents an incoming fund movement that increases the account balance. */
    CREDIT,

    /** Represents an outgoing fund movement that decreases the account balance. */
    DEBIT
}
