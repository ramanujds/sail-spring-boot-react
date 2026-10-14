package com.bsp.supportassistant.service;

import com.bsp.supportassistant.model.Priority;
import com.bsp.supportassistant.model.Ticket;
import com.bsp.supportassistant.model.TicketStatus;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SlaPolicyTest {

    private final LocalDateTime created = LocalDateTime.of(2026, 10, 14, 9, 0);

    @Test
    void highPriorityIsDueInOneDay() {
        LocalDateTime dueBy = SlaPolicy.dueBy(Priority.HIGH, created);

        assertEquals(LocalDateTime.of(2026, 10, 15, 9, 0), dueBy);
    }

    @Test
    void mediumPriorityIsDueInThreeDays() {
        assertEquals(created.plusDays(3), SlaPolicy.dueBy(Priority.MEDIUM, created));
    }

    @Test
    void lowPriorityIsDueInSevenDays() {
        assertEquals(created.plusDays(7), SlaPolicy.dueBy(Priority.LOW, created));
    }

    @Test
    void openTicketPastItsDueDateIsOverdue() {
        Ticket ticket = ticket(TicketStatus.OPEN, created.plusDays(1));

        assertTrue(SlaPolicy.isOverdue(ticket, created.plusDays(2)));
    }

    @Test
    void openTicketBeforeItsDueDateIsNotOverdue() {
        Ticket ticket = ticket(TicketStatus.OPEN, created.plusDays(1));

        assertFalse(SlaPolicy.isOverdue(ticket, created.plusHours(5)));
    }

    @Test
    void closedTicketIsNeverOverdue() {
        Ticket ticket = ticket(TicketStatus.CLOSED, created.plusDays(1));

        assertFalse(SlaPolicy.isOverdue(ticket, created.plusDays(30)));
    }

    private Ticket ticket(TicketStatus status, LocalDateTime dueBy) {
        Ticket ticket = new Ticket();
        ticket.setStatus(status);
        ticket.setDueBy(dueBy);
        return ticket;
    }
}
