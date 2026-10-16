package com.bsp.supportassistant.api;

import com.bsp.supportassistant.dto.BulkStatusRequest;
import com.bsp.supportassistant.dto.CreateTicketRequest;
import com.bsp.supportassistant.dto.PageResponse;
import com.bsp.supportassistant.dto.StatusRequest;
import com.bsp.supportassistant.dto.TicketCreated;
import com.bsp.supportassistant.dto.TicketResponse;
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


@RestController
@RequestMapping("/api/tickets")
public class TicketController {

    private final TicketService ticketService;

    public TicketController(TicketService ticketService) {
        this.ticketService = ticketService;
    }

    // Calls the function OPEN_COUNT.
    @GetMapping("/open-count")
    public int openCount(@RequestParam String priority) {
        return ticketService.openCount(priority);
    }

    // GET /api/tickets?status=OPEN&page=0&size=5&sort=dueBy&direction=asc returns one page.
    // Every parameter is optional. Without them you get the first 5 tickets, the most urgent first.
    @GetMapping
    public PageResponse<TicketResponse> find(@RequestParam(required = false) String status,
                                             @RequestParam(defaultValue = "0") int page,
                                             @RequestParam(defaultValue = "5") int size,
                                             @RequestParam(defaultValue = "dueBy") String sort,
                                             @RequestParam(defaultValue = "asc") String direction) {
        return ticketService.findPage(status, page, size, sort, direction);
    }

    // Calls the procedure CREATE_TICKET.
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TicketCreated create(@RequestBody CreateTicketRequest request) {
        return new TicketCreated(ticketService.create(request));
    }

    // Calls the procedure CHANGE_STATUS once.
    @PatchMapping("/{id}/status")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void changeStatus(@PathVariable long id, @RequestBody StatusRequest request) {
        ticketService.changeStatus(id, request.status());
    }

    // Calls CHANGE_STATUS for each id, in one transaction.
    @PostMapping("/bulk-status")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void changeStatus(@RequestBody BulkStatusRequest request) {
        ticketService.changeStatus(request.ticketIds(), request.status());
    }
}
