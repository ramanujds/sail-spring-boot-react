package com.bsp.supportassistant.api;

import com.bsp.supportassistant.dto.TicketRequest;
import com.bsp.supportassistant.dto.TicketResponse;
import com.bsp.supportassistant.mapper.TicketMapper;
import com.bsp.supportassistant.model.Ticket;
import com.bsp.supportassistant.model.TicketStatus;
import com.bsp.supportassistant.repository.TicketRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
import java.util.UUID;

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
    public List<TicketResponse> findAll(@RequestParam(required = false) TicketStatus status) {
        if (status == null) {
            return TicketMapper.toResponseList(ticketRepository.findAll());
        }
        return TicketMapper.toResponseList(ticketRepository.findAllByStatus(status));
    }

    @GetMapping("/{id}")
    public ResponseEntity<TicketResponse> findById(@PathVariable UUID id) {
        return ticketRepository.findById(id)
                .map(TicketMapper::toResponse)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<TicketResponse> create(@RequestBody TicketRequest request) {
        Ticket ticket = TicketMapper.toEntity(request);
        ticket.setStatus(TicketStatus.OPEN); // a new ticket always starts OPEN
        TicketResponse response = TicketMapper.toResponse(ticketRepository.save(ticket));
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // Send only the fields you want to change.
    @PatchMapping("/{id}")
    public ResponseEntity<TicketResponse> update(@PathVariable UUID id, @RequestBody TicketRequest request) {
        return ticketRepository.update(id, TicketMapper.toEntity(request))
                .map(TicketMapper::toResponse)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        if (ticketRepository.deleteById(id)) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }
}
