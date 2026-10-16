package com.bsp.supportassistant.dto;

import java.util.List;

// A parent and its children, as one JSON object: {"ticket": {...}, "comments": [...]}.
public record TicketWithComments(TicketResponse ticket, List<CommentResponse> comments) {
}
