package com.bsp.supportassistant.dto;

// The body of POST /api/tickets/with-first-comment: a ticket and its first comment.
public record CreateTicketWithCommentRequest(
        String title,
        String description,
        String priority,
        String author,
        String comment) {
}
