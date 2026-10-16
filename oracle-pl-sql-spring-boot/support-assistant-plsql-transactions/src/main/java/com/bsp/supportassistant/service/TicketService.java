package com.bsp.supportassistant.service;

import com.bsp.supportassistant.dto.CreateTicketRequest;
import com.bsp.supportassistant.dto.TicketResponse;
import com.bsp.supportassistant.exception.InvalidRequestException;
import com.bsp.supportassistant.repository.TicketRepository;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

// The business rules are in the PL/SQL package. The service decides what happens together.
@Service
public class TicketService {

    private final TicketRepository ticketRepository;
    private final ErrorLogService errorLogService;

    public TicketService(TicketRepository ticketRepository, ErrorLogService errorLogService) {
        this.ticketRepository = ticketRepository;
        this.errorLogService = errorLogService;
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
    // The reason is written to SUPPORT_ERROR_LOG first, in a transaction of its own.
    @Transactional
    public void changeStatus(List<Long> ticketIds, String newStatus) {
        if (ticketIds == null || ticketIds.isEmpty()) {
            throw new InvalidRequestException("ticketIds must hold at least one id");
        }
        for (Long ticketId : ticketIds) {
            try {
                ticketRepository.changeStatus(ticketId, newStatus);
            } catch (DataAccessException ex) {
                // Saved in a transaction of its own, so the rollback of this method does not remove it.
                errorLogService.record("Bulk change to " + newStatus + " stopped at ticket " + ticketId);
                throw ex;
            }
        }
    }

    public List<TicketResponse> find(String status) {
        return ticketRepository.findTickets(status);
    }
}
