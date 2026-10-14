package com.bsp.supportassistant.model;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "TICKET_WATCHERS")
public class TicketWatcher {

    // There is no @GeneratedValue: the application supplies both parts of the key.
    @EmbeddedId
    private TicketWatcherId id;

    private LocalDateTime watchingSince;

    public TicketWatcherId getId() {
        return id;
    }

    public void setId(TicketWatcherId id) {
        this.id = id;
    }

    public LocalDateTime getWatchingSince() {
        return watchingSince;
    }

    public void setWatchingSince(LocalDateTime watchingSince) {
        this.watchingSince = watchingSince;
    }
}
