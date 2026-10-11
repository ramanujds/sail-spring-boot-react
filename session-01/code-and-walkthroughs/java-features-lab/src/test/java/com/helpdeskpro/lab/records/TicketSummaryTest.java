package com.helpdeskpro.lab.records;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.helpdeskpro.lab.domain.TicketStatus;
import org.junit.jupiter.api.Test;

class TicketSummaryTest {

    // Passes from the start: the record already generates these.
    @Test
    void recordGeneratesEqualsAndToString() {
        var a = new TicketSummary(101, "Printer jam", TicketStatus.OPEN);
        var b = new TicketSummary(101, "Printer jam", TicketStatus.OPEN);

        assertEquals(a, b);
        assertEquals("TicketSummary[id=101, title=Printer jam, status=OPEN]", a.toString());
    }

    // TODO 1.1
    @Test
    void nullTitleIsRejected() {
        assertThrows(NullPointerException.class, () -> new TicketSummary(1, null, TicketStatus.OPEN));
    }

    // TODO 1.1
    @Test
    void nullStatusIsRejected() {
        assertThrows(NullPointerException.class, () -> new TicketSummary(1, "Printer jam", null));
    }

    // TODO 1.1
    @Test
    void blankTitleIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new TicketSummary(1, "   ", TicketStatus.OPEN));
    }

    // TODO 1.1
    @Test
    void zeroOrNegativeIdIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new TicketSummary(0, "Printer jam", TicketStatus.OPEN));
        assertThrows(IllegalArgumentException.class, () -> new TicketSummary(-5, "Printer jam", TicketStatus.OPEN));
    }
}
