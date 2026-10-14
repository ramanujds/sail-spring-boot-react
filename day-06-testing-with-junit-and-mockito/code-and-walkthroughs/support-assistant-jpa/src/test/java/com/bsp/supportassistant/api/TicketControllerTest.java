package com.bsp.supportassistant.api;

import com.bsp.supportassistant.dto.CloseRequest;
import com.bsp.supportassistant.model.Priority;
import com.bsp.supportassistant.model.Ticket;
import com.bsp.supportassistant.model.TicketStatus;
import com.bsp.supportassistant.repository.TicketRepository;
import com.bsp.supportassistant.service.TicketService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Starts only the web layer: TicketController, the exception handler, and JSON conversion.
// There is no database. The controller's two collaborators are mocks.
@WebMvcTest(TicketController.class)
class TicketControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TicketService ticketService;

    @MockitoBean
    private TicketRepository ticketRepository;

    @Test
    void getByIdAnswers200WithTheTicket() throws Exception {
        UUID id = UUID.randomUUID();
        when(ticketRepository.findById(id)).thenReturn(Optional.of(ticket(id, "Printer is offline")));

        mockMvc.perform(get("/api/v1/tickets/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.title").value("Printer is offline"))
                .andExpect(jsonPath("$.status").value("OPEN"))
                .andExpect(jsonPath("$.overdue").value(false));
    }

    @Test
    void getByIdAnswers404WithAnApiErrorWhenTheTicketIsMissing() throws Exception {
        UUID id = UUID.randomUUID();
        when(ticketRepository.findById(id)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/tickets/" + id))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Ticket " + id + " was not found"));
    }

    @Test
    void getByIdAnswers400WhenTheIdIsNotAUuid() throws Exception {
        mockMvc.perform(get("/api/v1/tickets/abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("'abc' is not a valid value for 'id'"));

        verify(ticketRepository, never()).findById(any());
    }

    @Test
    void postAnswers201WithTheCreatedTicket() throws Exception {
        UUID id = UUID.randomUUID();
        when(ticketService.create(any())).thenReturn(ticket(id, "VPN drops"));

        mockMvc.perform(post("/api/v1/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"VPN drops\",\"priority\":\"MEDIUM\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.title").value("VPN drops"));
    }

    @Test
    void postAnswers400WhenTheBodyIsNotJson() throws Exception {
        mockMvc.perform(post("/api/v1/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("The request body is missing or is not valid JSON"));
    }

    @Test
    void listAnswersOnePageOfTickets() throws Exception {
        Ticket first = ticket(UUID.randomUUID(), "Printer is offline");
        when(ticketService.findAll(any(), any(), any(), anyBoolean(), any()))
                .thenReturn(new PageImpl<>(List.of(first), PageRequest.of(0, 5), 1));

        mockMvc.perform(get("/api/v1/tickets?status=OPEN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].title").value("Printer is offline"))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.totalPages").value(1));
    }

    @Test
    void listAnswers400ForAFieldThatCannotBeSortedBy() throws Exception {
        mockMvc.perform(get("/api/v1/tickets?sort=colour"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Cannot sort by 'colour'"));
    }

    @Test
    void closeCallsTheServiceAndAnswersWithTheClosedTicket() throws Exception {
        UUID id = UUID.randomUUID();
        Ticket closed = ticket(id, "Printer is offline");
        closed.setStatus(TicketStatus.CLOSED);
        when(ticketRepository.findById(id)).thenReturn(Optional.of(closed));

        mockMvc.perform(post("/api/v1/tickets/" + id + "/close")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"agent\":\"Karan\",\"note\":\"Fixed\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CLOSED"));

        verify(ticketService).close(eq(id), eq(new CloseRequest("Karan", "Fixed")));
    }

    @Test
    void patchAnswers200WithTheChangedTicket() throws Exception {
        UUID id = UUID.randomUUID();
        Ticket changed = ticket(id, "Printer is offline");
        changed.setPriority(Priority.HIGH);
        when(ticketService.update(eq(id), any())).thenReturn(changed);

        mockMvc.perform(patch("/api/v1/tickets/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"priority\":\"HIGH\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.priority").value("HIGH"));
    }

    private Ticket ticket(UUID id, String title) {
        Ticket ticket = new Ticket();
        ticket.setId(id);
        ticket.setTitle(title);
        ticket.setStatus(TicketStatus.OPEN);
        ticket.setPriority(Priority.MEDIUM);
        ticket.setDueBy(LocalDateTime.now().plusDays(3));
        return ticket;
    }
}
