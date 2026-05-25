package com.eventledger.dto;

import com.eventledger.enums.EventType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

/**
 * Outbound response payload representing a persisted transaction event.
 *
 * @author Sarathkumar Ravi
 */
@Schema(description = "Response payload representing a persisted transaction event")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EventResponse {

    @Schema(description = "Unique identifier of the event (idempotency key)", example = "evt-001")
    private String eventId;

    @Schema(description = "Account this event belongs to", example = "acct-123")
    private String accountId;

    @Schema(description = "Transaction type: CREDIT or DEBIT", example = "CREDIT")
    private EventType type;

    @Schema(description = "Monetary amount of the transaction", example = "150.00")
    private BigDecimal amount;

    @Schema(description = "ISO 4217 currency code", example = "USD")
    private String currency;

    @Schema(description = "UTC timestamp at which the event occurred (ISO-8601)", example = "2026-05-15T14:02:11Z")
    private Instant eventTimestamp;

    @Schema(description = "Optional free-form key/value metadata")
    private Map<String, Object> metadata;

    @Schema(description = "UTC timestamp at which the event was persisted", example = "2026-05-15T14:02:12Z")
    private Instant createdAt;
}
