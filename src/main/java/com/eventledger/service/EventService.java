package com.eventledger.service;

import com.eventledger.dto.BalanceResponse;
import com.eventledger.dto.EventRequest;
import com.eventledger.dto.EventResponse;
import org.springframework.data.domain.Page;

/**
 * Service interface defining the business operations for the Event Ledger.
 *
 * <p>Implementations are responsible for:
 * <ul>
 *   <li>Idempotent event submission</li>
 *   <li>Retrieval of individual events by ID</li>
 *   <li>Retrieval of all events for an account in chronological order</li>
 *   <li>Net balance calculation for an account</li>
 * </ul>
 *
 * @author Sarathkumar Ravi
 */
public interface EventService {

    /**
     * Submits a transaction event, enforcing idempotency on {@code eventId}.
     *
     * <p>If an event with the same {@code eventId} already exists in the
     * store, the existing record is returned and {@code created} is
     * {@code false}. Otherwise the event is persisted and {@code created}
     * is {@code true}.
     *
     * @param request the event payload to submit
     * @return a {@link SubmitResult} containing the event response and a
     *         flag indicating whether a new record was created
     */
    SubmitResult submitEvent(EventRequest request);

    /**
     * Retrieves a single transaction event by its unique event identifier.
     *
     * @param eventId the event identifier to look up
     * @return the matching {@link EventResponse}
     * @throws com.eventledger.exception.EventNotFoundException if no event
     *         with the given ID exists
     */
    EventResponse getEventById(String eventId);

    /**
     * Returns a page of transaction events for the specified account, ordered
     * by event timestamp ascending.
     *
     * @param accountId the account whose events are to be retrieved
     * @param page      zero-based page index
     * @param size      number of events per page (must be &gt; 0)
     * @return a {@link Page} of {@link EventResponse} objects
     * @throws com.eventledger.exception.AccountNotFoundException if the account
     *         has no recorded events
     */
    Page<EventResponse> getEventsByAccount(String accountId, int page, int size);

    /**
     * Computes the current net balance for the specified account.
     *
     * <p>Balance = sum(CREDIT amounts) − sum(DEBIT amounts).
     *
     * @param accountId the account whose balance is to be calculated
     * @return a {@link BalanceResponse} containing the account ID, net balance,
     *         and currency
     * @throws com.eventledger.exception.AccountNotFoundException if the account
     *         has no recorded events
     */
    BalanceResponse getBalanceByAccount(String accountId);

    /**
     * Carries the result of an idempotent event submission.
     *
     * @param response the event data (new or pre-existing)
     * @param created  {@code true} if the event was newly created,
     *                 {@code false} if a duplicate was detected
     */
    record SubmitResult(EventResponse response, boolean created) {}
}
