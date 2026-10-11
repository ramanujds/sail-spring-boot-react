package com.helpdeskpro.lab.records;

import com.helpdeskpro.lab.domain.TicketStatus;
import java.util.List;

/**
 * Records: run this before and after you complete TODO 1.1 and 1.2.
 */
public class RecordsDemo {

    public static void main(String[] args) {
        var row = new TicketRow(101, "Printer jam", TicketStatus.OPEN, "agent-1");
        var sameRow = new TicketRow(101, "Printer jam", TicketStatus.OPEN, "agent-1");
        System.out.println("row equals sameRow:         " + row.equals(sameRow));

        var summary = TicketSummary.from(row);
        var sameSummary = TicketSummary.from(sameRow);
        System.out.println("summary equals sameSummary: " + summary.equals(sameSummary));
        System.out.println("summary:                    " + summary);

        row.setTitle("Printer on fire");
        System.out.println("row title after change:     " + row.getTitle());
        System.out.println("summary title after change: " + summary.title());

        tryInvalid("blank title", 102, "   ", TicketStatus.OPEN);
        tryInvalid("zero id", 0, "VPN down", TicketStatus.OPEN);

        try {
            System.out.println("toSummaries: " + TicketSummaryMapper.toSummaries(List.of(row, sameRow)));
        } catch (UnsupportedOperationException e) {
            System.out.println(e.getMessage());
        }
    }

    private static void tryInvalid(String label, long id, String title, TicketStatus status) {
        try {
            new TicketSummary(id, title, status);
            System.out.println(label + ": accepted (complete TODO 1.1 to reject it)");
        } catch (RuntimeException e) {
            System.out.println(label + ": rejected with " + e.getClass().getSimpleName());
        }
    }
}
