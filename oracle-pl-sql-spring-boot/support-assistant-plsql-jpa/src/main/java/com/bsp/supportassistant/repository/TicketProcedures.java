package com.bsp.supportassistant.repository;

import org.springframework.data.jpa.repository.query.Procedure;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

// Spring Data creates the code of these two methods. Each one calls a procedure of the package.
// It extends Repository, not JpaRepository, so there is no save() or delete() that would skip the package.
public interface TicketProcedures extends Repository<TicketEntity, Long> {

    // outputParameterName is the OUT parameter whose value is returned.
    @Procedure(procedureName = "SUPPORT_TICKET_PKG.CREATE_TICKET", outputParameterName = "P_TICKET_ID")
    Long createTicket(@Param("p_title") String title,
                      @Param("p_description") String description,
                      @Param("p_priority") String priority);

    // A procedure with no OUT parameter returns void.
    @Procedure(procedureName = "SUPPORT_TICKET_PKG.CHANGE_STATUS")
    void changeStatus(@Param("p_ticket_id") Long ticketId, @Param("p_new_status") String newStatus);
}
