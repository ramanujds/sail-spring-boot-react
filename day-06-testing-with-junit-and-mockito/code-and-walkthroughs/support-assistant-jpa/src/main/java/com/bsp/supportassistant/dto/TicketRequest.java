package com.bsp.supportassistant.dto;

import com.bsp.supportassistant.model.Priority;
import com.bsp.supportassistant.model.TicketStatus;

// The data a client may send. There is no id: the server decides it.
public record TicketRequest(
        String title,
        String description,
        TicketStatus status,
        Priority priority) {
}
