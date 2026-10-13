package com.bsp.supportassistant.repository;

import com.bsp.supportassistant.model.Ticket;
import com.bsp.supportassistant.model.TicketStatus;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class TicketRepository {

    // The list is the storage. The tickets are lost when the application stops.
    private final List<Ticket> tickets = new ArrayList<>();

    public List<Ticket> findAll() {
        return tickets;
    }

    public List<Ticket> findAllByStatus(TicketStatus status) {
        return tickets.stream()
                .filter(ticket -> ticket.getStatus() == status)
                .toList();
    }

    // The Optional holds the ticket, or holds nothing when the id does not exist.
    public Optional<Ticket> findById(UUID id) {
        return tickets.stream()
                .filter(ticket -> ticket.getId().equals(id))
                .findFirst();
    }

    public Ticket save(Ticket ticket) {
        ticket.setId(UUID.randomUUID());
        tickets.add(ticket);
        return ticket;
    }

    // Changes only the fields that are present in the request.
    public Optional<Ticket> update(UUID id, Ticket changes) {
        Optional<Ticket> found = findById(id);
        found.ifPresent(ticket -> applyChanges(ticket, changes));
        return found;
    }

    // Returns true when a ticket was removed, false when there was none with this id.
    public boolean deleteById(UUID id) {
        return tickets.removeIf(ticket -> ticket.getId().equals(id));
    }

    private void applyChanges(Ticket ticket, Ticket changes) {
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
    }
}
