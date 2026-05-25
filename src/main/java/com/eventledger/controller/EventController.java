package com.eventledger.controller;

import com.eventledger.dto.EventRequest;
import com.eventledger.dto.EventResponse;
import com.eventledger.service.EventService;
import com.eventledger.service.EventService.SubmitResult;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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
     * Lists all transaction events belonging to the specified account,
     * ordered by event timestamp ascending.
     *
     * @param accountId the account whose events are to be retrieved
     * @return {@code 200 OK} with the list of events, or
     *         {@code 404 Not Found} if the account has no recorded events
     */
    @GetMapping
    public ResponseEntity<List<EventResponse>> getEventsByAccount(
            @RequestParam("account") String accountId) {
        return ResponseEntity.ok(eventService.getEventsByAccount(accountId));
    }
}
