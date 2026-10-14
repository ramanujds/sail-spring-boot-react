package com.bsp.supportassistant.service;

import com.bsp.supportassistant.dto.TicketRequest;
import com.bsp.supportassistant.model.Priority;
import com.bsp.supportassistant.model.Ticket;
import com.bsp.supportassistant.model.TicketStatus;
import com.bsp.supportassistant.repository.CommentRepository;
import com.bsp.supportassistant.repository.OverdueRepository;
import com.bsp.supportassistant.repository.TicketRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// Spring creates only TicketService. Its three repositories are mocks that Spring puts in the context.
@SpringJUnitConfig(TicketService.class)
class TicketServiceSpringTest {

    @MockitoBean
    private TicketRepository ticketRepository;

    @MockitoBean
    private CommentRepository commentRepository;

    @MockitoBean
    private OverdueRepository overdueRepository;

    @Autowired
    private TicketService ticketService;

    @Test
    void springGivesTheServiceItsMockedRepositories() {
        assertNotNull(ticketService);
    }

    @Test
    void countOverdueAsksTheRepositoryForTheGivenPriority() {
        when(overdueRepository.countOverdue(Priority.LOW)).thenReturn(7);

        int count = ticketService.countOverdue(Priority.LOW);

        assertEquals(7, count);
        verify(overdueRepository).countOverdue(Priority.LOW);
    }

    @Test
    void createSavesTheTicketThroughTheRepository() {
        UUID id = UUID.randomUUID();
        when(ticketRepository.save(any(Ticket.class))).thenAnswer(call -> {
            Ticket saved = call.getArgument(0);
            saved.setId(id); // the database would do this
            return saved;
        });

        Ticket created = ticketService.create(new TicketRequest("VPN drops", null, null, Priority.MEDIUM));

        assertEquals(id, created.getId());
        assertEquals(TicketStatus.OPEN, created.getStatus());
    }
}
