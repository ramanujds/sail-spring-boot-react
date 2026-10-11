package com.example.testapp;

import java.time.LocalDateTime;

public record MessageResponse(Long id, String text, LocalDateTime createdAt) {

    static MessageResponse from(Message message) {
        return new MessageResponse(message.getId(), message.getText(), message.getCreatedAt());
    }
}
