package com.eventledger.dto;

import com.eventledger.enums.EventType;
import io.swagger.v3.oas.annotations.media.Schema;
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
@Schema(description = "Request payload for submitting a transaction event")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EventRequest {

    /**
     * Unique identifier for this event, used as the idempotency key.
     * Must not be blank.
     */
    @Schema(description = "Unique event identifier; acts as the idempotency key", example = "evt-001", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "eventId must not be blank")
    private String eventId;

    /**
     * Identifier of the account to which this event belongs.
     * Must not be blank.
     */
    @Schema(description = "Account to which this event belongs", example = "acct-123", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "accountId must not be blank")
    private String accountId;

    /**
     * Type of the transaction: {@code CREDIT} or {@code DEBIT}.
     * Must not be null.
     */
    @Schema(description = "Transaction type: CREDIT increases the balance, DEBIT decreases it", example = "CREDIT", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "type must not be null")
    private EventType type;

    /**
     * Monetary amount of the transaction.
     * Must be a positive value greater than zero.
     */
    @Schema(description = "Monetary amount; must be greater than zero", example = "150.00", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "amount must not be null")
    @Positive(message = "amount must be greater than 0")
    private BigDecimal amount;

    /**
     * ISO 4217 currency code (e.g. {@code USD}).
     * Must not be blank.
     */
    @Schema(description = "ISO 4217 currency code", example = "USD", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "currency must not be blank")
    private String currency;

    /**
     * The point in time at which the event occurred, in UTC.
     * Must not be null.
     */
    @Schema(description = "UTC timestamp at which the event occurred (ISO-8601)", example = "2026-05-15T14:02:11Z", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "eventTimestamp must not be null")
    private Instant eventTimestamp;

    /**
     * Optional free-form key/value metadata associated with this event.
     * Serialised as JSON text in the database.
     */
    @Schema(description = "Optional free-form key/value metadata", example = "{\"source\": \"mainframe-batch\", \"batchId\": \"B-9042\"}")
    private Map<String, Object> metadata;
}
