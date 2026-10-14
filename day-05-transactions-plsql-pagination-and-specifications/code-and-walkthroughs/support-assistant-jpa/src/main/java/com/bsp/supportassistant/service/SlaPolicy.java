package com.bsp.supportassistant.service;

import com.bsp.supportassistant.model.Priority;
import com.bsp.supportassistant.model.Ticket;
import com.bsp.supportassistant.model.TicketStatus;

import java.time.LocalDateTime;

// The service level rules: how long a ticket may stay unresolved.
public class SlaPolicy {

    // HIGH is due in 1 day, MEDIUM in 3 days, LOW in 7 days.
    public static LocalDateTime dueBy(Priority priority, LocalDateTime createdAt) {
        if (priority == Priority.HIGH) {
            return createdAt.plusDays(1);
        }
        if (priority == Priority.MEDIUM) {
            return createdAt.plusDays(3);
        }
        return createdAt.plusDays(7);
    }

    // A ticket is overdue when it is not finished and its due date has passed.
    public static boolean isOverdue(Ticket ticket, LocalDateTime now) {
        boolean finished = ticket.getStatus() == TicketStatus.RESOLVED
                || ticket.getStatus() == TicketStatus.CLOSED;
        return !finished && ticket.getDueBy().isBefore(now);
    }
}
