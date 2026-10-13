package com.bsp.supportassistant.api;

import com.bsp.supportassistant.model.Ticket;
import com.bsp.supportassistant.model.TicketStatus;
import com.bsp.supportassistant.repository.TicketRepository;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/tickets")
public class TicketController {

    private final TicketRepository ticketRepository;

    public TicketController(TicketRepository ticketRepository) {
        this.ticketRepository = ticketRepository;
    }

    // GET /api/v1/tickets lists every ticket.
    // GET /api/v1/tickets?status=OPEN lists only the tickets with that status.
    @GetMapping
    public List<Ticket> findAll(@RequestParam(required = false) TicketStatus status) {
        if (status == null) {
            return ticketRepository.findAll();
        }
        return ticketRepository.findAllByStatus(status);
    }

    @GetMapping("/{id}")
    public Ticket findById(@PathVariable Long id) {
        return ticketRepository.findById(id);
    }

    @PostMapping
    public Ticket create(@RequestBody Ticket ticket) {
        return ticketRepository.save(ticket);
    }

    // Send only the fields you want to change.
    @PatchMapping("/{id}")
    public Ticket update(@PathVariable Long id, @RequestBody Ticket ticket) {
        return ticketRepository.update(id, ticket);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        ticketRepository.deleteById(id);
    }
}
