package com.eventledger.dto;

import com.eventledger.enums.EventType;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EventResponse {

    private String eventId;
    private String accountId;
    private EventType type;
    private BigDecimal amount;
    private String currency;
    private Instant eventTimestamp;
    private Map<String, Object> metadata;
    private Instant createdAt;
}
