package com.bsp.supportassistant.api;

import com.bsp.supportassistant.dto.WatcherRequest;
import com.bsp.supportassistant.dto.WatcherResponse;
import com.bsp.supportassistant.exception.AlreadyWatchingException;
import com.bsp.supportassistant.exception.TicketNotFoundException;
import com.bsp.supportassistant.mapper.WatcherMapper;
import com.bsp.supportassistant.model.TicketWatcher;
import com.bsp.supportassistant.model.TicketWatcherId;
import com.bsp.supportassistant.repository.TicketRepository;
import com.bsp.supportassistant.repository.TicketWatcherRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/tickets/{ticketId}/watchers")
public class WatcherController {

    private final TicketRepository ticketRepository;
    private final TicketWatcherRepository watcherRepository;

    public WatcherController(TicketRepository ticketRepository, TicketWatcherRepository watcherRepository) {
        this.ticketRepository = ticketRepository;
        this.watcherRepository = watcherRepository;
    }

    // GET /api/v1/tickets/{ticketId}/watchers lists who watches the ticket.
    @GetMapping
    public List<WatcherResponse> findAll(@PathVariable UUID ticketId) {
        if (!ticketRepository.existsById(ticketId)) {
            throw new TicketNotFoundException(ticketId);
        }
        return WatcherMapper.toResponseList(
                watcherRepository.findAllByIdTicketIdOrderByWatchingSinceAsc(ticketId));
    }

    // POST /api/v1/tickets/{ticketId}/watchers with {"username":"karan"}
    @PostMapping
    public ResponseEntity<WatcherResponse> create(@PathVariable UUID ticketId,
                                                  @RequestBody WatcherRequest request) {
        if (!ticketRepository.existsById(ticketId)) {
            throw new TicketNotFoundException(ticketId);
        }
        TicketWatcherId id = new TicketWatcherId(ticketId, request.username());
        if (watcherRepository.existsById(id)) {
            throw new AlreadyWatchingException(ticketId, request.username());
        }
        TicketWatcher saved = watcherRepository.save(WatcherMapper.toEntity(ticketId, request.username()));
        return ResponseEntity.status(HttpStatus.CREATED).body(WatcherMapper.toResponse(saved));
    }

    // DELETE /api/v1/tickets/{ticketId}/watchers/karan stops watching. The answer is 204
    // whether or not the person was watching.
    @DeleteMapping("/{username}")
    public ResponseEntity<Void> delete(@PathVariable UUID ticketId, @PathVariable String username) {
        watcherRepository.deleteById(new TicketWatcherId(ticketId, username));
        return ResponseEntity.noContent().build();
    }
}
