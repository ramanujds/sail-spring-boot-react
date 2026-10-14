package com.bsp.supportassistant.dto;

import java.time.LocalDateTime;

public record WatcherResponse(
        String username,
        LocalDateTime watchingSince) {
}
