package com.eventledger.service;

import com.eventledger.dto.BalanceResponse;
import com.eventledger.dto.EventRequest;
import com.eventledger.dto.EventResponse;
import com.eventledger.entity.TransactionEvent;
import com.eventledger.enums.EventType;
import com.eventledger.exception.AccountNotFoundException;
import com.eventledger.exception.EventNotFoundException;
import com.eventledger.repository.TransactionEventRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EventServiceImplTest {

    @Mock
    private TransactionEventRepository repository;

    private EventServiceImpl service;

    @BeforeEach
    void setUp() {
        ObjectMapper objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        service = new EventServiceImpl(repository, objectMapper);
    }

    private EventRequest buildRequest(String eventId, String accountId, EventType type, BigDecimal amount) {
        return EventRequest.builder()
                .eventId(eventId)
                .accountId(accountId)
                .type(type)
                .amount(amount)
                .currency("USD")
                .eventTimestamp(Instant.parse("2026-05-15T10:00:00Z"))
                .build();
    }

    private TransactionEvent buildEntity(String eventId, String accountId, EventType type, BigDecimal amount) {
        return TransactionEvent.builder()
                .id(1L)
                .eventId(eventId)
                .accountId(accountId)
                .type(type)
                .amount(amount)
                .currency("USD")
                .eventTimestamp(Instant.parse("2026-05-15T10:00:00Z"))
                .createdAt(Instant.now())
                .build();
    }

    @Test
    void submitEvent_newEvent_returnsCreatedTrue() {
        EventRequest request = buildRequest("evt-001", "acct-123", EventType.CREDIT, new BigDecimal("100.00"));
        TransactionEvent saved = buildEntity("evt-001", "acct-123", EventType.CREDIT, new BigDecimal("100.00"));

        when(repository.findByEventId("evt-001")).thenReturn(Optional.empty());
        when(repository.save(any())).thenReturn(saved);

        EventService.SubmitResult result = service.submitEvent(request);

        assertThat(result.created()).isTrue();
        assertThat(result.response().getEventId()).isEqualTo("evt-001");
        verify(repository, times(1)).save(any());
    }

    @Test
    void submitEvent_duplicate_returnsCreatedFalse_noSaveCall() {
        EventRequest request = buildRequest("evt-001", "acct-123", EventType.CREDIT, new BigDecimal("100.00"));
        TransactionEvent existing = buildEntity("evt-001", "acct-123", EventType.CREDIT, new BigDecimal("100.00"));

        when(repository.findByEventId("evt-001")).thenReturn(Optional.of(existing));

        EventService.SubmitResult result = service.submitEvent(request);

        assertThat(result.created()).isFalse();
        assertThat(result.response().getEventId()).isEqualTo("evt-001");
        verify(repository, never()).save(any());
    }

    @Test
    void getEventById_existingId_returnsEventResponse() {
        TransactionEvent entity = buildEntity("evt-001", "acct-123", EventType.CREDIT, new BigDecimal("100.00"));
        when(repository.findByEventId("evt-001")).thenReturn(Optional.of(entity));

        EventResponse response = service.getEventById("evt-001");

        assertThat(response.getEventId()).isEqualTo("evt-001");
        assertThat(response.getAccountId()).isEqualTo("acct-123");
    }

    @Test
    void getEventById_notFound_throwsEventNotFoundException() {
        when(repository.findByEventId("evt-999")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getEventById("evt-999"))
                .isInstanceOf(EventNotFoundException.class)
                .hasMessageContaining("evt-999");
    }

    @Test
    void getEventsByAccount_existingAccount_returnsList() {
        TransactionEvent e1 = buildEntity("evt-001", "acct-123", EventType.CREDIT, new BigDecimal("100.00"));
        TransactionEvent e2 = buildEntity("evt-002", "acct-123", EventType.DEBIT, new BigDecimal("30.00"));

        when(repository.existsByAccountId("acct-123")).thenReturn(true);
        when(repository.findByAccountIdOrderByEventTimestampAsc("acct-123")).thenReturn(List.of(e1, e2));

        List<EventResponse> result = service.getEventsByAccount("acct-123");

        assertThat(result).hasSize(2);
    }

    @Test
    void getEventsByAccount_nonExistentAccount_throwsAccountNotFoundException() {
        when(repository.existsByAccountId("acct-999")).thenReturn(false);

        assertThatThrownBy(() -> service.getEventsByAccount("acct-999"))
                .isInstanceOf(AccountNotFoundException.class)
                .hasMessageContaining("acct-999");
    }

    @Test
    void getBalance_correctNetCalculation() {
        TransactionEvent entity = buildEntity("evt-001", "acct-123", EventType.CREDIT, new BigDecimal("100.00"));

        when(repository.existsByAccountId("acct-123")).thenReturn(true);
        when(repository.calculateBalanceByAccountId("acct-123")).thenReturn(new BigDecimal("70.00"));
        when(repository.findByAccountIdOrderByEventTimestampAsc("acct-123")).thenReturn(List.of(entity));

        BalanceResponse balance = service.getBalanceByAccount("acct-123");

        assertThat(balance.getBalance()).isEqualByComparingTo("70.00");
        assertThat(balance.getAccountId()).isEqualTo("acct-123");
    }

    @Test
    void getBalance_accountNotFound_throwsAccountNotFoundException() {
        when(repository.existsByAccountId("acct-999")).thenReturn(false);

        assertThatThrownBy(() -> service.getBalanceByAccount("acct-999"))
                .isInstanceOf(AccountNotFoundException.class)
                .hasMessageContaining("acct-999");
    }
}
