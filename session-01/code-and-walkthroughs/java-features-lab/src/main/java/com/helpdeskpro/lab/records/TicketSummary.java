package com.helpdeskpro.lab.records;

import com.helpdeskpro.lab.domain.TicketStatus;

/**
 * A read-only view of a ticket that is safe to hand to a caller.
 *
 * The components are declared for you. The validation rules are TODO 1.1.
 */
public record TicketSummary(long id, String title, TicketStatus status) {

    // ---------------------------------------------------------------------
    // TODO 1.1: validate in a compact constructor
    //
    // Add a compact constructor (a constructor with no parameter list) that
    // rejects invalid values:
    //
    //   title is null           -> NullPointerException
    //   status is null          -> NullPointerException
    //   id is zero or negative  -> IllegalArgumentException
    //   title is blank          -> IllegalArgumentException
    //
    // Do not assign the fields yourself; a compact constructor assigns them
    // for you after your checks.
    //
    // Hints: Objects.requireNonNull, String.isBlank()
    // ---------------------------------------------------------------------

    // Provided: build a summary from an entity-style row.
    public static TicketSummary from(TicketRow row) {
        return new TicketSummary(row.getId(), row.getTitle(), row.getStatus());
    }
}
