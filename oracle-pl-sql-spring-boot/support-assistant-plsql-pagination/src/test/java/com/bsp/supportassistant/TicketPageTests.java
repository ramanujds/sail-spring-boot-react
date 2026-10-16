package com.bsp.supportassistant;

import com.bsp.supportassistant.dto.PageResponse;
import com.bsp.supportassistant.dto.TicketResponse;
import com.bsp.supportassistant.exception.InvalidRequestException;
import com.bsp.supportassistant.repository.TicketRepository;
import com.bsp.supportassistant.service.TicketService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// These tests call the page procedure in your Oracle database, and roll back.
@SpringBootTest
@Transactional
class TicketPageTests {

    @Autowired
    private TicketService ticketService;

    @Autowired
    private TicketRepository ticketRepository;

    // Three tickets, so that there are at least two pages of 2.
    @BeforeEach
    void createTickets() {
        ticketRepository.createTicket("Page test: high", null, "HIGH");
        ticketRepository.createTicket("Page test: low", null, "LOW");
        ticketRepository.createTicket("Page test: medium", null, "MEDIUM");
    }

    @Test
    void aPageHoldsAtMostSizeTicketsAndCountsThemAll() {
        PageResponse<TicketResponse> first = ticketService.findPage(null, 0, 2, "dueBy", "asc");

        assertThat(first.content()).hasSize(2);
        assertThat(first.totalElements()).isEqualTo(ticketRepository.findTickets(null).size());
        assertThat(first.totalPages()).isGreaterThanOrEqualTo(2);
    }

    @Test
    void thePagesTogetherHoldEveryTicketOnceInOrder() {
        List<TicketResponse> all = new ArrayList<>();
        PageResponse<TicketResponse> page = ticketService.findPage(null, 0, 2, "dueBy", "asc");
        for (int number = 0; number < page.totalPages(); number++) {
            all.addAll(ticketService.findPage(null, number, 2, "dueBy", "asc").content());
        }

        assertThat(all).hasSize((int) page.totalElements());
        assertThat(all).extracting(TicketResponse::id).doesNotHaveDuplicates();
        assertThat(all).extracting(TicketResponse::dueBy).isSortedAccordingTo(Comparator.naturalOrder());
    }

    @Test
    void aPageAfterTheLastIsEmpty() {
        PageResponse<TicketResponse> page = ticketService.findPage(null, 9999, 5, "dueBy", "asc");

        assertThat(page.content()).isEmpty();
        assertThat(page.totalElements()).isPositive();
    }

    @Test
    void aSortThatIsNotAllowedIsRefused() {
        assertThatThrownBy(() -> ticketService.findPage(null, 0, 5, "colour", "asc"))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessage("Cannot sort by 'colour'. Use dueBy or createdAt");
    }
}
