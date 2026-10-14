package com.bsp.supportassistant.mapper;

import com.bsp.supportassistant.dto.TicketRequest;
import com.bsp.supportassistant.dto.TicketResponse;
import com.bsp.supportassistant.model.Ticket;

import java.util.List;

public class TicketMapper {

    public static Ticket toEntity(TicketRequest request) {
        Ticket ticket = new Ticket();
        ticket.setTitle(request.title());
        ticket.setDescription(request.description());
        ticket.setStatus(request.status());
        ticket.setPriority(request.priority());
        return ticket;
    }

    // Copies only the fields that are present in the request onto an existing ticket.
    public static void applyChanges(Ticket ticket, TicketRequest request) {
        if (request.title() != null) {
            ticket.setTitle(request.title());
        }
        if (request.description() != null) {
            ticket.setDescription(request.description());
        }
        if (request.status() != null) {
            ticket.setStatus(request.status());
        }
        if (request.priority() != null) {
            ticket.setPriority(request.priority());
        }
    }

    public static TicketResponse toResponse(Ticket ticket) {
        return new TicketResponse(
                ticket.getId(),
                ticket.getTitle(),
                ticket.getDescription(),
                ticket.getStatus(),
                ticket.getPriority(),
                ticket.getComments().size());
    }

    public static List<TicketResponse> toResponseList(List<Ticket> tickets) {
        return tickets.stream()
                .map(TicketMapper::toResponse)
                .toList();
    }
}
