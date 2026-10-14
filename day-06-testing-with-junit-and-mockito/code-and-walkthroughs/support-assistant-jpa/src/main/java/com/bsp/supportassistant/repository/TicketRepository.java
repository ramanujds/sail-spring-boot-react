package com.bsp.supportassistant.repository;

import com.bsp.supportassistant.model.Ticket;
import com.bsp.supportassistant.model.TicketStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface TicketRepository extends JpaRepository<Ticket, UUID>, JpaSpecificationExecutor<Ticket> {

    long countByStatus(TicketStatus status);

    // Tickets whose title or description contains the keyword, ignoring upper and lower case.
    @Query("select t from Ticket t "
            + "where lower(t.title) like lower(concat('%', :keyword, '%')) "
            + "or lower(t.description) like lower(concat('%', :keyword, '%')) "
            + "order by t.title")
    List<Ticket> search(@Param("keyword") String keyword);
}
