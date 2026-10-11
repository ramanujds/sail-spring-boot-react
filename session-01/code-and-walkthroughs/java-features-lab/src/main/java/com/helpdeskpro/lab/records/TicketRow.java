package com.helpdeskpro.lab.records;

import com.helpdeskpro.lab.domain.TicketStatus;

/**
 * A mutable, entity-style class with getters and setters.
 *
 * It stands in for a persistence entity. Notice what it does not give you:
 * two rows with the same data are not equal, and anyone holding a reference
 * can change it after you have handed it out.
 */
public class TicketRow {

    private final long id;
    private String title;
    private TicketStatus status;
    private String assignedTo;

    public TicketRow(long id, String title, TicketStatus status, String assignedTo) {
        this.id = id;
        this.title = title;
        this.status = status;
        this.assignedTo = assignedTo;
    }

    public long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public TicketStatus getStatus() {
        return status;
    }

    public void setStatus(TicketStatus status) {
        this.status = status;
    }

    public String getAssignedTo() {
        return assignedTo;
    }

    public void setAssignedTo(String assignedTo) {
        this.assignedTo = assignedTo;
    }
}
