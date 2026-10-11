package com.helpdeskpro.lab.sealedtypes;

/**
 * The closed set of things that can happen to a ticket.
 *
 * The interface is sealed, and every implementation lives in this file, so the
 * compiler knows the complete list of subtypes without a {@code permits} clause.
 */
public sealed interface TicketEvent {

    record Opened(long ticketId, String title) implements TicketEvent {}

    record Assigned(long ticketId, String agent) implements TicketEvent {}

    record Resolved(long ticketId, String resolution) implements TicketEvent {}

    record Closed(long ticketId) implements TicketEvent {}
}
