package com.eventledger.dto;

import io.swagger.v3.oas.annotations.media.Schema;
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
@Schema(description = "Response payload representing the current net balance of an account")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BalanceResponse {

    @Schema(description = "Unique identifier of the account", example = "acct-123")
    private String accountId;

    @Schema(description = "Net balance: total CREDITs minus total DEBITs. Negative means debits exceed credits.", example = "310.00")
    private BigDecimal balance;

    @Schema(description = "ISO 4217 currency code", example = "USD")
    private String currency;
}
