package com.bsp.supportassistant.api;

import com.bsp.supportassistant.dto.CloseRequest;
import com.bsp.supportassistant.dto.PageResponse;
import com.bsp.supportassistant.dto.TicketRequest;
import com.bsp.supportassistant.dto.TicketResponse;
import com.bsp.supportassistant.exception.InvalidSortException;
import com.bsp.supportassistant.exception.TicketNotFoundException;
import com.bsp.supportassistant.mapper.TicketMapper;
import com.bsp.supportassistant.model.Priority;
import com.bsp.supportassistant.model.TicketStatus;
import com.bsp.supportassistant.repository.TicketRepository;
import com.bsp.supportassistant.service.TicketService;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
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
import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/tickets")
public class TicketController {

    private final TicketRepository ticketRepository;
    private final TicketService ticketService;

    // The fields a list may be sorted by.
    private static final Set<String> SORT_FIELDS = Set.of("title", "status", "priority", "dueBy", "createdAt");

    public TicketController(TicketRepository ticketRepository, TicketService ticketService) {
        this.ticketRepository = ticketRepository;
        this.ticketService = ticketService;
    }

    // GET /api/v1/tickets?status=OPEN&priority=HIGH&keyword=printer&overdue=true&page=0&size=5&sort=title
    // Every parameter is optional. Without any, you get the first 5 tickets, the most urgent first.
    @GetMapping
    public PageResponse<TicketResponse> findAll(@RequestParam(required = false) TicketStatus status,
                                                @RequestParam(required = false) Priority priority,
                                                @RequestParam(required = false) String keyword,
                                                @RequestParam(defaultValue = "false") boolean overdue,
                                                @PageableDefault(size = 5, sort = "dueBy") Pageable pageable) {
        checkSort(pageable);
        return TicketMapper.toPageResponse(
                ticketService.findAll(status, priority, keyword, overdue, pageable));
    }

    private void checkSort(Pageable pageable) {
        for (Sort.Order order : pageable.getSort()) {
            if (!SORT_FIELDS.contains(order.getProperty())) {
                throw new InvalidSortException(order.getProperty());
            }
        }
    }

    // GET /api/v1/tickets/count?status=OPEN
    @GetMapping("/count")
    public long count(@RequestParam TicketStatus status) {
        return ticketRepository.countByStatus(status);
    }

    // GET /api/v1/tickets/overdue/count?priority=LOW calls the PL/SQL function.
    @GetMapping("/overdue/count")
    public int countOverdue(@RequestParam Priority priority) {
        return ticketService.countOverdue(priority);
    }

    // POST /api/v1/tickets/escalate-overdue calls the PL/SQL procedure.
    @PostMapping("/escalate-overdue")
    public int escalateOverdue() {
        return ticketService.escalateOverdue();
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
        TicketResponse response = TicketMapper.toResponse(ticketService.create(request));
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // Send only the fields you want to change.
    @PatchMapping("/{id}")
    public TicketResponse update(@PathVariable UUID id, @RequestBody TicketRequest request) {
        return TicketMapper.toResponse(ticketService.update(id, request));
    }

    // POST /api/v1/tickets/{id}/close closes the ticket and adds the note as a comment.
    @PostMapping("/{id}/close")
    public TicketResponse close(@PathVariable UUID id, @RequestBody CloseRequest request) {
        ticketService.close(id, request);
        return ticketRepository.findById(id)
                .map(TicketMapper::toResponse)
                .orElseThrow(() -> new TicketNotFoundException(id));
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
