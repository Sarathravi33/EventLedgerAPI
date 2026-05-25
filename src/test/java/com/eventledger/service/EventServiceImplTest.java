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
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link EventServiceImpl}.
 *
 * <p>The repository is replaced with a Mockito mock so that each test
 * exercises only the service logic in isolation, without a real database.
 * The {@link ObjectMapper} is constructed directly with the same configuration
 * used in production to keep serialisation behaviour realistic.
 *
 * @author Sarathkumar Ravi
 */
@ExtendWith(MockitoExtension.class)
class EventServiceImplTest {

    @Mock
    private TransactionEventRepository repository;

    private EventServiceImpl service;

    /**
     * Constructs the service under test with a real {@link ObjectMapper}
     * (JavaTimeModule enabled, timestamps disabled) before each test.
     */
    @BeforeEach
    void setUp() {
        ObjectMapper objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        service = new EventServiceImpl(repository, objectMapper);
    }

    /**
     * Builds a minimal {@link EventRequest} with the given parameters.
     *
     * @param eventId   unique event identifier
     * @param accountId account to associate the event with
     * @param type      {@code CREDIT} or {@code DEBIT}
     * @param amount    transaction amount
     * @return a populated {@link EventRequest}
     */
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

    /**
     * Builds a {@link TransactionEvent} entity with the given parameters,
     * simulating a record as it would appear after being persisted.
     *
     * @param eventId   unique event identifier
     * @param accountId account the event belongs to
     * @param type      {@code CREDIT} or {@code DEBIT}
     * @param amount    transaction amount
     * @return a populated {@link TransactionEvent}
     */
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

    /**
     * A new event (no existing record for its {@code eventId}) must be saved
     * and the result must have {@code created = true}.
     */
    @Test
    void submitEvent_newEvent_returnsCreatedTrue() {
        EventRequest request = buildRequest("evt-001", "acct-123", EventType.CREDIT, new BigDecimal("100.00"));
        TransactionEvent saved = buildEntity("evt-001", "acct-123", EventType.CREDIT, new BigDecimal("100.00"));

        when(repository.findByEventId("evt-001")).thenReturn(Optional.empty());
        when(repository.saveAndFlush(any())).thenReturn(saved);

        EventService.SubmitResult result = service.submitEvent(request);

        assertThat(result.created()).isTrue();
        assertThat(result.response().getEventId()).isEqualTo("evt-001");
        verify(repository, times(1)).saveAndFlush(any());
    }

    /**
     * Re-submitting a previously processed event must return the existing record
     * with {@code created = false} and must not call {@code saveAndFlush} on the repository.
     */
    @Test
    void submitEvent_duplicate_returnsCreatedFalse_noSaveCall() {
        EventRequest request = buildRequest("evt-001", "acct-123", EventType.CREDIT, new BigDecimal("100.00"));
        TransactionEvent existing = buildEntity("evt-001", "acct-123", EventType.CREDIT, new BigDecimal("100.00"));

        when(repository.findByEventId("evt-001")).thenReturn(Optional.of(existing));

        EventService.SubmitResult result = service.submitEvent(request);

        assertThat(result.created()).isFalse();
        assertThat(result.response().getEventId()).isEqualTo("evt-001");
        verify(repository, never()).saveAndFlush(any());
    }

    /**
     * When two concurrent requests carry the same {@code eventId}, the unique
     * constraint on the database column rejects the second insert with a
     * {@link DataIntegrityViolationException}. The service must catch this,
     * re-read the winner's record, and return it with {@code created = false}.
     */
    @Test
    void submitEvent_concurrentDuplicate_returnsExistingRecordWithCreatedFalse() {
        EventRequest request = buildRequest("evt-race", "acct-123", EventType.CREDIT, new BigDecimal("100.00"));
        TransactionEvent winner = buildEntity("evt-race", "acct-123", EventType.CREDIT, new BigDecimal("100.00"));

        // First read sees nothing (both concurrent threads pass this check)
        when(repository.findByEventId("evt-race"))
                .thenReturn(Optional.empty())          // initial check — record not yet visible
                .thenReturn(Optional.of(winner));      // re-read after constraint violation

        // Simulate the DB rejecting our insert because the other thread won
        when(repository.saveAndFlush(any())).thenThrow(DataIntegrityViolationException.class);

        EventService.SubmitResult result = service.submitEvent(request);

        assertThat(result.created()).isFalse();
        assertThat(result.response().getEventId()).isEqualTo("evt-race");
        verify(repository, times(2)).findByEventId("evt-race");
    }

    /**
     * Looking up an existing event by ID must return the corresponding
     * {@link EventResponse} with matching field values.
     */
    @Test
    void getEventById_existingId_returnsEventResponse() {
        TransactionEvent entity = buildEntity("evt-001", "acct-123", EventType.CREDIT, new BigDecimal("100.00"));
        when(repository.findByEventId("evt-001")).thenReturn(Optional.of(entity));

        EventResponse response = service.getEventById("evt-001");

        assertThat(response.getEventId()).isEqualTo("evt-001");
        assertThat(response.getAccountId()).isEqualTo("acct-123");
    }

    /**
     * Looking up an event ID that does not exist must throw
     * {@link EventNotFoundException} with the unknown ID in the message.
     */
    @Test
    void getEventById_notFound_throwsEventNotFoundException() {
        when(repository.findByEventId("evt-999")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getEventById("evt-999"))
                .isInstanceOf(EventNotFoundException.class)
                .hasMessageContaining("evt-999");
    }

    /**
     * Requesting events for a known account must return a page containing
     * all events for that account.
     */
    @Test
    void getEventsByAccount_existingAccount_returnsPage() {
        TransactionEvent e1 = buildEntity("evt-001", "acct-123", EventType.CREDIT, new BigDecimal("100.00"));
        TransactionEvent e2 = buildEntity("evt-002", "acct-123", EventType.DEBIT, new BigDecimal("30.00"));

        when(repository.existsByAccountId("acct-123")).thenReturn(true);
        when(repository.findByAccountId(eq("acct-123"), any())).thenReturn(new PageImpl<>(List.of(e1, e2)));

        Page<EventResponse> result = service.getEventsByAccount("acct-123", 0, 20);

        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getTotalElements()).isEqualTo(2);
    }

    /**
     * Requesting events for an account with no recorded events must throw
     * {@link AccountNotFoundException} with the unknown account ID in the message.
     */
    @Test
    void getEventsByAccount_nonExistentAccount_throwsAccountNotFoundException() {
        when(repository.existsByAccountId("acct-999")).thenReturn(false);

        assertThatThrownBy(() -> service.getEventsByAccount("acct-999", 0, 20))
                .isInstanceOf(AccountNotFoundException.class)
                .hasMessageContaining("acct-999");
    }

    /**
     * The balance for an account with both CREDIT and DEBIT events must match
     * the value returned by the repository's balance query.
     */
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

    /**
     * Requesting the balance for an account with no recorded events must throw
     * {@link AccountNotFoundException} with the unknown account ID in the message.
     */
    @Test
    void getBalance_accountNotFound_throwsAccountNotFoundException() {
        when(repository.existsByAccountId("acct-999")).thenReturn(false);

        assertThatThrownBy(() -> service.getBalanceByAccount("acct-999"))
                .isInstanceOf(AccountNotFoundException.class)
                .hasMessageContaining("acct-999");
    }
}
