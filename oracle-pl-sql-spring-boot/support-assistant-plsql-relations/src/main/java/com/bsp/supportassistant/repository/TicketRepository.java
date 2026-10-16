package com.bsp.supportassistant.repository;

import com.bsp.supportassistant.dto.CommentResponse;
import com.bsp.supportassistant.dto.TicketResponse;
import com.bsp.supportassistant.dto.TicketWithComments;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.simple.SimpleJdbcCall;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

// Calls the programs of the PL/SQL package DESK_TICKET_PKG. There is no entity and no JPA here.
@Repository
public class TicketRepository {

    private static final String PACKAGE = "DESK_TICKET_PKG";

    private final SimpleJdbcCall createTicket;
    private final SimpleJdbcCall changeStatus;
    private final SimpleJdbcCall findTickets;
    private final SimpleJdbcCall addComment;
    private final SimpleJdbcCall getTicket;
    private final SimpleJdbcCall findTicketsWithComments;

    public TicketRepository(JdbcTemplate jdbcTemplate) {
        this.createTicket = new SimpleJdbcCall(jdbcTemplate)
                .withCatalogName(PACKAGE).withProcedureName("CREATE_TICKET");
        this.changeStatus = new SimpleJdbcCall(jdbcTemplate)
                .withCatalogName(PACKAGE).withProcedureName("CHANGE_STATUS");
        // P_TICKETS is the SYS_REFCURSOR. Each row of it is turned into a TicketResponse by toTicket.
        this.findTickets = new SimpleJdbcCall(jdbcTemplate)
                .withCatalogName(PACKAGE).withProcedureName("FIND_TICKETS")
                .returningResultSet("P_TICKETS", this::toTicket);
        this.addComment = new SimpleJdbcCall(jdbcTemplate)
                .withCatalogName(PACKAGE).withProcedureName("ADD_COMMENT");
        // Two cursors. Each one is named, and each has its own row mapper.
        this.getTicket = new SimpleJdbcCall(jdbcTemplate)
                .withCatalogName(PACKAGE).withProcedureName("GET_TICKET")
                .returningResultSet("P_TICKET", this::toTicket)
                .returningResultSet("P_COMMENTS", this::toComment);
        // One cursor of joined rows. toJoinedRow builds a ticket and, when there is one, a comment.
        this.findTicketsWithComments = new SimpleJdbcCall(jdbcTemplate)
                .withCatalogName(PACKAGE).withProcedureName("FIND_TICKETS_WITH_COMMENTS")
                .returningResultSet("P_ROWS", this::toJoinedRow);
    }

    // A procedure with an OUT parameter: the values go in the order of the IN parameters.
    // The OUT value comes back in a Map. The key is the parameter name, in upper case.
    public long createTicket(String title, String description, String priority) {
        Map<String, Object> out = createTicket.execute(title, description, priority);
        return ((Number) out.get("P_TICKET_ID")).longValue();
    }

    // A procedure with no OUT parameter. A rule that fails comes back as an exception.
    public void changeStatus(long ticketId, String newStatus) {
        changeStatus.execute(ticketId, newStatus);
    }

    // A procedure with a result set. A null status asks for every ticket.
    @SuppressWarnings("unchecked")
    public List<TicketResponse> findTickets(String status) {
        Map<String, Object> out = findTickets.execute(status);
        return (List<TicketResponse>) out.get("P_TICKETS");
    }

    // The ticket id goes in with the other values. The package checks that the ticket exists.
    public long addComment(long ticketId, String author, String body) {
        Map<String, Object> out = addComment.execute(ticketId, author, body);
        return ((Number) out.get("P_COMMENT_ID")).longValue();
    }

    // One call, two cursors. Each comes back in the Map as a List, under its own name.
    @SuppressWarnings("unchecked")
    public TicketWithComments getTicket(long ticketId) {
        Map<String, Object> out = getTicket.execute(ticketId);
        List<TicketResponse> ticket = (List<TicketResponse>) out.get("P_TICKET");
        List<CommentResponse> comments = (List<CommentResponse>) out.get("P_COMMENTS");
        return new TicketWithComments(ticket.get(0), comments);
    }

    // A ticket and one of its comments. The comment is null for a ticket that has none.
    private record JoinedRow(TicketResponse ticket, CommentResponse comment) {
    }

    // One call, one cursor, many rows. The rows come ordered by ticket, and a ticket
    // with 3 comments has 3 rows. Group them back into one object for each ticket.
    @SuppressWarnings("unchecked")
    public List<TicketWithComments> findTicketsWithComments(String status) {
        Map<String, Object> out = findTicketsWithComments.execute(status);
        List<JoinedRow> rows = (List<JoinedRow>) out.get("P_ROWS");

        Map<Long, TicketWithComments> tickets = new LinkedHashMap<>();
        for (JoinedRow row : rows) {
            TicketWithComments entry = tickets.computeIfAbsent(row.ticket().id(),
                    id -> new TicketWithComments(row.ticket(), new ArrayList<>()));
            if (row.comment() != null) {
                entry.comments().add(row.comment());
            }
        }
        return new ArrayList<>(tickets.values());
    }

    private TicketResponse toTicket(ResultSet rs, int rowNumber) throws SQLException {
        return new TicketResponse(
                rs.getLong("ID"),
                rs.getString("TITLE"),
                rs.getString("DESCRIPTION"),
                rs.getString("STATUS"),
                rs.getString("PRIORITY"),
                rs.getTimestamp("CREATED_AT").toLocalDateTime(),
                rs.getTimestamp("DUE_BY").toLocalDateTime());
    }

    private CommentResponse toComment(ResultSet rs, int rowNumber) throws SQLException {
        return new CommentResponse(
                rs.getLong("ID"),
                rs.getString("AUTHOR"),
                rs.getString("BODY"),
                rs.getTimestamp("CREATED_AT").toLocalDateTime());
    }

    // The ticket columns are read as before. The comment columns are NULL when the ticket has no comment:
    // getLong gives 0 for a NULL, so ask wasNull() straight after it.
    private JoinedRow toJoinedRow(ResultSet rs, int rowNumber) throws SQLException {
        long commentId = rs.getLong("COMMENT_ID");
        CommentResponse comment = null;
        if (!rs.wasNull()) {
            comment = new CommentResponse(
                    commentId,
                    rs.getString("AUTHOR"),
                    rs.getString("BODY"),
                    rs.getTimestamp("COMMENT_AT").toLocalDateTime());
        }
        return new JoinedRow(toTicket(rs, rowNumber), comment);
    }
}
