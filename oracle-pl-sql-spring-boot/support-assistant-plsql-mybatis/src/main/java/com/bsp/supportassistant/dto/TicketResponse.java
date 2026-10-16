package com.bsp.supportassistant.dto;

import java.time.LocalDateTime;

// One row of the result set that FIND_TICKETS opens. It is a plain record, not a JPA entity.
public record TicketResponse(
        long id,
        String title,
        String description,
        String status,
        String priority,
        LocalDateTime createdAt,
        LocalDateTime dueBy) {
}
