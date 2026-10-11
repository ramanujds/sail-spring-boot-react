package com.helpdeskpro.lab.records;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.helpdeskpro.lab.domain.TicketStatus;
import java.util.List;
import org.junit.jupiter.api.Test;

class TicketSummaryMapperTest {

    private static List<TicketRow> rows() {
        return List.of(
                new TicketRow(1, "Printer jam", TicketStatus.OPEN, null),
                new TicketRow(2, "VPN down", TicketStatus.ASSIGNED, "agent-1"));
    }

    // TODO 1.2
    @Test
    void mapsEveryRowInOrder() {
        List<TicketSummary> result = TicketSummaryMapper.toSummaries(rows());

        assertEquals(List.of(
                new TicketSummary(1, "Printer jam", TicketStatus.OPEN),
                new TicketSummary(2, "VPN down", TicketStatus.ASSIGNED)), result);
    }

    // TODO 1.2
    @Test
    void returnsAnUnmodifiableList() {
        List<TicketSummary> result = TicketSummaryMapper.toSummaries(rows());

        assertThrows(UnsupportedOperationException.class,
                () -> result.add(new TicketSummary(3, "Another", TicketStatus.OPEN)));
    }

    // TODO 1.2
    @Test
    void emptyInputGivesEmptyResult() {
        assertTrue(TicketSummaryMapper.toSummaries(List.of()).isEmpty());
    }
}
