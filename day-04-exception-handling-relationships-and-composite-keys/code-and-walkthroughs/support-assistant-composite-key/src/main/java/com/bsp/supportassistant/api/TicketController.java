package com.bsp.supportassistant.api;

import com.bsp.supportassistant.dto.TicketRequest;
import com.bsp.supportassistant.dto.TicketResponse;
import com.bsp.supportassistant.exception.TicketNotFoundException;
import com.bsp.supportassistant.mapper.TicketMapper;
import com.bsp.supportassistant.model.Priority;
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
    // Add ?status=OPEN, ?priority=HIGH, or both, to list only the tickets that match.
    @GetMapping
    public List<TicketResponse> findAll(@RequestParam(required = false) TicketStatus status,
                                        @RequestParam(required = false) Priority priority) {
        if (status != null && priority != null) {
            return TicketMapper.toResponseList(ticketRepository.findAllByStatusAndPriority(status, priority));
        }
        if (status != null) {
            return TicketMapper.toResponseList(ticketRepository.findAllByStatus(status));
        }
        if (priority != null) {
            return TicketMapper.toResponseList(ticketRepository.findAllByPriority(priority));
        }
        return TicketMapper.toResponseList(ticketRepository.findAll());
    }

    // GET /api/v1/tickets/count?status=OPEN
    @GetMapping("/count")
    public long count(@RequestParam TicketStatus status) {
        return ticketRepository.countByStatus(status);
    }

    // GET /api/v1/tickets/search?keyword=printer
    @GetMapping("/search")
    public List<TicketResponse> search(@RequestParam String keyword) {
        return TicketMapper.toResponseList(ticketRepository.search(keyword));
    }

    @GetMapping("/{id}")
    public ResponseEntity<TicketResponse> findById(@PathVariable UUID id) {
        return ticketRepository.findById(id)
                .map(TicketMapper::toResponse)
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new TicketNotFoundException(id));
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
        Ticket ticket = ticketRepository.findById(id)
                .orElseThrow(() -> new TicketNotFoundException(id));
        TicketMapper.applyChanges(ticket, request);
        ticketRepository.save(ticket);
        return ResponseEntity.ok(TicketMapper.toResponse(ticket));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        if (!ticketRepository.existsById(id)) {
            throw new TicketNotFoundException(id);
        }
        ticketRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
