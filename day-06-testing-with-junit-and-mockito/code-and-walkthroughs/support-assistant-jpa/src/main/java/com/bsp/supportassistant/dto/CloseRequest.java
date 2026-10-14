package com.bsp.supportassistant.dto;

// Who closes the ticket, and the note that is added to it as a comment.
public record CloseRequest(
        String agent,
        String note) {
}
