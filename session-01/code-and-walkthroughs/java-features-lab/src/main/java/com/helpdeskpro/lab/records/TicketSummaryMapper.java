package com.helpdeskpro.lab.records;

import java.util.List;

/**
 * Maps entity-style rows to summaries, the way a service maps entities to
 * response DTOs before returning them from a controller.
 */
public final class TicketSummaryMapper {

    private TicketSummaryMapper() {
    }

    // ---------------------------------------------------------------------
    // TODO 1.2: map a list of rows to a list of summaries
    //
    // Convert every TicketRow with TicketSummary.from(...).
    //
    //  - keep the original order
    //  - the returned list must be unmodifiable
    //  - an empty input gives an empty result
    //
    // Hints: List.stream(), Stream.map(...), Stream.toList()
    // ---------------------------------------------------------------------
    public static List<TicketSummary> toSummaries(List<TicketRow> rows) {
        throw new UnsupportedOperationException("TODO 1.2: implement toSummaries");
    }
}
