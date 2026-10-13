package com.bsp.supportassistant.repository;

import com.bsp.supportassistant.model.Ticket;
import com.bsp.supportassistant.model.TicketStatus;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;

@Repository
public class TicketRepository {

    // The list is the storage. The tickets are lost when the application stops.
    private final List<Ticket> tickets = new ArrayList<>();
    private long nextId = 1;

    public List<Ticket> findAll() {
        return tickets;
    }

    public List<Ticket> findAllByStatus(TicketStatus status) {
        List<Ticket> result = new ArrayList<>();
        for (Ticket ticket : tickets) {
            if (ticket.getStatus() == status) {
                result.add(ticket);
            }
        }
        return result;
    }

    // Returns the ticket, or null when there is no ticket with this id.
    public Ticket findById(Long id) {
        for (Ticket ticket : tickets) {
            if (ticket.getId().equals(id)) {
                return ticket;
            }
        }
        return null;
    }

    public Ticket save(Ticket ticket) {
        ticket.setId(nextId++);
        tickets.add(ticket);
        return ticket;
    }

    // Changes only the fields that are present in the request.
    // Returns the ticket, or null when there is no ticket with this id.
    public Ticket update(Long id, Ticket changes) {
        Ticket ticket = findById(id);
        if (ticket == null) {
            return null;
        }
        if (changes.getTitle() != null) {
            ticket.setTitle(changes.getTitle());
        }
        if (changes.getDescription() != null) {
            ticket.setDescription(changes.getDescription());
        }
        if (changes.getStatus() != null) {
            ticket.setStatus(changes.getStatus());
        }
        if (changes.getPriority() != null) {
            ticket.setPriority(changes.getPriority());
        }
        return ticket;
    }

    public void deleteById(Long id) {
        tickets.removeIf(ticket -> ticket.getId().equals(id));
    }
}
