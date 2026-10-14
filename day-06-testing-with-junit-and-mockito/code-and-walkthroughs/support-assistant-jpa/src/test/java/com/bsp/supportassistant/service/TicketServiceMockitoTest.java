package com.bsp.supportassistant.service;

import com.bsp.supportassistant.dto.CloseRequest;
import com.bsp.supportassistant.dto.TicketRequest;
import com.bsp.supportassistant.exception.TicketNotFoundException;
import com.bsp.supportassistant.model.Comment;
import com.bsp.supportassistant.model.Priority;
import com.bsp.supportassistant.model.Ticket;
import com.bsp.supportassistant.model.TicketStatus;
import com.bsp.supportassistant.repository.CommentRepository;
import com.bsp.supportassistant.repository.OverdueRepository;
import com.bsp.supportassistant.repository.TicketRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TicketServiceMockitoTest {

    @Mock
    private TicketRepository ticketRepository;

    @Mock
    private CommentRepository commentRepository;

    @Mock
    private OverdueRepository overdueRepository;

    @InjectMocks
    private TicketService ticketService;

    @Test
    void createStartsTheTicketOpenAndSetsItsDueDate() {
        // save returns the ticket it was given
        when(ticketRepository.save(any(Ticket.class))).thenAnswer(call -> call.getArgument(0));
        TicketRequest request = new TicketRequest("Printer is offline", null, null, Priority.HIGH);

        Ticket created = ticketService.create(request);

        assertEquals(TicketStatus.OPEN, created.getStatus());
        assertEquals(created.getCreatedAt().plusDays(1), created.getDueBy());
    }

    @Test
    void closeSetsTheStatusAndSavesTheNoteAsAComment() {
        UUID id = UUID.randomUUID();
        Ticket ticket = new Ticket();
        ticket.setStatus(TicketStatus.IN_PROGRESS);
        when(ticketRepository.findById(id)).thenReturn(Optional.of(ticket));

        ticketService.close(id, new CloseRequest("Karan", "Fixed the printer queue."));

        assertEquals(TicketStatus.CLOSED, ticket.getStatus());

        ArgumentCaptor<Comment> saved = ArgumentCaptor.forClass(Comment.class);
        verify(commentRepository).save(saved.capture());
        assertEquals("Karan", saved.getValue().getAuthor());
        assertEquals("Fixed the printer queue.", saved.getValue().getText());
    }

    @Test
    void closeOfAnUnknownTicketThrowsAndSavesNothing() {
        UUID id = UUID.randomUUID();
        when(ticketRepository.findById(id)).thenReturn(Optional.empty());

        assertThrows(TicketNotFoundException.class,
                () -> ticketService.close(id, new CloseRequest("Karan", "Done")));

        verify(ticketRepository, never()).save(any());
        verify(commentRepository, never()).save(any());
    }

    @Test
    void escalateOverdueReturnsTheNumberThatTheProcedureChanged() {
        when(overdueRepository.escalateOverdue()).thenReturn(3);

        assertEquals(3, ticketService.escalateOverdue());
    }
}
