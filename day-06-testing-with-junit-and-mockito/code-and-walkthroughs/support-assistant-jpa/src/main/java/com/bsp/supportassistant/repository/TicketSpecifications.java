package com.bsp.supportassistant.repository;

import com.bsp.supportassistant.model.Priority;
import com.bsp.supportassistant.model.Ticket;
import com.bsp.supportassistant.model.TicketStatus;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;

// Each method is one condition. Conditions are joined with and(...) when the request needs them.
public class TicketSpecifications {

    public static Specification<Ticket> hasStatus(TicketStatus status) {
        return (root, query, cb) -> cb.equal(root.get("status"), status);
    }

    public static Specification<Ticket> hasPriority(Priority priority) {
        return (root, query, cb) -> cb.equal(root.get("priority"), priority);
    }

    // The keyword is in the title or in the description, in any case.
    public static Specification<Ticket> matchesKeyword(String keyword) {
        return (root, query, cb) -> {
            String pattern = "%" + keyword.toLowerCase() + "%";
            return cb.or(
                    cb.like(cb.lower(root.get("title")), pattern),
                    cb.like(cb.lower(root.get("description")), pattern));
        };
    }

    // Not finished, and the due date has passed. The same rule as SlaPolicy.isOverdue.
    public static Specification<Ticket> isOverdue(LocalDateTime now) {
        return (root, query, cb) -> cb.and(
                root.get("status").in(TicketStatus.OPEN, TicketStatus.IN_PROGRESS),
                cb.lessThan(root.get("dueBy"), now));
    }
}
