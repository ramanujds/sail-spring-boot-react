package com.bsp.supportassistant.repository;

import com.bsp.supportassistant.model.Ticket;
import com.bsp.supportassistant.model.TicketStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TicketRepository extends JpaRepository<Ticket, UUID> {

    // Load each ticket together with its comments, in one query.
    @Override
    @EntityGraph(attributePaths = "comments")
    List<Ticket> findAll();

    @EntityGraph(attributePaths = "comments")
    List<Ticket> findAllByStatus(TicketStatus status);

    @EntityGraph(attributePaths = "comments")
    Optional<Ticket> findWithCommentsById(UUID id);
}
