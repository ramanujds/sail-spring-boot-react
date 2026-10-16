# Step 3 — PageResponse and the repository

> [Back to index](README.md) · Previous: [The page procedure](02-the-page-procedure.md) · Next: [The service and the controller](04-service-and-controller.md)

## Goal

A record `PageResponse` for one page, and a method `TicketRepository.findPage` that calls `FIND_PAGE` and fills it.

## Why this matters

The caller needs more than the tickets. To show "page 2 of 3" and a "next" button, it needs the page number, the size, and the totals. `PageResponse` holds all of them. The repository reads the two `OUT` values of one call and builds it.

### 3.1 The record

Create `PageResponse` in the package `dto`:

```java
public record PageResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages) {
}
```

`T` stands for the type of the items: here `TicketResponse`. The same record can later hold a page of anything.

### 3.2 The call

In `TicketRepository`, add a field:

```java
private final SimpleJdbcCall findPage;
```

Build it in the constructor. It uses the same row mapper as `findTickets`:

```java
// The same row mapper. P_TOTAL, the second OUT parameter, comes back in the same Map.
this.findPage = new SimpleJdbcCall(jdbcTemplate)
        .withCatalogName("SUPPORT_TICKET_PAGE_PKG").withProcedureName("FIND_PAGE")
        .returningResultSet("P_TICKETS", this::toTicket);
```

### 3.3 The method

```java
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
```

| Line | What it does |
|---|---|
| `execute(status, page, size, sortColumn, direction)` | Passes the five `IN` values, in the order of the specification |
| `out.get("P_TICKETS")` | The rows of the cursor, already turned into `TicketResponse` by `toTicket` |
| `((Number) out.get("P_TOTAL")).longValue()` | An Oracle `NUMBER` arrives as a `BigDecimal`. `Number` accepts it |
| `(total + size - 1) / size` | Divides and rounds up |

## Try it

```bash
mvn -q compile
```

Expected: no output. The controller still calls the old `find`, so the application behaves as before.

## Checkpoint

`src/main/java/com/bsp/supportassistant/dto/PageResponse.java` - Matches [../support-assistant-plsql-pagination/src/main/java/com/bsp/supportassistant/dto/PageResponse.java](../support-assistant-plsql-pagination/src/main/java/com/bsp/supportassistant/dto/PageResponse.java) exactly.

<details>
<summary>Full <code>src/main/java/com/bsp/supportassistant/dto/PageResponse.java</code></summary>

```java
package com.bsp.supportassistant.dto;

import java.util.List;

// One page of a list, and what the caller needs to ask for the next one.
// T stands for the type of the items, here TicketResponse.
public record PageResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages) {
}
```

</details>

`src/main/java/com/bsp/supportassistant/repository/TicketRepository.java` - Matches [../support-assistant-plsql-pagination/src/main/java/com/bsp/supportassistant/repository/TicketRepository.java](../support-assistant-plsql-pagination/src/main/java/com/bsp/supportassistant/repository/TicketRepository.java) exactly.

<details>
<summary>Full <code>src/main/java/com/bsp/supportassistant/repository/TicketRepository.java</code></summary>

```java
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
```

</details>

## Common mistakes

Each row was tried with this project.

| Symptom | Cause | Fix |
|---|---|---|
| `ClassCastException: class java.math.BigDecimal cannot be cast to class java.lang.Integer` | `P_TOTAL` was cast to `Integer` | Cast to `Number`, then call `longValue()` |
| `NullPointerException` on the total | The key was written `"p_total"`. The keys of the `Map` are in upper case | Use `"P_TOTAL"` |
| `ClassCastException: class org.springframework.util.LinkedCaseInsensitiveMap cannot be cast to class ...TicketResponse` | `returningResultSet("P_TICKETS", this::toTicket)` is missing, so each row arrives as a `Map` | Add it to the call |

Next: **[The service and the controller](04-service-and-controller.md)**.
