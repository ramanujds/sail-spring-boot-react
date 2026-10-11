package com.helpdeskpro.lab.switchexpressions;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.helpdeskpro.lab.domain.TicketStatus;
import org.junit.jupiter.api.Test;

class StatusLabelsTest {

    // TODO 3.1
    @Test
    void labelMatchesTheJava8StyleVersionForEveryStatus() {
        for (TicketStatus status : TicketStatus.values()) {
            assertEquals(StatusLabels.labelJava8Style(status), StatusLabels.label(status),
                    "label for " + status);
        }
    }

    // TODO 3.2
    @Test
    void overdueTicketReportsPositiveHours() {
        assertEquals("Overdue by 3 hours", StatusLabels.slaMessage(TicketStatus.IN_PROGRESS, -3));
    }

    // TODO 3.2
    @Test
    void ticketWithTimeLeftReportsHoursLeft() {
        assertEquals("5 hours left", StatusLabels.slaMessage(TicketStatus.ASSIGNED, 5));
        assertEquals("0 hours left", StatusLabels.slaMessage(TicketStatus.IN_PROGRESS, 0));
    }

    // TODO 3.2
    @Test
    void statusesWithoutAClockReportNoSla() {
        assertEquals("No SLA clock running", StatusLabels.slaMessage(TicketStatus.OPEN, 10));
        assertEquals("No SLA clock running", StatusLabels.slaMessage(TicketStatus.CLOSED, -4));
    }
}
