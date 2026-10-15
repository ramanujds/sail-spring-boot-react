# Step 6 — Read a result set

> [Back to index](README.md) · Previous: [Call a procedure with an OUT parameter](05-procedure-with-out-parameter.md) · Next: [Change the status](07-change-status.md)

## Goal

`GET /api/tickets?status=OPEN` reads the `SYS_REFCURSOR` of `FIND_TICKETS` into a JSON list.

## Why this matters

A cursor is a set of rows that the procedure has opened. Java has to turn each row into an object. A row mapper does this: Spring calls it once for each row, and you build one record from the columns. A `null` status asks the procedure for every ticket.

### 6.1 The response record

One row of the cursor:

```java
public record TicketResponse(
        long id,
        String title,
        String description,
        String status,
        String priority,
        LocalDateTime createdAt,
        LocalDateTime dueBy) {
}
```

### 6.2 The repository

`returningResultSet` names the cursor parameter and gives the row mapper:

```java
this.findTickets = new SimpleJdbcCall(jdbcTemplate)
        .withCatalogName(PACKAGE).withProcedureName("FIND_TICKETS")
        .returningResultSet("P_TICKETS", this::toTicket);
```

```java
@SuppressWarnings("unchecked")
public List<TicketResponse> findTickets(String status) {
    Map<String, Object> out = findTickets.execute(status);
    return (List<TicketResponse>) out.get("P_TICKETS");
}
```

The row mapper builds one record from the current row:

```java
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
```

### 6.3 The service and the controller

```java
public List<TicketResponse> find(String status) {
    return ticketRepository.findTickets(status);
}
```

```java
@GetMapping
public List<TicketResponse> find(@RequestParam(required = false) String status) {
    return ticketService.find(status);
}
```

## Try it

Restart. If the tickets of step 5 are gone, create them again. Then:

```bash
curl "http://localhost:8080/api/tickets?status=OPEN"
```

Expected: a list of the three tickets. The first is:

```json
{"id":1,"title":"Printer is jammed","description":"Second floor printer","status":"OPEN",
 "priority":"HIGH","createdAt":"2026-10-16T10:15:00.123456","dueBy":"2026-10-17T10:15:00.123456"}
```

`dueBy` is one day after `createdAt`. The package worked that out, not Java.

## Checkpoint

`src/main/java/com/bsp/supportassistant/dto/TicketResponse.java` - Matches [../support-assistant-plsql/src/main/java/com/bsp/supportassistant/dto/TicketResponse.java](../support-assistant-plsql/src/main/java/com/bsp/supportassistant/dto/TicketResponse.java) exactly.

<details>
<summary>Full <code>src/main/java/com/bsp/supportassistant/dto/TicketResponse.java</code></summary>

```java
package com.bsp.supportassistant.dto;

import java.time.LocalDateTime;

// One row of the result set that FIND_TICKETS opens. It is a plain record, not a JPA entity.
public record TicketResponse(
        long id,
        String title,
        String description,
        String status,
        String priority,
        LocalDateTime createdAt,
        LocalDateTime dueBy) {
}
```

</details>

`src/main/java/com/bsp/supportassistant/repository/TicketRepository.java` - Grows in a later step.

<details>
<summary>Full <code>src/main/java/com/bsp/supportassistant/repository/TicketRepository.java</code></summary>

```java
package com.bsp.supportassistant.repository;

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
    private final SimpleJdbcCall findTickets;

    public TicketRepository(JdbcTemplate jdbcTemplate) {
        this.openCount = new SimpleJdbcCall(jdbcTemplate)
                .withCatalogName(PACKAGE).withFunctionName("OPEN_COUNT");
        this.createTicket = new SimpleJdbcCall(jdbcTemplate)
                .withCatalogName(PACKAGE).withProcedureName("CREATE_TICKET");
        // P_TICKETS is the SYS_REFCURSOR. Each row of it is turned into a TicketResponse by toTicket.
        this.findTickets = new SimpleJdbcCall(jdbcTemplate)
                .withCatalogName(PACKAGE).withProcedureName("FIND_TICKETS")
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

    // A procedure with a result set. A null status asks for every ticket.
    @SuppressWarnings("unchecked")
    public List<TicketResponse> findTickets(String status) {
        Map<String, Object> out = findTickets.execute(status);
        return (List<TicketResponse>) out.get("P_TICKETS");
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

`src/main/java/com/bsp/supportassistant/service/TicketService.java` - Grows in a later step.

<details>
<summary>Full <code>src/main/java/com/bsp/supportassistant/service/TicketService.java</code></summary>

```java
package com.bsp.supportassistant.service;

import com.bsp.supportassistant.dto.CreateTicketRequest;
import com.bsp.supportassistant.dto.TicketResponse;
import com.bsp.supportassistant.repository.TicketRepository;
import org.springframework.stereotype.Service;

import java.util.List;

// The business rules are in the PL/SQL package. The service decides what happens together.
@Service
public class TicketService {

    private final TicketRepository ticketRepository;

    public TicketService(TicketRepository ticketRepository) {
        this.ticketRepository = ticketRepository;
    }

    public int openCount(String priority) {
        return ticketRepository.openCount(priority);
    }

    public long create(CreateTicketRequest request) {
        return ticketRepository.createTicket(request.title(), request.description(), request.priority());
    }

    public List<TicketResponse> find(String status) {
        return ticketRepository.findTickets(status);
    }
}
```

</details>

`src/main/java/com/bsp/supportassistant/api/TicketController.java` - Grows in a later step.

<details>
<summary>Full <code>src/main/java/com/bsp/supportassistant/api/TicketController.java</code></summary>

```java
package com.bsp.supportassistant.api;

import com.bsp.supportassistant.dto.CreateTicketRequest;
import com.bsp.supportassistant.dto.TicketCreated;
import com.bsp.supportassistant.dto.TicketResponse;
import com.bsp.supportassistant.service.TicketService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/tickets")
public class TicketController {

    private final TicketService ticketService;

    public TicketController(TicketService ticketService) {
        this.ticketService = ticketService;
    }

    // Calls the function OPEN_COUNT.
    @GetMapping("/open-count")
    public int openCount(@RequestParam String priority) {
        return ticketService.openCount(priority);
    }

    // Calls the procedure FIND_TICKETS. The status is optional.
    @GetMapping
    public List<TicketResponse> find(@RequestParam(required = false) String status) {
        return ticketService.find(status);
    }

    // Calls the procedure CREATE_TICKET.
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TicketCreated create(@RequestBody CreateTicketRequest request) {
        return new TicketCreated(ticketService.create(request));
    }
}
```

</details>

## Common mistakes

| Symptom | Cause | Fix |
|---|---|---|
| The list is `[]` for `?status=open` | The comparison in the package is case sensitive. The status is `OPEN` | Send the status in upper case |
| The list is `[]` although tickets exist | The name in `returningResultSet("P_TICKET", ...)` does not match the cursor parameter `P_TICKETS`. There is no error | Copy the parameter name from the specification, in upper case |
| `200` with an empty body | `out.get("p_tickets")` was written in lower case, so it returns `null` | Use the upper-case name: `P_TICKETS` |

Next: **[Change the status](07-change-status.md)**.
