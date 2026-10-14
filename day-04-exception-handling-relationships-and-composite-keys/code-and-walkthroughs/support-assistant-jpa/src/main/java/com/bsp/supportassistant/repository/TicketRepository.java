package com.bsp.supportassistant.repository;

import com.bsp.supportassistant.model.Priority;
import com.bsp.supportassistant.model.Ticket;
import com.bsp.supportassistant.model.TicketStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TicketRepository extends JpaRepository<Ticket, UUID> {

    // Read each ticket together with its comments, in one query.
    @Override
    @EntityGraph(attributePaths = "comments")
    List<Ticket> findAll();

    @Override
    @EntityGraph(attributePaths = "comments")
    Optional<Ticket> findById(UUID id);

    @EntityGraph(attributePaths = "comments")
    List<Ticket> findAllByStatus(TicketStatus status);

    @EntityGraph(attributePaths = "comments")
    List<Ticket> findAllByPriority(Priority priority);

    @EntityGraph(attributePaths = "comments")
    List<Ticket> findAllByStatusAndPriority(TicketStatus status, Priority priority);

    long countByStatus(TicketStatus status);

    // Tickets whose title or description contains the keyword, ignoring upper and lower case.
    @Query("select t from Ticket t "
            + "where lower(t.title) like lower(concat('%', :keyword, '%')) "
            + "or lower(t.description) like lower(concat('%', :keyword, '%')) "
            + "order by t.title")
    @EntityGraph(attributePaths = "comments")
    List<Ticket> search(@Param("keyword") String keyword);
}
