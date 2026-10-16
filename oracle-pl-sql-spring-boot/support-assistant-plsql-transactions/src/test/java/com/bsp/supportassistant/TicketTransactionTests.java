package com.bsp.supportassistant;

import com.bsp.supportassistant.dto.TicketResponse;
import com.bsp.supportassistant.repository.TicketRepository;
import com.bsp.supportassistant.service.TicketService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.UncategorizedSQLException;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// This class is NOT @Transactional on purpose. The service opens its own transaction,
// and each test checks what is left in the database after that transaction ends.
// The tickets that the tests create stay in the table.
@SpringBootTest
class TicketTransactionTests {

    @Autowired
    private TicketService ticketService;

    @Autowired
    private TicketRepository ticketRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void aRefusedBulkChangePutsEveryTicketBack() {
        long first = ticketRepository.createTicket("Transaction test: first", null, "LOW");
        long second = ticketRepository.createTicket("Transaction test: second", null, "LOW");
        ticketRepository.changeStatus(second, "IN_PROGRESS");

        // The first ticket can move. The second is already IN_PROGRESS, so the package refuses it.
        assertThatThrownBy(() -> ticketService.changeStatus(List.of(first, second), "IN_PROGRESS"))
                .isInstanceOf(UncategorizedSQLException.class)
                .hasMessageContaining("ORA-20004");

        assertThat(statusOf(first)).isEqualTo("OPEN");
    }

    @Test
    void theErrorLineStaysAfterTheRollback() {
        long ticket = ticketRepository.createTicket("Transaction test: closed", null, "LOW");
        ticketRepository.changeStatus(ticket, "IN_PROGRESS");

        assertThatThrownBy(() -> ticketService.changeStatus(List.of(ticket), "IN_PROGRESS"))
                .isInstanceOf(UncategorizedSQLException.class);

        Integer lines = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM SUPPORT_ERROR_LOG WHERE MESSAGE = ?", Integer.class,
                "Bulk change to IN_PROGRESS stopped at ticket " + ticket);
        assertThat(lines).isEqualTo(1);
    }

    private String statusOf(long ticketId) {
        return ticketRepository.findTickets(null).stream()
                .filter(t -> t.id() == ticketId)
                .map(TicketResponse::status)
                .findFirst()
                .orElseThrow();
    }
}
