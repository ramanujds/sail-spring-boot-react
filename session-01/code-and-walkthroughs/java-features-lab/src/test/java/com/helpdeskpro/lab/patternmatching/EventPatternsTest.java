package com.helpdeskpro.lab.patternmatching;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.helpdeskpro.lab.sealedtypes.TicketEvent;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class EventPatternsTest {

    // TODO 2.1
    @Test
    void assigneeOfAnAssignedEventIsTheAgent() {
        var event = new TicketEvent.Assigned(101, "agent-1");

        assertEquals(Optional.of("agent-1"), EventPatterns.assigneeOf(event));
    }

    // TODO 2.1
    @Test
    void assigneeOfAnyOtherEventIsEmpty() {
        assertEquals(Optional.empty(), EventPatterns.assigneeOf(new TicketEvent.Opened(101, "Printer jam")));
        assertEquals(Optional.empty(), EventPatterns.assigneeOf(new TicketEvent.Resolved(101, "Fixed")));
        assertEquals(Optional.empty(), EventPatterns.assigneeOf(new TicketEvent.Closed(101)));
    }

    // TODO 2.2
    @Test
    void describesOpened() {
        assertEquals("Ticket 101 opened: Printer jam",
                EventPatterns.describe(new TicketEvent.Opened(101, "Printer jam")));
    }

    // TODO 2.2
    @Test
    void describesAssigned() {
        assertEquals("Ticket 101 assigned to agent-1",
                EventPatterns.describe(new TicketEvent.Assigned(101, "agent-1")));
    }

    // TODO 2.2
    @Test
    void describesResolvedWithResolutionText() {
        assertEquals("Ticket 101 resolved: Replaced the toner",
                EventPatterns.describe(new TicketEvent.Resolved(101, "Replaced the toner")));
    }

    // TODO 2.2
    @Test
    void describesResolvedWithBlankResolutionText() {
        assertEquals("Ticket 101 resolved (no resolution text)",
                EventPatterns.describe(new TicketEvent.Resolved(101, "   ")));
    }

    // TODO 2.2
    @Test
    void describesClosed() {
        assertEquals("Ticket 101 closed", EventPatterns.describe(new TicketEvent.Closed(101)));
    }
}
