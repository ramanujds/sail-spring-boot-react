package com.bsp.supportassistant.repository;

import com.bsp.supportassistant.dto.PageResponse;
import com.bsp.supportassistant.dto.TicketResponse;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.simple.SimpleJdbcCall;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

// Calls the programs of the PL/SQL package SUPPORT_TICKET_PKG. There is no entity and no JPA here.
@Repository
public class TicketRepository {

    private static final String PACKAGE = "SUPPORT_TICKET_PKG";

    private final SimpleJdbcCall openCount;
    private final SimpleJdbcCall createTicket;
    private final SimpleJdbcCall changeStatus;
    private final SimpleJdbcCall findTickets;
    private final SimpleJdbcCall findPage;

    public TicketRepository(JdbcTemplate jdbcTemplate) {
        this.openCount = new SimpleJdbcCall(jdbcTemplate)
                .withCatalogName(PACKAGE).withFunctionName("OPEN_COUNT");
        this.createTicket = new SimpleJdbcCall(jdbcTemplate)
                .withCatalogName(PACKAGE).withProcedureName("CREATE_TICKET");
        this.changeStatus = new SimpleJdbcCall(jdbcTemplate)
                .withCatalogName(PACKAGE).withProcedureName("CHANGE_STATUS");
        // P_TICKETS is the SYS_REFCURSOR. Each row of it is turned into a TicketResponse by toTicket.
        this.findTickets = new SimpleJdbcCall(jdbcTemplate)
                .withCatalogName(PACKAGE).withProcedureName("FIND_TICKETS")
                .returningResultSet("P_TICKETS", this::toTicket);
        // The same row mapper. P_TOTAL, the second OUT parameter, comes back in the same Map.
        this.findPage = new SimpleJdbcCall(jdbcTemplate)
                .withCatalogName("SUPPORT_TICKET_PAGE_PKG").withProcedureName("FIND_PAGE")
                .returningResultSet("P_TICKETS", this::toTicket);
    }

    // A function: the values go in order, and the return value is read as a Number.
    public int openCount(String priority) {
        Number count = openCount.executeFunction(Number.class, priority);
        return count.intValue();
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

    // One page of tickets. sortColumn is DUE_BY or CREATED_AT, direction is ASC or DESC.
    @SuppressWarnings("unchecked")
    public PageResponse<TicketResponse> findPage(String status, int page, int size,
                                                 String sortColumn, String direction) {
        Map<String, Object> out = findPage.execute(status, page, size, sortColumn, direction);
        List<TicketResponse> tickets = (List<TicketResponse>) out.get("P_TICKETS");
        long total = ((Number) out.get("P_TOTAL")).longValue();
        // 12 tickets in pages of 5 are 3 pages: the last one holds 2.
        int totalPages = (int) ((total + size - 1) / size);
        return new PageResponse<>(tickets, page, size, total, totalPages);
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
}
