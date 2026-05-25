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
import java.util.Map;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class EventControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String uniqueAccount;

    @BeforeEach
    void setUp() {
        uniqueAccount = "acct-" + UUID.randomUUID();
    }

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

    private void postEvent(EventRequest request) throws Exception {
        mockMvc.perform(post("/events")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));
    }

    // ── POST /events ──────────────────────────────────────────────────────────

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

    @Test
    void postEvent_zeroAmount_returns400() throws Exception {
        EventRequest request = buildRequest("evt-zero", uniqueAccount, EventType.CREDIT, 0.00, "2026-05-15T10:00:00Z");

        mockMvc.perform(post("/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("amount")));
    }

    @Test
    void postEvent_negativeAmount_returns400() throws Exception {
        EventRequest request = buildRequest("evt-neg", uniqueAccount, EventType.CREDIT, -50.00, "2026-05-15T10:00:00Z");

        mockMvc.perform(post("/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("amount")));
    }

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

    @Test
    void postEvent_malformedJson_returns400() throws Exception {
        mockMvc.perform(post("/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{invalid json"))
                .andExpect(status().isBadRequest());
    }

    // ── GET /events/{id} ──────────────────────────────────────────────────────

    @Test
    void getEvent_existingId_returns200WithBody() throws Exception {
        EventRequest request = buildRequest("evt-get-1", uniqueAccount, EventType.CREDIT, 200.00, "2026-05-15T10:00:00Z");
        postEvent(request);

        mockMvc.perform(get("/events/evt-get-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.eventId").value("evt-get-1"))
                .andExpect(jsonPath("$.amount").value(200.00));
    }

    @Test
    void getEvent_nonExistentId_returns404() throws Exception {
        mockMvc.perform(get("/events/evt-does-not-exist"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    // ── GET /events?account={accountId} ──────────────────────────────────────

    @Test
    void getEventsByAccount_existingAccount_returns200WithList() throws Exception {
        postEvent(buildRequest("evt-list-1", uniqueAccount, EventType.CREDIT, 100.00, "2026-05-15T10:00:00Z"));
        postEvent(buildRequest("evt-list-2", uniqueAccount, EventType.DEBIT, 30.00, "2026-05-15T11:00:00Z"));

        mockMvc.perform(get("/events").param("account", uniqueAccount))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));
    }

    @Test
    void getEventsByAccount_outOfOrderSubmission_returnsChronologicalOrder() throws Exception {
        // Submit in reverse chronological order
        postEvent(buildRequest("evt-ooo-c", uniqueAccount, EventType.CREDIT, 100.00, "2026-05-15T12:00:00Z"));
        postEvent(buildRequest("evt-ooo-a", uniqueAccount, EventType.DEBIT,  50.00, "2026-05-15T08:00:00Z"));
        postEvent(buildRequest("evt-ooo-b", uniqueAccount, EventType.CREDIT, 200.00, "2026-05-15T10:00:00Z"));

        mockMvc.perform(get("/events").param("account", uniqueAccount))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)))
                // Chronological order: 08:00 → 10:00 → 12:00
                .andExpect(jsonPath("$[0].eventId").value("evt-ooo-a"))
                .andExpect(jsonPath("$[1].eventId").value("evt-ooo-b"))
                .andExpect(jsonPath("$[2].eventId").value("evt-ooo-c"));
    }

    @Test
    void getEventsByAccount_nonExistentAccount_returns404() throws Exception {
        mockMvc.perform(get("/events").param("account", "acct-unknown-" + UUID.randomUUID()))
                .andExpect(status().isNotFound());
    }
}
