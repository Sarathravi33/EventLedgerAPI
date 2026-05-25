package com.eventledger.controller;

import com.eventledger.dto.EventRequest;
import com.eventledger.dto.EventResponse;
import com.eventledger.exception.ErrorResponse;
import com.eventledger.service.EventService;
import com.eventledger.service.EventService.SubmitResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Events", description = "Submit and query transaction events")
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
    @Operation(
            summary = "Submit a transaction event",
            description = "Creates a new CREDIT or DEBIT event. Re-submitting the same eventId " +
                          "returns the original record with 200 OK instead of creating a duplicate."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Event created",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = EventResponse.class))),
            @ApiResponse(responseCode = "200", description = "Duplicate submission — original event returned",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = EventResponse.class))),
            @ApiResponse(responseCode = "400", description = "Validation failure",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class)))
    })
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
    @Operation(summary = "Retrieve a single event by ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Event found",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = EventResponse.class))),
            @ApiResponse(responseCode = "404", description = "Event not found",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/{id}")
    public ResponseEntity<EventResponse> getEvent(
            @Parameter(description = "Unique event identifier", example = "evt-001")
            @PathVariable String id) {
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
    @Operation(
            summary = "List events for an account",
            description = "Returns a paginated list of events ordered by eventTimestamp ascending. " +
                          "Response is a Spring Page object containing content, totalElements, totalPages, etc."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Page of events returned"),
            @ApiResponse(responseCode = "400", description = "Invalid pagination parameters",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Account not found",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping
    public ResponseEntity<Page<EventResponse>> getEventsByAccount(
            @Parameter(description = "Account identifier", required = true, example = "acct-123")
            @RequestParam("account") String accountId,
            @Parameter(description = "Zero-based page index", example = "0")
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @Parameter(description = "Number of events per page (min 1)", example = "20")
            @RequestParam(defaultValue = "20") @Min(1) int size) {
        return ResponseEntity.ok(eventService.getEventsByAccount(accountId, page, size));
    }
}
