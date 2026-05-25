package com.eventledger.controller;

import com.eventledger.dto.EventRequest;
import com.eventledger.enums.EventType;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for {@link EventController} covering all three endpoints:
 * <ul>
 *   <li>{@code POST /events} — submit a new or duplicate event</li>
 *   <li>{@code GET /events/{id}} — retrieve an event by ID</li>
 *   <li>{@code GET /events?account={accountId}} — list events for an account</li>
 * </ul>
 *
 * <p>Each test runs against the full Spring context backed by an in-memory H2
 * database. The {@code @Transactional} annotation rolls back all changes after
 * each test, guaranteeing isolation without manual teardown.
 *
 * <p>A random account ID is generated per test to prevent data leaking between
 * test cases.
 *
 * @author Sarathkumar Ravi
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class EventControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    /** Unique account ID generated fresh for each test to ensure isolation. */
    private String uniqueAccount;

    /**
     * Creates a fresh random account ID before each test so that data
     * written by one test cannot affect another.
     */
    @BeforeEach
    void setUp() {
        uniqueAccount = "acct-" + UUID.randomUUID();
    }

    /**
     * Builds a fully-populated {@link EventRequest} for use in tests.
     *
     * @param eventId   unique event identifier
     * @param accountId account to associate the event with
     * @param type      {@code CREDIT} or {@code DEBIT}
     * @param amount    transaction amount
     * @param timestamp ISO-8601 UTC timestamp string for the event
     * @return a ready-to-submit {@link EventRequest}
     */
    private EventRequest buildRequest(String eventId, String accountId, EventType type, double amount, String timestamp) {
        return EventRequest.builder()
                .eventId(eventId)
                .accountId(accountId)
                .type(type)
                .amount(new BigDecimal(String.valueOf(amount)))
                .currency("USD")
                .eventTimestamp(Instant.parse(timestamp))
                .metadata(Map.of("source", "test"))
                .build();
    }

    /**
     * Helper that POSTs the given request and ignores the response.
     * Used to seed data for subsequent GET assertions.
     *
     * @param request the event payload to submit
     * @throws Exception if the MockMvc request fails unexpectedly
     */
    private void postEvent(EventRequest request) throws Exception {
        mockMvc.perform(post("/events")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));
    }

    // ── POST /events ──────────────────────────────────────────────────────────

    /**
     * A valid new event must be persisted and return HTTP 201 Created with
     * the full event body in the response.
     */
    @Test
    void postEvent_validPayload_returns201Created() throws Exception {
        EventRequest request = buildRequest("evt-001", uniqueAccount, EventType.CREDIT, 150.00, "2026-05-15T10:00:00Z");

        mockMvc.perform(post("/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.eventId").value("evt-001"))
                .andExpect(jsonPath("$.accountId").value(uniqueAccount))
                .andExpect(jsonPath("$.type").value("CREDIT"))
                .andExpect(jsonPath("$.amount").value(150.00))
                .andExpect(jsonPath("$.currency").value("USD"));
    }

    /**
     * Re-submitting an event with the same {@code eventId} must return HTTP 200 OK
     * with the original event data — no duplicate record should be created.
     */
    @Test
    void postEvent_duplicateEventId_returns200OkWithOriginalData() throws Exception {
        EventRequest request = buildRequest("evt-dup", uniqueAccount, EventType.CREDIT, 100.00, "2026-05-15T10:00:00Z");

        // First submission — 201
        mockMvc.perform(post("/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        // Second submission — 200 with same data
        mockMvc.perform(post("/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.eventId").value("evt-dup"))
                .andExpect(jsonPath("$.amount").value(100.00));
    }

    /**
     * A request with a missing {@code eventId} must be rejected with HTTP 400 Bad Request.
     */
    @Test
    void postEvent_missingEventId_returns400() throws Exception {
        String body = """
                {
                  "accountId": "acct-123",
                  "type": "CREDIT",
                  "amount": 100.00,
                  "currency": "USD",
                  "eventTimestamp": "2026-05-15T10:00:00Z"
                }
                """;
        mockMvc.perform(post("/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    /**
     * A request with a missing {@code accountId} must be rejected with HTTP 400 Bad Request.
     */
    @Test
    void postEvent_missingAccountId_returns400() throws Exception {
        String body = """
                {
                  "eventId": "evt-001",
                  "type": "CREDIT",
                  "amount": 100.00,
                  "currency": "USD",
                  "eventTimestamp": "2026-05-15T10:00:00Z"
                }
                """;
        mockMvc.perform(post("/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    /**
     * A zero {@code amount} violates the {@code @Positive} constraint and must
     * return HTTP 400 with an error message referencing the {@code amount} field.
     */
    @Test
    void postEvent_zeroAmount_returns400() throws Exception {
        EventRequest request = buildRequest("evt-zero", uniqueAccount, EventType.CREDIT, 0.00, "2026-05-15T10:00:00Z");

        mockMvc.perform(post("/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("amount")));
    }

    /**
     * A negative {@code amount} violates the {@code @Positive} constraint and must
     * return HTTP 400 with an error message referencing the {@code amount} field.
     */
    @Test
    void postEvent_negativeAmount_returns400() throws Exception {
        EventRequest request = buildRequest("evt-neg", uniqueAccount, EventType.CREDIT, -50.00, "2026-05-15T10:00:00Z");

        mockMvc.perform(post("/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("amount")));
    }

    /**
     * An unrecognised {@code type} value (e.g. {@code "TRANSFER"}) cannot be
     * deserialised into {@link EventType} and must return HTTP 400 Bad Request.
     */
    @Test
    void postEvent_invalidType_returns400() throws Exception {
        String body = """
                {
                  "eventId": "evt-bad-type",
                  "accountId": "acct-123",
                  "type": "TRANSFER",
                  "amount": 100.00,
                  "currency": "USD",
                  "eventTimestamp": "2026-05-15T10:00:00Z"
                }
                """;
        mockMvc.perform(post("/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    /**
     * A request with a missing {@code eventTimestamp} violates the {@code @NotNull}
     * constraint and must return HTTP 400 Bad Request.
     */
    @Test
    void postEvent_missingTimestamp_returns400() throws Exception {
        String body = """
                {
                  "eventId": "evt-no-ts",
                  "accountId": "acct-123",
                  "type": "CREDIT",
                  "amount": 100.00,
                  "currency": "USD"
                }
                """;
        mockMvc.perform(post("/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    /**
     * A syntactically invalid JSON body must return HTTP 400 Bad Request.
     */
    @Test
    void postEvent_malformedJson_returns400() throws Exception {
        mockMvc.perform(post("/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{invalid json"))
                .andExpect(status().isBadRequest());
    }

    // ── GET /events/{id} ──────────────────────────────────────────────────────

    /**
     * A GET request for an existing event ID must return HTTP 200 OK with
     * the corresponding event body.
     */
    @Test
    void getEvent_existingId_returns200WithBody() throws Exception {
        EventRequest request = buildRequest("evt-get-1", uniqueAccount, EventType.CREDIT, 200.00, "2026-05-15T10:00:00Z");
        postEvent(request);

        mockMvc.perform(get("/events/evt-get-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.eventId").value("evt-get-1"))
                .andExpect(jsonPath("$.amount").value(200.00));
    }

    /**
     * A GET request for an unknown event ID must return HTTP 404 Not Found
     * with a status field in the error body.
     */
    @Test
    void getEvent_nonExistentId_returns404() throws Exception {
        mockMvc.perform(get("/events/evt-does-not-exist"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    // ── GET /events?account={accountId} ──────────────────────────────────────

    /**
     * A GET request for a known account must return HTTP 200 OK with a paged
     * response whose {@code content} contains all events for that account.
     */
    @Test
    void getEventsByAccount_existingAccount_returns200WithPagedContent() throws Exception {
        postEvent(buildRequest("evt-list-1", uniqueAccount, EventType.CREDIT, 100.00, "2026-05-15T10:00:00Z"));
        postEvent(buildRequest("evt-list-2", uniqueAccount, EventType.DEBIT, 30.00, "2026-05-15T11:00:00Z"));

        mockMvc.perform(get("/events").param("account", uniqueAccount))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.totalPages").value(1));
    }

    /**
     * Events submitted out of chronological order must be returned sorted by
     * {@code eventTimestamp} ascending (08:00 → 10:00 → 12:00), regardless of
     * submission order.
     */
    @Test
    void getEventsByAccount_outOfOrderSubmission_returnsChronologicalOrder() throws Exception {
        // Submit in reverse chronological order
        postEvent(buildRequest("evt-ooo-c", uniqueAccount, EventType.CREDIT, 100.00, "2026-05-15T12:00:00Z"));
        postEvent(buildRequest("evt-ooo-a", uniqueAccount, EventType.DEBIT,  50.00, "2026-05-15T08:00:00Z"));
        postEvent(buildRequest("evt-ooo-b", uniqueAccount, EventType.CREDIT, 200.00, "2026-05-15T10:00:00Z"));

        mockMvc.perform(get("/events").param("account", uniqueAccount))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(3)))
                // Chronological order: 08:00 → 10:00 → 12:00
                .andExpect(jsonPath("$.content[0].eventId").value("evt-ooo-a"))
                .andExpect(jsonPath("$.content[1].eventId").value("evt-ooo-b"))
                .andExpect(jsonPath("$.content[2].eventId").value("evt-ooo-c"));
    }

    /**
     * A GET request for an account with no recorded events must return
     * HTTP 404 Not Found.
     */
    @Test
    void getEventsByAccount_nonExistentAccount_returns404() throws Exception {
        mockMvc.perform(get("/events").param("account", "acct-unknown-" + UUID.randomUUID()))
                .andExpect(status().isNotFound());
    }

    /**
     * Requesting page 0 with size 2 from a 3-event account must return
     * the first two events and indicate a second page exists.
     */
    @Test
    void getEventsByAccount_firstPage_returnsCorrectSlice() throws Exception {
        postEvent(buildRequest("evt-pg-1", uniqueAccount, EventType.CREDIT, 10.00, "2026-05-15T08:00:00Z"));
        postEvent(buildRequest("evt-pg-2", uniqueAccount, EventType.CREDIT, 20.00, "2026-05-15T09:00:00Z"));
        postEvent(buildRequest("evt-pg-3", uniqueAccount, EventType.CREDIT, 30.00, "2026-05-15T10:00:00Z"));

        mockMvc.perform(get("/events")
                        .param("account", uniqueAccount)
                        .param("page", "0")
                        .param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.content[0].eventId").value("evt-pg-1"))
                .andExpect(jsonPath("$.content[1].eventId").value("evt-pg-2"))
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.totalPages").value(2))
                .andExpect(jsonPath("$.first").value(true))
                .andExpect(jsonPath("$.last").value(false));
    }

    /**
     * Requesting page 1 with size 2 from a 3-event account must return
     * only the third event and flag it as the last page.
     */
    @Test
    void getEventsByAccount_secondPage_returnsRemainingEvents() throws Exception {
        postEvent(buildRequest("evt-pg2-1", uniqueAccount, EventType.CREDIT, 10.00, "2026-05-15T08:00:00Z"));
        postEvent(buildRequest("evt-pg2-2", uniqueAccount, EventType.CREDIT, 20.00, "2026-05-15T09:00:00Z"));
        postEvent(buildRequest("evt-pg2-3", uniqueAccount, EventType.CREDIT, 30.00, "2026-05-15T10:00:00Z"));

        mockMvc.perform(get("/events")
                        .param("account", uniqueAccount)
                        .param("page", "1")
                        .param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].eventId").value("evt-pg2-3"))
                .andExpect(jsonPath("$.last").value(true));
    }

    /**
     * A negative page index must be rejected with HTTP 400 Bad Request.
     */
    @Test
    void getEventsByAccount_negativePage_returns400() throws Exception {
        mockMvc.perform(get("/events")
                        .param("account", uniqueAccount)
                        .param("page", "-1"))
                .andExpect(status().isBadRequest());
    }

    /**
     * A page size of zero must be rejected with HTTP 400 Bad Request.
     */
    @Test
    void getEventsByAccount_zeroSize_returns400() throws Exception {
        mockMvc.perform(get("/events")
                        .param("account", uniqueAccount)
                        .param("size", "0"))
                .andExpect(status().isBadRequest());
    }

    // ── Concurrency ───────────────────────────────────────────────────────────

    /**
     * Five threads submit the same {@code eventId} simultaneously. The DB unique
     * constraint guarantees exactly one insert succeeds; the service must catch
     * the resulting {@link org.springframework.dao.DataIntegrityViolationException}
     * and return {@code 200 OK} for every thread that lost the race — never a
     * {@code 500 Internal Server Error}.
     *
     * <p>{@code Propagation.NOT_SUPPORTED} opts this test out of the class-level
     * transaction so that each spawned thread's MockMvc request runs in its own
     * independent transaction, accurately mirroring real concurrent HTTP requests.
     */
    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void submitEvent_concurrentIdenticalRequests_onlyOneCreated() throws Exception {
        String concurrentEventId = "evt-race-" + UUID.randomUUID();
        String body = objectMapper.writeValueAsString(
                buildRequest(concurrentEventId, uniqueAccount, EventType.CREDIT, 100.00, "2026-05-15T10:00:00Z"));

        int threadCount = 5;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startGate = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);
        List<Integer> statuses = new CopyOnWriteArrayList<>();

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    startGate.await();
                    int status = mockMvc.perform(post("/events")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(body))
                            .andReturn()
                            .getResponse()
                            .getStatus();
                    statuses.add(status);
                } catch (Exception e) {
                    statuses.add(-1);
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startGate.countDown();
        assertThat(doneLatch.await(10, TimeUnit.SECONDS)).isTrue();
        executor.shutdown();

        assertThat(statuses).hasSize(threadCount);
        assertThat(statuses).doesNotContain(-1);
        assertThat(statuses.stream().filter(s -> s == 201).count()).isEqualTo(1);
        assertThat(statuses.stream().filter(s -> s == 200).count()).isEqualTo(threadCount - 1);
    }
}
