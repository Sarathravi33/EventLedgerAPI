package com.eventledger.dto;

import com.eventledger.enums.EventType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

/**
 * Inbound request payload for submitting a transaction event.
 *
 * <p>All required fields are validated via Bean Validation constraints.
 * The {@code eventId} acts as the idempotency key: submitting two
 * requests with the same {@code eventId} will not create a duplicate.
 *
 * @author Sarathkumar Ravi
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EventRequest {

    /**
     * Unique identifier for this event, used as the idempotency key.
     * Must not be blank.
     */
    @NotBlank(message = "eventId must not be blank")
    private String eventId;

    /**
     * Identifier of the account to which this event belongs.
     * Must not be blank.
     */
    @NotBlank(message = "accountId must not be blank")
    private String accountId;

    /**
     * Type of the transaction: {@code CREDIT} or {@code DEBIT}.
     * Must not be null.
     */
    @NotNull(message = "type must not be null")
    private EventType type;

    /**
     * Monetary amount of the transaction.
     * Must be a positive value greater than zero.
     */
    @NotNull(message = "amount must not be null")
    @Positive(message = "amount must be greater than 0")
    private BigDecimal amount;

    /**
     * ISO 4217 currency code (e.g. {@code USD}).
     * Must not be blank.
     */
    @NotBlank(message = "currency must not be blank")
    private String currency;

    /**
     * The point in time at which the event occurred, in UTC.
     * Must not be null.
     */
    @NotNull(message = "eventTimestamp must not be null")
    private Instant eventTimestamp;

    /**
     * Optional free-form key/value metadata associated with this event.
     * Serialised as JSON text in the database.
     */
    private Map<String, Object> metadata;
}
