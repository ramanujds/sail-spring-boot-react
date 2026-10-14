package com.bsp.supportassistant.dto;

import java.util.List;

// One page of results. T is the type of the items: TicketResponse here.
public record PageResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages) {
}
