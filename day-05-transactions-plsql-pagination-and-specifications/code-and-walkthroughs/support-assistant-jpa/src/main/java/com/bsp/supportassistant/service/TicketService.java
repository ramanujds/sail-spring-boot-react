package com.bsp.supportassistant.service;

import com.bsp.supportassistant.dto.CloseRequest;
import com.bsp.supportassistant.dto.CommentRequest;
import com.bsp.supportassistant.dto.TicketRequest;
import com.bsp.supportassistant.exception.TicketNotFoundException;
import com.bsp.supportassistant.mapper.CommentMapper;
import com.bsp.supportassistant.mapper.TicketMapper;
import com.bsp.supportassistant.model.Ticket;
import com.bsp.supportassistant.model.TicketStatus;
import com.bsp.supportassistant.model.Priority;
import com.bsp.supportassistant.repository.CommentRepository;
import com.bsp.supportassistant.repository.OverdueRepository;
import com.bsp.supportassistant.repository.TicketRepository;
import com.bsp.supportassistant.repository.TicketSpecifications;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class TicketService {

    private final TicketRepository ticketRepository;
    private final CommentRepository commentRepository;
    private final OverdueRepository overdueRepository;

    public TicketService(TicketRepository ticketRepository, CommentRepository commentRepository,
                         OverdueRepository overdueRepository) {
        this.ticketRepository = ticketRepository;
        this.commentRepository = commentRepository;
        this.overdueRepository = overdueRepository;
    }

    // Every filter is optional. Only the filters that were sent are added to the query.
    public Page<Ticket> findAll(TicketStatus status, Priority priority, String keyword,
                                boolean overdue, Pageable pageable) {
        Specification<Ticket> spec = (root, query, cb) -> cb.conjunction(); // always true
        if (status != null) {
            spec = spec.and(TicketSpecifications.hasStatus(status));
        }
        if (priority != null) {
            spec = spec.and(TicketSpecifications.hasPriority(priority));
        }
        if (keyword != null && !keyword.isBlank()) {
            spec = spec.and(TicketSpecifications.matchesKeyword(keyword));
        }
        if (overdue) {
            spec = spec.and(TicketSpecifications.isOverdue(LocalDateTime.now()));
        }
        return ticketRepository.findAll(spec, pageable);
    }

    // A new ticket starts OPEN, and gets its due date from its priority.
    public Ticket create(TicketRequest request) {
        Ticket ticket = TicketMapper.toEntity(request);
        LocalDateTime now = LocalDateTime.now();
        ticket.setStatus(TicketStatus.OPEN);
        ticket.setCreatedAt(now);
        ticket.setDueBy(SlaPolicy.dueBy(ticket.getPriority(), now));
        return ticketRepository.save(ticket);
    }

    // A new priority gives the ticket a new due date, counted from when it was created.
    public Ticket update(UUID id, TicketRequest request) {
        Ticket ticket = ticketRepository.findById(id)
                .orElseThrow(() -> new TicketNotFoundException(id));
        TicketMapper.applyChanges(ticket, request);
        if (request.priority() != null) {
            ticket.setDueBy(SlaPolicy.dueBy(ticket.getPriority(), ticket.getCreatedAt()));
        }
        ticketRepository.save(ticket);
        return ticket;
    }

    // Two changes that belong together: the status, and the closing note.
    // Both are saved, or neither is.
    @Transactional
    public void close(UUID id, CloseRequest request) {
        Ticket ticket = ticketRepository.findById(id)
                .orElseThrow(() -> new TicketNotFoundException(id));

        ticket.setStatus(TicketStatus.CLOSED);
        ticketRepository.save(ticket);

        CommentRequest note = new CommentRequest(request.agent(), request.note());
        commentRepository.save(CommentMapper.toEntity(note, ticket));
    }

    public int countOverdue(Priority priority) {
        return overdueRepository.countOverdue(priority);
    }

    // The procedure updates many rows. If anything fails, none of them are changed.
    @Transactional
    public int escalateOverdue() {
        return overdueRepository.escalateOverdue();
    }
}
