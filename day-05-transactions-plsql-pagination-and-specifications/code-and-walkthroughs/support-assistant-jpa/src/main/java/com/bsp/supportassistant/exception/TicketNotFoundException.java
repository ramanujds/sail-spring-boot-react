package com.bsp.supportassistant.exception;

import java.util.UUID;

// Thrown when no ticket has the requested id.
public class TicketNotFoundException extends RuntimeException {

    public TicketNotFoundException(UUID id) {
        super("Ticket " + id + " was not found");
    }
}
