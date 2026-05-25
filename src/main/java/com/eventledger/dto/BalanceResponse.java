package com.eventledger.dto;

import lombok.*;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BalanceResponse {

    private String accountId;
    private BigDecimal balance;
    private String currency;
}
