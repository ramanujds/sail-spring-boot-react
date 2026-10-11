package com.helpdeskpro.lab.switchexpressions;

import com.helpdeskpro.lab.domain.TicketStatus;

/**
 * Switch expressions: run this before and after you complete TODO 3.1 and 3.2.
 */
public class SwitchExpressionsDemo {

    public static void main(String[] args) {
        System.out.println("--- Java 8 style labels ---");
        for (TicketStatus status : TicketStatus.values()) {
            System.out.println(status + " -> " + StatusLabels.labelJava8Style(status));
        }

        System.out.println("--- switch expression labels (TODO 3.1) ---");
        try {
            for (TicketStatus status : TicketStatus.values()) {
                System.out.println(status + " -> " + StatusLabels.label(status));
            }
        } catch (UnsupportedOperationException e) {
            System.out.println(e.getMessage());
        }

        System.out.println("--- SLA messages (TODO 3.2) ---");
        try {
            System.out.println(StatusLabels.slaMessage(TicketStatus.IN_PROGRESS, 5));
            System.out.println(StatusLabels.slaMessage(TicketStatus.IN_PROGRESS, -3));
            System.out.println(StatusLabels.slaMessage(TicketStatus.CLOSED, 0));
        } catch (UnsupportedOperationException e) {
            System.out.println(e.getMessage());
        }
    }
}
