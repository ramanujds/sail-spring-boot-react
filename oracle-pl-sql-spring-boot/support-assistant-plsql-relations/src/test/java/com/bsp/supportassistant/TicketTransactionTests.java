package com.bsp.supportassistant;

import com.bsp.supportassistant.dto.CreateTicketWithCommentRequest;
import com.bsp.supportassistant.dto.TicketResponse;
import com.bsp.supportassistant.repository.TicketRepository;
import com.bsp.supportassistant.service.TicketService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.UncategorizedSQLException;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// This class is NOT @Transactional on purpose. The service opens its own transaction,
// and the test checks what is left in the database after that transaction is undone.
@SpringBootTest
class TicketTransactionTests {

    @Autowired
    private TicketService ticketService;

    @Autowired
    private TicketRepository ticketRepository;

    @Test
    void aRefusedFirstCommentLeavesNoTicketBehind() {
        String title = "Atomic test " + UUID.randomUUID();
        CreateTicketWithCommentRequest request =
                new CreateTicketWithCommentRequest(title, null, "LOW", "Meera", null);

        assertThatThrownBy(() -> ticketService.createWithFirstComment(request))
                .isInstanceOf(UncategorizedSQLException.class)
                .hasMessageContaining("ORA-20005");

        assertThat(ticketRepository.findTickets(null))
                .extracting(TicketResponse::title)
                .doesNotContain(title);
    }
}
