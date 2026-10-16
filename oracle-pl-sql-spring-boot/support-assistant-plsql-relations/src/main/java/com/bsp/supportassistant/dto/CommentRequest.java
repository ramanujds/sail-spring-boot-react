package com.bsp.supportassistant.dto;

// The body of POST /api/tickets/7/comments. The ticket id is in the URL, so it is not here.
public record CommentRequest(String author, String body) {
}
