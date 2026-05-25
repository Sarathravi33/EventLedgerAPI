package com.eventledger.dto;

import com.eventledger.enums.EventType;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

/**
 * Outbound response payload representing a persisted transaction event.
 *
 * @author Sarathkumar Ravi
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EventResponse {

    /** Unique identifier of the event (idempotency key). */
    private String eventId;

    /** Identifier of the account this event belongs to. */
    private String accountId;

    /** Type of the transaction: {@code CREDIT} or {@code DEBIT}. */
    private EventType type;

    /** Monetary amount of the transaction. */
    private BigDecimal amount;

    /** ISO 4217 currency code (e.g. {@code USD}). */
    private String currency;

    /** Timestamp at which the event occurred, as supplied by the client. */
    private Instant eventTimestamp;

    /** Optional key/value metadata associated with this event. */
    private Map<String, Object> metadata;

    /** Timestamp at which the event was persisted to the store. */
    private Instant createdAt;
}
