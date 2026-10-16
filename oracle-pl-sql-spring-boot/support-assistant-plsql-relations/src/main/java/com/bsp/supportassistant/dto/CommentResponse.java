package com.bsp.supportassistant.dto;

import java.time.LocalDateTime;

// One row of a comment cursor. The ticket it belongs to is the record around it, not a field here.
public record CommentResponse(
        long id,
        String author,
        String body,
        LocalDateTime createdAt) {
}
