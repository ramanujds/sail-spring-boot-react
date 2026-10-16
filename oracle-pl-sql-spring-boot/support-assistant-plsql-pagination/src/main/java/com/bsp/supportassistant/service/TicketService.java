package com.bsp.supportassistant.service;

import com.bsp.supportassistant.dto.CreateTicketRequest;
import com.bsp.supportassistant.dto.PageResponse;
import com.bsp.supportassistant.dto.TicketResponse;
import com.bsp.supportassistant.exception.InvalidRequestException;
import com.bsp.supportassistant.repository.TicketRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

// The business rules are in the PL/SQL package. The service decides what happens together.
@Service
public class TicketService {

    // The fields a list may be sorted by, and the column each one stands for.
    private static final Map<String, String> SORT_COLUMNS = Map.of("dueBy", "DUE_BY", "createdAt", "CREATED_AT");
    private static final int MAX_SIZE = 50;

    private final TicketRepository ticketRepository;

    public TicketService(TicketRepository ticketRepository) {
        this.ticketRepository = ticketRepository;
    }

    public int openCount(String priority) {
        return ticketRepository.openCount(priority);
    }

    public long create(CreateTicketRequest request) {
        return ticketRepository.createTicket(request.title(), request.description(), request.priority());
    }

    public void changeStatus(long ticketId, String newStatus) {
        ticketRepository.changeStatus(ticketId, newStatus);
    }

    // All or nothing: if one ticket cannot change, the tickets before it are put back.
    @Transactional
    public void changeStatus(List<Long> ticketIds, String newStatus) {
        if (ticketIds == null || ticketIds.isEmpty()) {
            throw new InvalidRequestException("ticketIds must hold at least one id");
        }
        for (Long ticketId : ticketIds) {
            ticketRepository.changeStatus(ticketId, newStatus);
        }
    }

    // Checks what the caller asked for, then lets the package read the page.
    public PageResponse<TicketResponse> findPage(String status, int page, int size, String sort, String direction) {
        if (page < 0) {
            throw new InvalidRequestException("page must be 0 or more");
        }
        if (size < 1 || size > MAX_SIZE) {
            throw new InvalidRequestException("size must be from 1 to " + MAX_SIZE);
        }
        String column = SORT_COLUMNS.get(sort);
        if (column == null) {
            throw new InvalidRequestException("Cannot sort by '" + sort + "'. Use dueBy or createdAt");
        }
        if (!direction.equalsIgnoreCase("asc") && !direction.equalsIgnoreCase("desc")) {
            throw new InvalidRequestException("direction must be asc or desc");
        }
        return ticketRepository.findPage(status, page, size, column, direction.toUpperCase());
    }
}
