package com.bsp.supportassistant.exception;

import java.util.UUID;

// Thrown when the person already watches the ticket.
public class AlreadyWatchingException extends RuntimeException {

    public AlreadyWatchingException(UUID ticketId, String username) {
        super(username + " already watches ticket " + ticketId);
    }
}
