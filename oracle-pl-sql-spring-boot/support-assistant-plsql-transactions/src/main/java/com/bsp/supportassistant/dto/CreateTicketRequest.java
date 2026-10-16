package com.bsp.supportassistant.dto;

// The body of POST /api/tickets.
public record CreateTicketRequest(String title, String description, String priority) {
}
