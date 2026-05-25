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
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for {@link AccountController} — {@code GET /accounts/{accountId}/balance}.
 *
 * <p>Each test runs against the full Spring context backed by an in-memory H2
 * database. The {@code @Transactional} annotation rolls back all database
 * changes after each test, keeping tests isolated without requiring explicit
 * teardown.
 *
 * <p>A random account ID is generated per test to avoid cross-test interference.
 *
 * @author Sarathkumar Ravi
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AccountControllerIntegrationTest {

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
     * Helper that POSTs a transaction event for {@link #uniqueAccount}.
     *
     * @param eventId the unique event identifier
     * @param type    {@code CREDIT} or {@code DEBIT}
     * @param amount  the transaction amount
     * @throws Exception if the MockMvc request fails unexpectedly
     */
    private void postEvent(String eventId, EventType type, double amount) throws Exception {
        EventRequest request = EventRequest.builder()
                .eventId(eventId)
                .accountId(uniqueAccount)
                .type(type)
                .amount(new BigDecimal(String.valueOf(amount)))
                .currency("USD")
                .eventTimestamp(Instant.parse("2026-05-15T10:00:00Z"))
                .build();

        mockMvc.perform(post("/events")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));
    }

    /**
     * Verifies that a balance of 750.00 is returned after posting two CREDIT
     * events of 500.00 and 250.00 for the same account.
     */
    @Test
    void getBalance_creditOnly_returnsPositiveBalance() throws Exception {
        postEvent("evt-c1", EventType.CREDIT, 500.00);
        postEvent("evt-c2", EventType.CREDIT, 250.00);

        mockMvc.perform(get("/accounts/{accountId}/balance", uniqueAccount))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accountId").value(uniqueAccount))
                .andExpect(jsonPath("$.balance").value(750.00))
                .andExpect(jsonPath("$.currency").value("USD"));
    }

    /**
     * Verifies that a negative balance of -150.00 is returned when only
     * DEBIT events are present.
     */
    @Test
    void getBalance_debitOnly_returnsNegativeBalance() throws Exception {
        postEvent("evt-d1", EventType.DEBIT, 100.00);
        postEvent("evt-d2", EventType.DEBIT, 50.00);

        mockMvc.perform(get("/accounts/{accountId}/balance", uniqueAccount))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(-150.00));
    }

    /**
     * Verifies the net balance calculation when both CREDIT and DEBIT events
     * are present: 300.00 + 50.00 − 100.00 = 250.00.
     */
    @Test
    void getBalance_creditAndDebit_returnsNetBalance() throws Exception {
        postEvent("evt-mix-c1", EventType.CREDIT, 300.00);
        postEvent("evt-mix-d1", EventType.DEBIT,  100.00);
        postEvent("evt-mix-c2", EventType.CREDIT, 50.00);

        mockMvc.perform(get("/accounts/{accountId}/balance", uniqueAccount))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(250.00));
    }

    /**
     * Verifies that requesting the balance for an account with no recorded
     * events returns HTTP 404 with a populated error body.
     */
    @Test
    void getBalance_unknownAccount_returns404WithErrorBody() throws Exception {
        mockMvc.perform(get("/accounts/{accountId}/balance", "acct-unknown-" + UUID.randomUUID()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").exists());
    }
}
