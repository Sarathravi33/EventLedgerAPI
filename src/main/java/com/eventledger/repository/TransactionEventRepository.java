package com.eventledger.repository;

import com.eventledger.entity.TransactionEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for {@link TransactionEvent} entities.
 *
 * <p>Provides standard CRUD operations inherited from {@link JpaRepository}
 * plus custom queries for the event-ledger domain: idempotency lookup,
 * account history retrieval, and balance calculation.
 *
 * @author Sarathkumar Ravi
 */
@Repository
public interface TransactionEventRepository extends JpaRepository<TransactionEvent, Long> {

    /**
     * Looks up a transaction event by its unique event identifier.
     *
     * <p>Used by the idempotency check: if a record is present, the event
     * has already been processed and no new record should be created.
     *
     * @param eventId the client-supplied event identifier
     * @return an {@link Optional} containing the event, or empty if not found
     */
    Optional<TransactionEvent> findByEventId(String eventId);

    /**
     * Returns all transaction events for the specified account, sorted by
     * event timestamp in ascending (chronological) order.
     *
     * @param accountId the account whose events are to be retrieved
     * @return an ordered list of matching events; empty if none exist
     */
    List<TransactionEvent> findByAccountIdOrderByEventTimestampAsc(String accountId);

    /**
     * Checks whether any transaction events have been recorded for the
     * given account.
     *
     * <p>Used as a pre-condition guard to distinguish "account not found"
     * (no events) from a genuine zero balance.
     *
     * @param accountId the account identifier to check
     * @return {@code true} if at least one event exists for the account
     */
    boolean existsByAccountId(String accountId);

    /**
     * Calculates the net balance for an account as the sum of all CREDIT
     * amounts minus the sum of all DEBIT amounts.
     *
     * <p>{@code COALESCE} guards against {@code NULL} when there are no
     * events of a particular type, ensuring the result is always a numeric
     * value rather than {@code NULL}.
     *
     * @param accountId the account whose balance is to be calculated
     * @return the net balance; never {@code null}
     */
    @Query("SELECT COALESCE(SUM(CASE WHEN e.type = 'CREDIT' THEN e.amount ELSE 0 END), 0) - " +
           "COALESCE(SUM(CASE WHEN e.type = 'DEBIT' THEN e.amount ELSE 0 END), 0) " +
           "FROM TransactionEvent e WHERE e.accountId = :accountId")
    BigDecimal calculateBalanceByAccountId(@Param("accountId") String accountId);
}
