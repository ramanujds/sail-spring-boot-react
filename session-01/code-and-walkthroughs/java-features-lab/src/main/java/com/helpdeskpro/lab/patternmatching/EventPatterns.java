package com.helpdeskpro.lab.patternmatching;

import com.helpdeskpro.lab.sealedtypes.TicketEvent;
import java.util.Optional;

/**
 * Pattern matching over the sealed {@link TicketEvent} hierarchy.
 */
public final class EventPatterns {

    private EventPatterns() {
    }

    // ---------------------------------------------------------------------
    // TODO 2.1: read a component with pattern matching for instanceof
    //
    // Return the agent when the event is an Assigned event, and an empty
    // Optional for every other kind of event.
    //
    // Rule: use `instanceof` with a pattern variable. Do not cast by hand
    // and do not call getClass().
    //
    // Hints: instanceof TicketEvent.Assigned assigned, Optional.of,
    //        Optional.empty
    // ---------------------------------------------------------------------
    public static Optional<String> assigneeOf(TicketEvent event) {
        throw new UnsupportedOperationException("TODO 2.1: implement assigneeOf");
    }

    // ---------------------------------------------------------------------
    // TODO 2.2: describe an event with a pattern-matching switch
    //
    // Return a one-line description:
    //
    //   Opened                          -> "Ticket <id> opened: <title>"
    //   Assigned                        -> "Ticket <id> assigned to <agent>"
    //   Resolved, non-blank resolution  -> "Ticket <id> resolved: <resolution>"
    //   Resolved, blank resolution      -> "Ticket <id> resolved (no resolution text)"
    //   Closed                          -> "Ticket <id> closed"
    //
    // Rules:
    //  - use a switch expression over the sealed type
    //  - do NOT add a default branch; the compiler must be the one that
    //    checks every TicketEvent subtype is handled
    //  - take the record components apart in the case label
    //
    // Hints: record patterns, a `when` guard, String.isBlank()
    // ---------------------------------------------------------------------
    public static String describe(TicketEvent event) {
        throw new UnsupportedOperationException("TODO 2.2: implement describe");
    }
}
