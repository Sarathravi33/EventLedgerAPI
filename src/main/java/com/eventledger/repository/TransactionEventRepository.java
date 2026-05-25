package com.eventledger.repository;

import com.eventledger.entity.TransactionEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface TransactionEventRepository extends JpaRepository<TransactionEvent, Long> {

    Optional<TransactionEvent> findByEventId(String eventId);

    List<TransactionEvent> findByAccountIdOrderByEventTimestampAsc(String accountId);

    boolean existsByAccountId(String accountId);

    @Query("SELECT COALESCE(SUM(CASE WHEN e.type = 'CREDIT' THEN e.amount ELSE 0 END), 0) - " +
           "COALESCE(SUM(CASE WHEN e.type = 'DEBIT' THEN e.amount ELSE 0 END), 0) " +
           "FROM TransactionEvent e WHERE e.accountId = :accountId")
    BigDecimal calculateBalanceByAccountId(@Param("accountId") String accountId);
}
