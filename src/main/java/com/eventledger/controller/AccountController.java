package com.eventledger.controller;

import com.eventledger.dto.BalanceResponse;
import com.eventledger.service.EventService;
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
    @GetMapping("/{accountId}/balance")
    public ResponseEntity<BalanceResponse> getBalance(@PathVariable String accountId) {
        return ResponseEntity.ok(eventService.getBalanceByAccount(accountId));
    }
}
