package com.bsp.supportassistant.service;

import com.bsp.supportassistant.model.Priority;
import com.bsp.supportassistant.repository.CommentRepository;
import com.bsp.supportassistant.repository.OverdueRepository;
import com.bsp.supportassistant.repository.TicketRepository;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// The same idea as the other service test, with every step written out.
class TicketServiceByHandTest {

    @Test
    void aServiceCanBeBuiltWithMocksInsteadOfRealRepositories() {
        // 1. make the stand-ins
        TicketRepository ticketRepository = mock(TicketRepository.class);
        CommentRepository commentRepository = mock(CommentRepository.class);
        OverdueRepository overdueRepository = mock(OverdueRepository.class);

        // 2. give them to the real class under test
        TicketService service = new TicketService(ticketRepository, commentRepository, overdueRepository);

        // 3. decide what the mock answers
        when(overdueRepository.countOverdue(Priority.HIGH)).thenReturn(4);

        // 4. call the real class, and check the result
        assertEquals(4, service.countOverdue(Priority.HIGH));

        // 5. check that the mock was called
        verify(overdueRepository).countOverdue(Priority.HIGH);
    }

    @Test
    void aMockThatWasNotToldWhatToAnswerGivesAnEmptyValue() {
        TicketRepository ticketRepository = mock(TicketRepository.class);

        assertTrue(ticketRepository.findById(UUID.randomUUID()).isEmpty());
        assertEquals(0, ticketRepository.countByStatus(null));
    }
}
