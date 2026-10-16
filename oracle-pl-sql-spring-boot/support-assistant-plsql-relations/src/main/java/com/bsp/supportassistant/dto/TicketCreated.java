package com.bsp.supportassistant.dto;

// The answer to a request that creates a ticket: the id that the PL/SQL procedure gave back.
public record TicketCreated(long id) {
}
