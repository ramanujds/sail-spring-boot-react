package com.helpdeskpro.lab.domain;

/**
 * The ticket lifecycle states used throughout the programme.
 *
 * Lifecycle: OPEN -> ASSIGNED -> IN_PROGRESS -> RESOLVED -> CLOSED,
 * with REOPENED branching off CLOSED and looping back to ASSIGNED.
 */
public enum TicketStatus {
    OPEN,
    ASSIGNED,
    IN_PROGRESS,
    RESOLVED,
    CLOSED,
    REOPENED
}
