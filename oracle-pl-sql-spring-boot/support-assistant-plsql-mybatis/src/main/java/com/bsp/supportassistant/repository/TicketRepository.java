package com.bsp.supportassistant.repository;

import com.bsp.supportassistant.dto.TicketResponse;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

// Gives the service the same four methods as the other projects. TicketMapper does the calls.
// A Map carries the values in, and the OUT values back.
@Repository
public class TicketRepository {

    private final TicketMapper ticketMapper;

    public TicketRepository(TicketMapper ticketMapper) {
        this.ticketMapper = ticketMapper;
    }

    public int openCount(String priority) {
        return ticketMapper.openCount(priority);
    }

    public long createTicket(String title, String description, String priority) {
        Map<String, Object> params = new HashMap<>();
        params.put("title", title);
        params.put("description", description);
        params.put("priority", priority);
        ticketMapper.createTicket(params);
        return ((Number) params.get("ticketId")).longValue();
    }

    public void changeStatus(long ticketId, String newStatus) {
        ticketMapper.changeStatus(ticketId, newStatus);
    }

    @SuppressWarnings("unchecked")
    public List<TicketResponse> findTickets(String status) {
        Map<String, Object> params = new HashMap<>();
        params.put("status", status);
        ticketMapper.findTickets(params);
        return (List<TicketResponse>) params.get("tickets");
    }
}
