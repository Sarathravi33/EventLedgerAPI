package com.eventledger.service;

import com.eventledger.dto.BalanceResponse;
import com.eventledger.dto.EventRequest;
import com.eventledger.dto.EventResponse;
import com.eventledger.entity.TransactionEvent;
import com.eventledger.exception.AccountNotFoundException;
import com.eventledger.exception.EventNotFoundException;
import com.eventledger.repository.TransactionEventRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;

/**
 * Default implementation of {@link EventService}.
 *
 * <p>Delegates all persistence to {@link TransactionEventRepository} and uses
 * a shared {@link ObjectMapper} for serialising and deserialising the optional
 * JSON metadata field.
 *
 * <p>Idempotency is enforced at the service layer: {@link #submitEvent} checks
 * for an existing record by {@code eventId} before attempting an insert.
 * A unique constraint on the database column provides a second layer of
 * protection against concurrent duplicate submissions.
 *
 * @author Sarathkumar Ravi
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EventServiceImpl implements EventService {

    private final TransactionEventRepository repository;
    private final ObjectMapper objectMapper;

    /**
     * {@inheritDoc}
     *
     * <p>If the {@code eventId} is already present in the store, the existing
     * record is returned immediately without touching the database write path.
     *
     * <p>No outer transaction wraps this method deliberately. Each repository
     * call runs in its own transaction so that a {@link DataIntegrityViolationException}
     * from a concurrent duplicate insert only rolls back that single save — leaving
     * the connection clean for the follow-up re-read that returns the winner's record.
     *
     * @param request the event payload containing the idempotency key and transaction details
     */
    @Override
    public SubmitResult submitEvent(EventRequest request) {
        Optional<TransactionEvent> existing = repository.findByEventId(request.getEventId());
        if (existing.isPresent()) {
            log.debug("Duplicate event received: {}", request.getEventId());
            return new SubmitResult(toResponse(existing.get()), false);
        }

        TransactionEvent entity = TransactionEvent.builder()
                .eventId(request.getEventId())
                .accountId(request.getAccountId())
                .type(request.getType())
                .amount(request.getAmount())
                .currency(request.getCurrency())
                .eventTimestamp(request.getEventTimestamp())
                .metadata(toJson(request.getMetadata()))
                .build();

        try {
            // saveAndFlush flushes within its own transaction so the unique constraint
            // fires immediately — making the DataIntegrityViolationException catchable here.
            TransactionEvent saved = repository.saveAndFlush(entity);
            log.debug("Saved new event: {}", saved.getEventId());
            return new SubmitResult(toResponse(saved), true);
        } catch (DataIntegrityViolationException e) {
            // A concurrent request inserted the same eventId between our read and write.
            // The DB constraint is the authoritative guard — treat this as a duplicate.
            log.debug("Concurrent duplicate detected for eventId={}, returning existing record",
                    request.getEventId());
            return repository.findByEventId(request.getEventId())
                    .map(ev -> new SubmitResult(toResponse(ev), false))
                    .orElseThrow(() -> new IllegalStateException(
                            "Unique constraint violated but no record found for eventId: "
                                    + request.getEventId(), e));
        }
    }

    /**
     * {@inheritDoc}
     *
     * @param eventId the unique event identifier to look up
     * @throws EventNotFoundException if no event with the given ID exists
     */
    @Override
    @Transactional(readOnly = true)
    public EventResponse getEventById(String eventId) {
        return repository.findByEventId(eventId)
                .map(this::toResponse)
                .orElseThrow(() -> new EventNotFoundException(eventId));
    }

    /**
     * {@inheritDoc}
     *
     * @param accountId the account whose events are to be retrieved
     * @param page      zero-based page index
     * @param size      number of events per page
     * @throws AccountNotFoundException if the account has no recorded events
     */
    @Override
    @Transactional(readOnly = true)
    public Page<EventResponse> getEventsByAccount(String accountId, int page, int size) {
        if (!repository.existsByAccountId(accountId)) {
            throw new AccountNotFoundException(accountId);
        }
        PageRequest pageable = PageRequest.of(page, size, Sort.by("eventTimestamp").ascending());
        return repository.findByAccountId(accountId, pageable)
                .map(this::toResponse);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Currency is inferred from the earliest event for the account.
     * Falls back to {@code "USD"} if no events are present (should not
     * occur given the existence check above).
     *
     * @param accountId the account whose balance is to be calculated
     * @throws AccountNotFoundException if the account has no recorded events
     */
    @Override
    @Transactional(readOnly = true)
    public BalanceResponse getBalanceByAccount(String accountId) {
        if (!repository.existsByAccountId(accountId)) {
            throw new AccountNotFoundException(accountId);
        }
        BigDecimal balance = repository.calculateBalanceByAccountId(accountId);
        // Determine currency from first event for this account
        String currency = repository.findByAccountIdOrderByEventTimestampAsc(accountId)
                .stream()
                .findFirst()
                .map(TransactionEvent::getCurrency)
                .orElse("USD");
        return new BalanceResponse(accountId, balance, currency);
    }

    /**
     * Maps a {@link TransactionEvent} entity to an {@link EventResponse} DTO.
     *
     * @param entity the persisted entity to convert
     * @return the corresponding response DTO
     */
    private EventResponse toResponse(TransactionEvent entity) {
        return EventResponse.builder()
                .eventId(entity.getEventId())
                .accountId(entity.getAccountId())
                .type(entity.getType())
                .amount(entity.getAmount())
                .currency(entity.getCurrency())
                .eventTimestamp(entity.getEventTimestamp())
                .metadata(fromJson(entity.getMetadata()))
                .createdAt(entity.getCreatedAt())
                .build();
    }

    /**
     * Serialises a metadata map to a JSON string.
     *
     * <p>Returns {@code null} if the map is {@code null} or if serialisation
     * fails; in the latter case a warning is logged to aid debugging.
     *
     * @param metadata the key/value map to serialise; may be {@code null}
     * @return the JSON string, or {@code null} on failure or absent input
     */
    private String toJson(Map<String, Object> metadata) {
        if (metadata == null) return null;
        try {
            return objectMapper.writeValueAsString(metadata);
        } catch (JsonProcessingException e) {
            log.warn("Failed to serialize metadata", e);
            return null;
        }
    }

    /**
     * Deserialises a JSON string back into a metadata map.
     *
     * <p>Returns {@code null} if the input is {@code null} or if parsing
     * fails; in the latter case a warning is logged to aid debugging.
     *
     * @param json the JSON string to parse; may be {@code null}
     * @return the parsed key/value map, or {@code null} on failure or absent input
     */
    private Map<String, Object> fromJson(String json) {
        if (json == null) return null;
        try {
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (JsonProcessingException e) {
            log.warn("Failed to deserialize metadata", e);
            return null;
        }
    }
}
