package com.bsp.supportassistant.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record CommentResponse(
        UUID id,
        String author,
        String text,
        LocalDateTime createdAt) {
}
