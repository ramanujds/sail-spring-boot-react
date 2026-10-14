package com.bsp.supportassistant.model;

import jakarta.persistence.Embeddable;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.io.Serializable;
import java.util.UUID;

// The two columns that make up the primary key of TICKET_WATCHERS.
// A record gives equals and hashCode, which a key class needs.
@Embeddable
public record TicketWatcherId(
        @JdbcTypeCode(SqlTypes.VARCHAR) UUID ticketId, // stored as text, like TICKETS.ID
        String username) implements Serializable {
}
