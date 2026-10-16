package com.bsp.supportassistant.dto;

import java.util.List;

// One page of a list, and what the caller needs to ask for the next one.
// T stands for the type of the items, here TicketResponse.
public record PageResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages) {
}
