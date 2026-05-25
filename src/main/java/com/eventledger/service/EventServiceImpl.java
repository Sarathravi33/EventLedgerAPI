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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class EventServiceImpl implements EventService {

    private final TransactionEventRepository repository;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
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

        TransactionEvent saved = repository.save(entity);
        log.debug("Saved new event: {}", saved.getEventId());
        return new SubmitResult(toResponse(saved), true);
    }

    @Override
    @Transactional(readOnly = true)
    public EventResponse getEventById(String eventId) {
        return repository.findByEventId(eventId)
                .map(this::toResponse)
                .orElseThrow(() -> new EventNotFoundException(eventId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<EventResponse> getEventsByAccount(String accountId) {
        if (!repository.existsByAccountId(accountId)) {
            throw new AccountNotFoundException(accountId);
        }
        return repository.findByAccountIdOrderByEventTimestampAsc(accountId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

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

    private String toJson(Map<String, Object> metadata) {
        if (metadata == null) return null;
        try {
            return objectMapper.writeValueAsString(metadata);
        } catch (JsonProcessingException e) {
            log.warn("Failed to serialize metadata", e);
            return null;
        }
    }

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
