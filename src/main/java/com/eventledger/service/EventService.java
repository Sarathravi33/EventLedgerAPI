package com.eventledger.service;

import com.eventledger.dto.BalanceResponse;
import com.eventledger.dto.EventRequest;
import com.eventledger.dto.EventResponse;

import java.util.List;

public interface EventService {

    SubmitResult submitEvent(EventRequest request);

    EventResponse getEventById(String eventId);

    List<EventResponse> getEventsByAccount(String accountId);

    BalanceResponse getBalanceByAccount(String accountId);

    record SubmitResult(EventResponse response, boolean created) {}
}
