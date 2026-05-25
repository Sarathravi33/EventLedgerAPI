package com.eventledger.dto;

import com.eventledger.enums.EventType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EventRequest {

    @NotBlank(message = "eventId must not be blank")
    private String eventId;

    @NotBlank(message = "accountId must not be blank")
    private String accountId;

    @NotNull(message = "type must not be null")
    private EventType type;

    @NotNull(message = "amount must not be null")
    @Positive(message = "amount must be greater than 0")
    private BigDecimal amount;

    @NotBlank(message = "currency must not be blank")
    private String currency;

    @NotNull(message = "eventTimestamp must not be null")
    private Instant eventTimestamp;

    private Map<String, Object> metadata;
}
