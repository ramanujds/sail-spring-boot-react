package com.helpdeskpro.lab.sealedtypes;

import java.util.Arrays;
import java.util.List;

/**
 * Sealed types: nothing to implement here. Run it, then work through the
 * experiments in the walkthrough.
 */
public class SealedTypesDemo {

    public static void main(String[] args) {
        List<TicketEvent> timeline = List.of(
                new TicketEvent.Opened(101, "Printer jam"),
                new TicketEvent.Assigned(101, "agent-1"),
                new TicketEvent.Resolved(101, "Replaced the toner"),
                new TicketEvent.Closed(101));

        timeline.forEach(event -> System.out.println(event));

        System.out.println("sealed:    " + TicketEvent.class.isSealed());
        System.out.println("permitted: " + Arrays.stream(TicketEvent.class.getPermittedSubclasses())
                .map(Class::getSimpleName)
                .sorted()
                .toList());
    }
}
