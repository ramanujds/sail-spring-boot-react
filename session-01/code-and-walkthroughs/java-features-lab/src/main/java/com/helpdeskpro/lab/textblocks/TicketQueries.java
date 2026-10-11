package com.helpdeskpro.lab.textblocks;

/**
 * Text blocks for multi-line JPQL and SQL.
 */
public final class TicketQueries {

    private TicketQueries() {
    }

    // Provided: the Java 8 style version, for comparison with TODO 4.1.
    public static String byStatusAndPriorityConcatenated() {
        return "SELECT t FROM Ticket t " +
               "WHERE t.status = :status " +
               "AND t.priority = :priority " +
               "ORDER BY t.dueBy";
    }

    // ---------------------------------------------------------------------
    // TODO 4.1: write the same JPQL query as a text block
    //
    // The query must read as these four lines, with the AND line indented by
    // two spaces and no indentation on the others:
    //
    //   SELECT t FROM Ticket t
    //   WHERE t.status = :status
    //     AND t.priority = :priority
    //   ORDER BY t.dueBy
    //
    // Rule: use a text block (""" ... """), not string concatenation. The
    // position of the closing delimiter decides how much indentation is
    // stripped, so check it against your source indentation.
    // ---------------------------------------------------------------------
    public static String byStatusAndPriority() {
        throw new UnsupportedOperationException("TODO 4.1: implement byStatusAndPriority");
    }
}
