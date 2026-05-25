package com.eventledger.dto;

import lombok.*;

import java.math.BigDecimal;

/**
 * Response payload representing the current net balance of an account.
 *
 * <p>The {@code balance} is computed as total CREDITs minus total DEBITs
 * across all events recorded for the account. A negative value indicates
 * that debits exceed credits.
 *
 * @author Sarathkumar Ravi
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BalanceResponse {

    /** Unique identifier of the account. */
    private String accountId;

    /** Net balance (CREDITs − DEBITs) with up to four decimal places. */
    private BigDecimal balance;

    /** ISO 4217 currency code (e.g. {@code USD}). */
    private String currency;
}
