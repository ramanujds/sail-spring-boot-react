package com.bsp.supportassistant.repository;

import com.bsp.supportassistant.model.Comment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface CommentRepository extends JpaRepository<Comment, UUID> {

    // ticket.id = ?  ordered by createdAt, oldest first
    List<Comment> findAllByTicketIdOrderByCreatedAtAsc(UUID ticketId);
}
