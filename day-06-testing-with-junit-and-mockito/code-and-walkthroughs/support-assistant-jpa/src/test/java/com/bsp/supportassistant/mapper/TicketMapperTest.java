package com.bsp.supportassistant.mapper;

import com.bsp.supportassistant.dto.TicketRequest;
import com.bsp.supportassistant.dto.TicketResponse;
import com.bsp.supportassistant.model.Priority;
import com.bsp.supportassistant.model.Ticket;
import com.bsp.supportassistant.model.TicketStatus;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TicketMapperTest {

    @Test
    void applyChangesCopiesOnlyTheFieldsThatWereSent() {
        Ticket ticket = new Ticket();
        ticket.setTitle("Printer is offline");
        ticket.setDescription("Floor 2");
        ticket.setPriority(Priority.LOW);

        TicketRequest request = new TicketRequest(null, null, null, Priority.HIGH);
        TicketMapper.applyChanges(ticket, request);

        assertEquals(Priority.HIGH, ticket.getPriority());        // sent, so changed
        assertEquals("Printer is offline", ticket.getTitle());     // not sent, so kept
        assertEquals("Floor 2", ticket.getDescription());
    }

    @Test
    void toResponseShowsAnUnfinishedTicketPastItsDueDateAsOverdue() {
        Ticket ticket = new Ticket();
        ticket.setId(UUID.randomUUID());
        ticket.setTitle("VPN drops");
        ticket.setStatus(TicketStatus.OPEN);
        ticket.setPriority(Priority.MEDIUM);
        ticket.setDueBy(LocalDateTime.now().minusHours(1));

        TicketResponse response = TicketMapper.toResponse(ticket);

        assertEquals("VPN drops", response.title());
        assertTrue(response.overdue());
    }

    @Test
    void toResponseShowsAClosedTicketAsNotOverdue() {
        Ticket ticket = new Ticket();
        ticket.setStatus(TicketStatus.CLOSED);
        ticket.setDueBy(LocalDateTime.now().minusDays(5));

        assertFalse(TicketMapper.toResponse(ticket).overdue());
    }
}
