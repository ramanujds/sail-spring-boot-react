package com.bsp.supportassistant;

import com.bsp.supportassistant.dto.CommentResponse;
import com.bsp.supportassistant.dto.TicketWithComments;
import com.bsp.supportassistant.repository.TicketRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.UncategorizedSQLException;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// These tests call the real package in your Oracle database.
// @Transactional on a test class rolls each test back, so the tables stay as they were.
@SpringBootTest
@Transactional
class TicketRelationTests {

    @Autowired
    private TicketRepository ticketRepository;

    @Test
    void commentsComeBackWithTheirTicketInTheOrderTheyWereAdded() {
        long ticketId = ticketRepository.createTicket("Printer is jammed", null, "HIGH");
        ticketRepository.addComment(ticketId, "Meera", "Paper tray is stuck");
        ticketRepository.addComment(ticketId, "Karan", "On my way");

        TicketWithComments result = ticketRepository.getTicket(ticketId);

        assertThat(result.ticket().id()).isEqualTo(ticketId);
        assertThat(result.comments()).extracting(CommentResponse::author).containsExactly("Meera", "Karan");
    }

    @Test
    void aTicketWithoutCommentsHasAnEmptyList() {
        long ticketId = ticketRepository.createTicket("VPN drops every hour", null, "MEDIUM");

        assertThat(ticketRepository.getTicket(ticketId).comments()).isEmpty();
    }

    @Test
    void theJoinedListGroupsCommentsAndKeepsTicketsWithoutComments() {
        long quiet = ticketRepository.createTicket("No comments here", null, "LOW");
        long busy = ticketRepository.createTicket("Two comments here", null, "LOW");
        ticketRepository.addComment(busy, "Meera", "First");
        ticketRepository.addComment(busy, "Karan", "Second");

        List<TicketWithComments> all = ticketRepository.findTicketsWithComments(null);

        assertThat(all).extracting(t -> t.ticket().id()).containsOnlyOnce(quiet, busy);
        assertThat(find(all, quiet).comments()).isEmpty();
        assertThat(find(all, busy).comments()).extracting(CommentResponse::body).containsExactly("First", "Second");
    }

    @Test
    void aClosedTicketTakesNoComments() {
        long ticketId = ticketRepository.createTicket("Reset my password", null, "LOW");
        ticketRepository.changeStatus(ticketId, "IN_PROGRESS");
        ticketRepository.changeStatus(ticketId, "RESOLVED");
        ticketRepository.changeStatus(ticketId, "CLOSED");

        assertThatThrownBy(() -> ticketRepository.addComment(ticketId, "Meera", "Thanks"))
                .isInstanceOf(UncategorizedSQLException.class)
                .hasMessageContaining("ORA-20006");
    }

    @Test
    void aCommentNeedsAnExistingTicket() {
        assertThatThrownBy(() -> ticketRepository.addComment(999_999, "Meera", "Hello"))
                .isInstanceOf(UncategorizedSQLException.class)
                .hasMessageContaining("ORA-20003");
    }

    private TicketWithComments find(List<TicketWithComments> tickets, long ticketId) {
        return tickets.stream().filter(t -> t.ticket().id() == ticketId).findFirst().orElseThrow();
    }
}
