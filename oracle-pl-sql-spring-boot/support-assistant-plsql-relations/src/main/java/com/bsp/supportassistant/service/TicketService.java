package com.bsp.supportassistant.service;

import com.bsp.supportassistant.dto.CommentRequest;
import com.bsp.supportassistant.dto.CreateTicketRequest;
import com.bsp.supportassistant.dto.CreateTicketWithCommentRequest;
import com.bsp.supportassistant.dto.TicketResponse;
import com.bsp.supportassistant.dto.TicketWithComments;
import com.bsp.supportassistant.repository.TicketRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

// The business rules are in the PL/SQL package. The service decides what happens together.
@Service
public class TicketService {

    private final TicketRepository ticketRepository;

    public TicketService(TicketRepository ticketRepository) {
        this.ticketRepository = ticketRepository;
    }

    public long create(CreateTicketRequest request) {
        return ticketRepository.createTicket(request.title(), request.description(), request.priority());
    }

    public void changeStatus(long ticketId, String newStatus) {
        ticketRepository.changeStatus(ticketId, newStatus);
    }

    public List<TicketResponse> find(String status) {
        return ticketRepository.findTickets(status);
    }

    public long addComment(long ticketId, CommentRequest request) {
        return ticketRepository.addComment(ticketId, request.author(), request.body());
    }

    public TicketWithComments get(long ticketId) {
        return ticketRepository.getTicket(ticketId);
    }

    public List<TicketWithComments> findWithComments(String status) {
        return ticketRepository.findTicketsWithComments(status);
    }

    // All or nothing: if the comment is refused, the ticket is put back too.
    // The comment needs the id of the ticket, so the ticket must be created first.
    @Transactional
    public long createWithFirstComment(CreateTicketWithCommentRequest request) {
        long ticketId = ticketRepository.createTicket(request.title(), request.description(), request.priority());
        ticketRepository.addComment(ticketId, request.author(), request.comment());
        return ticketId;
    }
}
