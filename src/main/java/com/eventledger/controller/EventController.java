package com.eventledger.controller;

import com.eventledger.dto.EventRequest;
import com.eventledger.dto.EventResponse;
import com.eventledger.service.EventService;
import com.eventledger.service.EventService.SubmitResult;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller that handles CRUD operations for transaction events.
 *
 * <p>Base path: {@code /events}
 *
 * <p>Event submission is idempotent: re-submitting a previously seen
 * {@code eventId} returns the original record with HTTP 200 instead
 * of creating a duplicate.
 *
 * @author Sarathkumar Ravi
 */
@Validated
@RestController
@RequestMapping("/events")
@RequiredArgsConstructor
public class EventController {

    private final EventService eventService;

    /**
     * Submits a new transaction event.
     *
     * <p>If the {@code eventId} has not been seen before, the event is
     * persisted and {@code 201 Created} is returned. If the same
     * {@code eventId} is submitted again, the original event is returned
     * with {@code 200 OK} — no duplicate is created (idempotency guarantee).
     *
     * @param request validated event payload
     * @return {@code 201 Created} for a new event, or {@code 200 OK} for a
     *         duplicate submission
     */
    @PostMapping
    public ResponseEntity<EventResponse> submitEvent(@RequestBody @Valid EventRequest request) {
        SubmitResult result = eventService.submitEvent(request);
        HttpStatus status = result.created() ? HttpStatus.CREATED : HttpStatus.OK;
        return ResponseEntity.status(status).body(result.response());
    }

    /**
     * Retrieves a single transaction event by its event ID.
     *
     * @param id the unique event identifier
     * @return {@code 200 OK} with the matching {@link EventResponse}, or
     *         {@code 404 Not Found} if no event with the given ID exists
     */
    @GetMapping("/{id}")
    public ResponseEntity<EventResponse> getEvent(@PathVariable String id) {
        return ResponseEntity.ok(eventService.getEventById(id));
    }

    /**
     * Lists transaction events for the specified account, ordered by event
     * timestamp ascending, with cursor-based pagination.
     *
     * @param accountId the account whose events are to be retrieved
     * @param page      zero-based page index (default 0)
     * @param size      number of events per page, 1–200 (default 20)
     * @return {@code 200 OK} with a page of events, or
     *         {@code 404 Not Found} if the account has no recorded events
     */
    @GetMapping
    public ResponseEntity<Page<EventResponse>> getEventsByAccount(
            @RequestParam("account") String accountId,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) int size) {
        return ResponseEntity.ok(eventService.getEventsByAccount(accountId, page, size));
    }
}
