package com.helpdeskpro.lab.textblocks;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.Test;

class TicketQueriesTest {

    // TODO 4.1
    @Test
    void textBlockMatchesTheConcatenatedQueryOnceWhitespaceIsNormalised() {
        String normalisedBlock = String.join(" ",
                TicketQueries.byStatusAndPriority().lines().map(String::strip).toList());

        assertEquals(TicketQueries.byStatusAndPriorityConcatenated(), normalisedBlock);
    }

    // TODO 4.1
    @Test
    void textBlockKeepsTheIntendedLineStructureAndIndentation() {
        List<String> lines = TicketQueries.byStatusAndPriority().lines().toList();

        assertEquals(List.of(
                "SELECT t FROM Ticket t",
                "WHERE t.status = :status",
                "  AND t.priority = :priority",
                "ORDER BY t.dueBy"), lines);
    }
}
