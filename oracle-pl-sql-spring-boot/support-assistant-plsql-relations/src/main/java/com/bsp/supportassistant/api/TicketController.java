package com.bsp.supportassistant.api;

import com.bsp.supportassistant.dto.CommentCreated;
import com.bsp.supportassistant.dto.CommentRequest;
import com.bsp.supportassistant.dto.CreateTicketRequest;
import com.bsp.supportassistant.dto.CreateTicketWithCommentRequest;
import com.bsp.supportassistant.dto.StatusRequest;
import com.bsp.supportassistant.dto.TicketCreated;
import com.bsp.supportassistant.dto.TicketResponse;
import com.bsp.supportassistant.dto.TicketWithComments;
import com.bsp.supportassistant.service.TicketService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/tickets")
public class TicketController {

    private final TicketService ticketService;

    public TicketController(TicketService ticketService) {
        this.ticketService = ticketService;
    }

    // Calls the procedure FIND_TICKETS. The status is optional.
    @GetMapping
    public List<TicketResponse> find(@RequestParam(required = false) String status) {
        return ticketService.find(status);
    }

    // Calls the procedure CREATE_TICKET.
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TicketCreated create(@RequestBody CreateTicketRequest request) {
        return new TicketCreated(ticketService.create(request));
    }

    // Calls the procedure CHANGE_STATUS.
    @PatchMapping("/{id}/status")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void changeStatus(@PathVariable long id, @RequestBody StatusRequest request) {
        ticketService.changeStatus(id, request.status());
    }

    // Calls the procedure ADD_COMMENT. The comment belongs to the ticket in the URL.
    @PostMapping("/{id}/comments")
    @ResponseStatus(HttpStatus.CREATED)
    public CommentCreated addComment(@PathVariable long id, @RequestBody CommentRequest request) {
        return new CommentCreated(ticketService.addComment(id, request));
    }

    // Calls the procedure GET_TICKET: one ticket and its comments.
    @GetMapping("/{id}")
    public TicketWithComments get(@PathVariable long id) {
        return ticketService.get(id);
    }

    // Calls the procedure FIND_TICKETS_WITH_COMMENTS: many tickets, each with its comments.
    // The fixed path "with-comments" wins over "/{id}", so this does not clash with the route above.
    @GetMapping("/with-comments")
    public List<TicketWithComments> findWithComments(@RequestParam(required = false) String status) {
        return ticketService.findWithComments(status);
    }

    // Creates a ticket and its first comment, in one transaction.
    @PostMapping("/with-first-comment")
    @ResponseStatus(HttpStatus.CREATED)
    public TicketCreated createWithFirstComment(@RequestBody CreateTicketWithCommentRequest request) {
        return new TicketCreated(ticketService.createWithFirstComment(request));
    }
}
