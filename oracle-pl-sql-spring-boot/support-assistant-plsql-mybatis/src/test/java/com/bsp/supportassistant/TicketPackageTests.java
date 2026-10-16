package com.bsp.supportassistant;

import com.bsp.supportassistant.dto.TicketResponse;
import com.bsp.supportassistant.repository.TicketRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.UncategorizedSQLException;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// These tests call the real package in your Oracle database.
// @Transactional on a test class rolls each test back, so the tables stay as they were.
@SpringBootTest
@Transactional
class TicketPackageTests {

    @Autowired
    private TicketRepository ticketRepository;

    @Test
    void createTicketGivesBackAnIdAndStartsOpen() {
        long id = ticketRepository.createTicket("Printer is jammed", "Second floor", "HIGH");

        List<TicketResponse> open = ticketRepository.findTickets("OPEN");

        assertThat(open).extracting(TicketResponse::id).contains(id);
    }

    @Test
    void openCountGoesUpByOne() {
        int before = ticketRepository.openCount("MEDIUM");

        ticketRepository.createTicket("VPN drops every hour", null, "MEDIUM");

        assertThat(ticketRepository.openCount("MEDIUM")).isEqualTo(before + 1);
    }

    @Test
    void highPriorityIsDueInOneDay() {
        long id = ticketRepository.createTicket("Server room is hot", null, "HIGH");

        TicketResponse ticket = ticketRepository.findTickets(null).stream()
                .filter(t -> t.id() == id)
                .findFirst()
                .orElseThrow();

        assertThat(Duration.between(ticket.createdAt(), ticket.dueBy())).isEqualTo(Duration.ofDays(1));
    }

    @Test
    void aTicketCannotSkipAStep() {
        long id = ticketRepository.createTicket("Reset my password", null, "LOW");

        assertThatThrownBy(() -> ticketRepository.changeStatus(id, "CLOSED"))
                .isInstanceOf(UncategorizedSQLException.class)
                .hasMessageContaining("ORA-20004");
    }
}
