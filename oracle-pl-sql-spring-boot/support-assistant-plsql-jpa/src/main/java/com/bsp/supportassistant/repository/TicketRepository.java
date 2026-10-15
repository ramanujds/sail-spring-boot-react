package com.bsp.supportassistant.repository;

import com.bsp.supportassistant.dto.TicketResponse;
import jakarta.persistence.EntityManager;
import jakarta.persistence.ParameterMode;
import jakarta.persistence.StoredProcedureQuery;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

// Calls the programs of the PL/SQL package SUPPORT_TICKET_PKG through JPA.
// @Transactional: JPA reads an OUT value after the call, so the call and the read need one transaction.
@Repository
@Transactional
public class TicketRepository {

    private final TicketProcedures ticketProcedures;
    private final EntityManager entityManager;

    public TicketRepository(TicketProcedures ticketProcedures, EntityManager entityManager) {
        this.ticketProcedures = ticketProcedures;
        this.entityManager = entityManager;
    }

    // JPA's procedure call does not work for a function, so the function is called from a SELECT.
    public int openCount(String priority) {
        Object count = entityManager
                .createNativeQuery("SELECT SUPPORT_TICKET_PKG.open_count(:priority) FROM DUAL")
                .setParameter("priority", priority)
                .getSingleResult();
        return ((Number) count).intValue();
    }

    public long createTicket(String title, String description, String priority) {
        return ticketProcedures.createTicket(title, description, priority);
    }

    public void changeStatus(long ticketId, String newStatus) {
        ticketProcedures.changeStatus(ticketId, newStatus);
    }

    // Each row of the cursor arrives as an Object[], in the order of the SELECT in find_tickets.
    @SuppressWarnings("unchecked")
    public List<TicketResponse> findTickets(String status) {
        StoredProcedureQuery query = entityManager
                .createStoredProcedureQuery("SUPPORT_TICKET_PKG.FIND_TICKETS")
                .registerStoredProcedureParameter("p_status", String.class, ParameterMode.IN)
                .registerStoredProcedureParameter("p_tickets", void.class, ParameterMode.REF_CURSOR)
                .setParameter("p_status", status);
        List<Object[]> rows = query.getResultList();
        return rows.stream().map(this::toTicket).toList();
    }

    // NUMBER is a BigDecimal and VARCHAR2 is a String.
    private TicketResponse toTicket(Object[] row) {
        return new TicketResponse(
                ((Number) row[0]).longValue(),
                (String) row[1],
                (String) row[2],
                (String) row[3],
                (String) row[4],
                toLocalDateTime(row[5]),
                toLocalDateTime(row[6]));
    }

    // Spring Boot 4 (Hibernate 7) gives a LocalDateTime. Spring Boot 3.5 (Hibernate 6) gives a Timestamp.
    private LocalDateTime toLocalDateTime(Object value) {
        if (value instanceof Timestamp) {
            return ((Timestamp) value).toLocalDateTime();
        }
        return (LocalDateTime) value;
    }
}
