package com.bsp.supportassistant.api;

import com.bsp.supportassistant.dto.CommentRequest;
import com.bsp.supportassistant.dto.CommentResponse;
import com.bsp.supportassistant.exception.TicketNotFoundException;
import com.bsp.supportassistant.mapper.CommentMapper;
import com.bsp.supportassistant.model.Comment;
import com.bsp.supportassistant.model.Ticket;
import com.bsp.supportassistant.repository.CommentRepository;
import com.bsp.supportassistant.repository.TicketRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/tickets/{ticketId}/comments")
public class CommentController {

    private final TicketRepository ticketRepository;
    private final CommentRepository commentRepository;

    public CommentController(TicketRepository ticketRepository, CommentRepository commentRepository) {
        this.ticketRepository = ticketRepository;
        this.commentRepository = commentRepository;
    }

    // GET /api/v1/tickets/{ticketId}/comments lists the comments of one ticket, oldest first.
    @GetMapping
    public List<CommentResponse> findAll(@PathVariable UUID ticketId) {
        if (!ticketRepository.existsById(ticketId)) {
            throw new TicketNotFoundException(ticketId);
        }
        return CommentMapper.toResponseList(
                commentRepository.findAllByTicketIdOrderByCreatedAtAsc(ticketId));
    }

    // POST /api/v1/tickets/{ticketId}/comments adds a comment to that ticket.
    @PostMapping
    public ResponseEntity<CommentResponse> create(@PathVariable UUID ticketId,
                                                  @RequestBody CommentRequest request) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new TicketNotFoundException(ticketId));
        Comment saved = commentRepository.save(CommentMapper.toEntity(request, ticket));
        return ResponseEntity.status(HttpStatus.CREATED).body(CommentMapper.toResponse(saved));
    }
}
