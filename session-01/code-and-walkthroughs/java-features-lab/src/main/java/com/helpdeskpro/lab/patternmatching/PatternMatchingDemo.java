package com.helpdeskpro.lab.patternmatching;

import com.helpdeskpro.lab.sealedtypes.TicketEvent;
import java.util.List;

/**
 * Pattern matching: run this before and after you complete TODO 2.1 and 2.2.
 */
public class PatternMatchingDemo {

    public static void main(String[] args) {
        List<TicketEvent> timeline = List.of(
                new TicketEvent.Opened(101, "Printer jam"),
                new TicketEvent.Assigned(101, "agent-1"),
                new TicketEvent.Resolved(101, "  "),
                new TicketEvent.Resolved(102, "Replaced the toner"),
                new TicketEvent.Closed(101));

        System.out.println("--- instanceof pattern (provided) ---");
        timeline.forEach(PatternMatchingDemo::printIfOpened);

        System.out.println("--- assigneeOf (TODO 2.1) ---");
        try {
            for (TicketEvent event : timeline) {
                System.out.println(event.getClass().getSimpleName() + " -> " + EventPatterns.assigneeOf(event));
            }
        } catch (UnsupportedOperationException e) {
            System.out.println(e.getMessage());
        }

        System.out.println("--- describe (TODO 2.2) ---");
        try {
            for (TicketEvent event : timeline) {
                System.out.println(EventPatterns.describe(event));
            }
        } catch (UnsupportedOperationException e) {
            System.out.println(e.getMessage());
        }
    }

    // Provided: pattern matching for instanceof (Java 16)
    static void printIfOpened(Object value) {
        if (value instanceof TicketEvent.Opened opened) {
            System.out.println("opened ticket " + opened.ticketId() + " titled " + opened.title());
        }
    }
}
