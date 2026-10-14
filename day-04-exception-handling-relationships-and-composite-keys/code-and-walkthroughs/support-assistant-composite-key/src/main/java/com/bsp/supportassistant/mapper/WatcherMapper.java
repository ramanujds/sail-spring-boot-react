package com.bsp.supportassistant.mapper;

import com.bsp.supportassistant.dto.WatcherResponse;
import com.bsp.supportassistant.model.TicketWatcher;
import com.bsp.supportassistant.model.TicketWatcherId;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public class WatcherMapper {

    public static TicketWatcher toEntity(UUID ticketId, String username) {
        TicketWatcher watcher = new TicketWatcher();
        watcher.setId(new TicketWatcherId(ticketId, username));
        watcher.setWatchingSince(LocalDateTime.now());
        return watcher;
    }

    public static WatcherResponse toResponse(TicketWatcher watcher) {
        return new WatcherResponse(watcher.getId().username(), watcher.getWatchingSince());
    }

    public static List<WatcherResponse> toResponseList(List<TicketWatcher> watchers) {
        return watchers.stream()
                .map(WatcherMapper::toResponse)
                .toList();
    }
}
