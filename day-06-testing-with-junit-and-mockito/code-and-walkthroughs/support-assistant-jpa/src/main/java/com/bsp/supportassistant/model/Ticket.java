package com.bsp.supportassistant.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import org.hibernate.annotations.Formula;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "TICKETS")
public class Ticket {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @JdbcTypeCode(SqlTypes.VARCHAR) // store the UUID as text in a VARCHAR2(36) column
    private UUID id;

    private String title;
    private String description;

    @Enumerated(EnumType.STRING)
    private TicketStatus status;

    @Enumerated(EnumType.STRING)
    private Priority priority;

    // The number of comments. The database counts them when the ticket is read. It is not a column.
    @Formula("(select count(*) from COMMENTS c where c.TICKET_ID = ID)")
    private int commentCount;

    private LocalDateTime createdAt;
    private LocalDateTime dueBy;

    // The other side of Comment.ticket. This field has no column in TICKETS.
    @OneToMany(mappedBy = "ticket", cascade = CascadeType.REMOVE)
    private List<Comment> comments = new ArrayList<>();

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public TicketStatus getStatus() {
        return status;
    }

    public void setStatus(TicketStatus status) {
        this.status = status;
    }

    public Priority getPriority() {
        return priority;
    }

    public void setPriority(Priority priority) {
        this.priority = priority;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getDueBy() {
        return dueBy;
    }

    public void setDueBy(LocalDateTime dueBy) {
        this.dueBy = dueBy;
    }

    public int getCommentCount() {
        return commentCount;
    }

    public List<Comment> getComments() {
        return comments;
    }
}
