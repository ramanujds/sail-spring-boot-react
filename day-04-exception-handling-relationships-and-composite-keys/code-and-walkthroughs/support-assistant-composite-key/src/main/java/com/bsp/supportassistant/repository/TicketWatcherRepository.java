package com.bsp.supportassistant.repository;

import com.bsp.supportassistant.model.TicketWatcher;
import com.bsp.supportassistant.model.TicketWatcherId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

// The second type is the type of the key: the whole TicketWatcherId, not a UUID.
public interface TicketWatcherRepository extends JpaRepository<TicketWatcher, TicketWatcherId> {

    // id.ticketId = ?  ordered by watchingSince, oldest first
    List<TicketWatcher> findAllByIdTicketIdOrderByWatchingSinceAsc(UUID ticketId);
}
