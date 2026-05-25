package com.eventledger.controller;

import com.eventledger.dto.BalanceResponse;
import com.eventledger.exception.ErrorResponse;
import com.eventledger.service.EventService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller that exposes account-level query endpoints.
 *
 * <p>Base path: {@code /accounts}
 *
 * @author Sarathkumar Ravi
 */
@Tag(name = "Accounts", description = "Account-level balance queries")
@RestController
@RequestMapping("/accounts")
@RequiredArgsConstructor
public class AccountController {

    private final EventService eventService;

    /**
     * Returns the current net balance for the given account.
     *
     * <p>Balance is computed as the sum of all CREDIT events minus the sum
     * of all DEBIT events recorded for the account.
     *
     * @param accountId the unique account identifier
     * @return {@code 200 OK} with a {@link BalanceResponse} body, or
     *         {@code 404 Not Found} if no events exist for the account
     */
    @Operation(
            summary = "Get account balance",
            description = "Computes the net balance as total CREDITs minus total DEBITs. " +
                          "A negative value indicates debits exceed credits."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Balance computed",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = BalanceResponse.class))),
            @ApiResponse(responseCode = "404", description = "Account not found",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/{accountId}/balance")
    public ResponseEntity<BalanceResponse> getBalance(
            @Parameter(description = "Unique account identifier", example = "acct-123")
            @PathVariable String accountId) {
        return ResponseEntity.ok(eventService.getBalanceByAccount(accountId));
    }
}
