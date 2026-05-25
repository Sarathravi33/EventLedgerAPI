package com.eventledger.entity;

import com.eventledger.enums.EventType;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * JPA entity representing a single financial transaction event in the ledger.
 *
 * <p>Each event is uniquely identified by {@code eventId}, which serves as
 * the idempotency key. A unique index on {@code event_id} is defined at
 * the database level to guarantee that duplicates are rejected even under
 * concurrent inserts.
 *
 * <p>The {@code createdAt} field is set automatically by {@link #onCreate()}
 * and is never updated after initial persistence.
 *
 * @author Sarathkumar Ravi
 */
@Entity
@Table(
    name = "transaction_events",
    indexes = {
        @Index(name = "idx_event_id", columnList = "event_id", unique = true),
        @Index(name = "idx_account_id", columnList = "account_id")
    }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransactionEvent {

    /** Auto-generated surrogate primary key. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Client-supplied unique event identifier used as the idempotency key.
     * Maximum length 100 characters.
     */
    @Column(name = "event_id", nullable = false, unique = true, length = 100)
    private String eventId;

    /**
     * Identifier of the account to which this event is associated.
     * Maximum length 100 characters.
     */
    @Column(name = "account_id", nullable = false, length = 100)
    private String accountId;

    /**
     * Type of the transaction — either {@code CREDIT} or {@code DEBIT}.
     * Stored as a string to remain readable without requiring enum lookups.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private EventType type;

    /**
     * Monetary amount of the transaction.
     * Stored with precision 19 and scale 4 to support large values
     * and fractional currency units.
     */
    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    /** ISO 4217 currency code (e.g. {@code USD}). Maximum length 10 characters. */
    @Column(nullable = false, length = 10)
    private String currency;

    /** The point in time at which the event occurred, as reported by the client. */
    @Column(name = "event_timestamp", nullable = false)
    private Instant eventTimestamp;

    /**
     * Optional free-form metadata serialised as a JSON string.
     * Stored as TEXT to accommodate payloads of arbitrary size.
     */
    @Column(columnDefinition = "TEXT")
    private String metadata;

    /**
     * Timestamp at which this record was first persisted.
     * Set automatically before insert and never modified thereafter.
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    /**
     * JPA lifecycle callback that stamps {@code createdAt} with the current
     * UTC time immediately before the entity is first inserted.
     */
    @PrePersist
    void onCreate() {
        this.createdAt = Instant.now();
    }
}
