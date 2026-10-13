package com.bsp.supportassistant.dto;

import com.bsp.supportassistant.model.Priority;
import com.bsp.supportassistant.model.TicketStatus;

import java.util.UUID;

// The data a client receives. Only these fields leave the application.
public record TicketResponse(
        UUID id,
        String title,
        String description,
        TicketStatus status,
        Priority priority,
        int commentCount) {
}
