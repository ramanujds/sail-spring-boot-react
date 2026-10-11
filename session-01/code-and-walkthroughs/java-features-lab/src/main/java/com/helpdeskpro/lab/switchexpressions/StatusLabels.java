package com.helpdeskpro.lab.switchexpressions;

import com.helpdeskpro.lab.domain.TicketStatus;

/**
 * Switch expressions over the ticket status.
 */
public final class StatusLabels {

    private StatusLabels() {
    }

    // Provided: the Java 8 style version, for comparison with TODO 3.1.
    public static String labelJava8Style(TicketStatus status) {
        String label;
        switch (status) {
            case OPEN:
            case REOPENED:
                label = "Waiting";
                break;
            case ASSIGNED:
            case IN_PROGRESS:
                label = "In progress";
                break;
            case RESOLVED:
                label = "Resolved";
                break;
            case CLOSED:
                label = "Closed";
                break;
            default:
                throw new IllegalArgumentException("Unknown status: " + status);
        }
        return label;
    }

    // ---------------------------------------------------------------------
    // TODO 3.1: the same mapping as a switch expression
    //
    //   OPEN, REOPENED          -> "Waiting"
    //   ASSIGNED, IN_PROGRESS   -> "In progress"
    //   RESOLVED                -> "Resolved"
    //   CLOSED                  -> "Closed"
    //
    // Rules:
    //  - one switch expression, arrow labels, several constants per label
    //  - no default branch. Because TicketStatus is an enum, the compiler
    //    reports an error if a constant is missing.
    // ---------------------------------------------------------------------
    public static String label(TicketStatus status) {
        throw new UnsupportedOperationException("TODO 3.1: implement label");
    }

    // ---------------------------------------------------------------------
    // TODO 3.2: a switch branch that needs a block and `yield`
    //
    // Describe the SLA position of a ticket:
    //
    //   ASSIGNED or IN_PROGRESS, hoursLeft < 0   -> "Overdue by <n> hours"
    //                                               (n is the positive number)
    //   ASSIGNED or IN_PROGRESS, hoursLeft >= 0  -> "<n> hours left"
    //   any other status                         -> "No SLA clock running"
    //
    // Example: (IN_PROGRESS, -3) -> "Overdue by 3 hours"
    //
    // Rule: use a switch expression, and put the overdue/left decision in a
    // block that ends with `yield`.
    // ---------------------------------------------------------------------
    public static String slaMessage(TicketStatus status, int hoursLeft) {
        throw new UnsupportedOperationException("TODO 3.2: implement slaMessage");
    }
}
