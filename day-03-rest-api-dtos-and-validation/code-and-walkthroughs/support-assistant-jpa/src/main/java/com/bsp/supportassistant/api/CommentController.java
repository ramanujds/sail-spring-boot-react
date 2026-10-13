package com.bsp.supportassistant.api;

import com.bsp.supportassistant.dto.CommentRequest;
import com.bsp.supportassistant.dto.CommentResponse;
import com.bsp.supportassistant.mapper.CommentMapper;
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

    @GetMapping
    public ResponseEntity<List<CommentResponse>> findAll(@PathVariable UUID ticketId) {
        if (!ticketRepository.existsById(ticketId)) {
            return ResponseEntity.notFound().build();
        }
        List<CommentResponse> comments = CommentMapper.toResponseList(
                commentRepository.findAllByTicketIdOrderByCreatedAtAsc(ticketId));
        return ResponseEntity.ok(comments);
    }

    @PostMapping
    public ResponseEntity<CommentResponse> create(@PathVariable UUID ticketId,
                                                  @RequestBody CommentRequest request) {
        return ticketRepository.findById(ticketId)
                .map(ticket -> commentRepository.save(CommentMapper.toEntity(request, ticket)))
                .map(CommentMapper::toResponse)
                .map(saved -> ResponseEntity.status(HttpStatus.CREATED).body(saved))
                .orElse(ResponseEntity.notFound().build());
    }
}
