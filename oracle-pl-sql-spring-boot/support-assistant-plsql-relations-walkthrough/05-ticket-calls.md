# Step 5 — Ticket calls

> [Back to index](README.md) · Previous: [Project and connection](04-project-and-connection.md) · Next: [Add a comment](06-add-a-comment.md)

## Goal

Create a ticket, list tickets, and change the status of a ticket. These are the parent's own calls, and none of them touches a comment.

## Why this matters

You start with the parent alone, so that the comment steps add only what is new. These three calls are the same as in the first project: one `SimpleJdbcCall` for each program, a `Map` for the `OUT` value, and a row mapper for the cursor. If a step feels familiar, that is the point. Relations add a few ideas. They do not change how a call is made.

### 5.1 The records

Records for the requests and the answers:

```java
public record CreateTicketRequest(String title, String description, String priority) { }

public record TicketCreated(long id) { }

public record StatusRequest(String status) { }
```

`TicketResponse` is one row of the ticket cursor. It has no field for comments, because a plain list of tickets does not carry them:

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

### 5.2 The repository

Build three calls in the constructor. The package name is a constant, so it is written once:

```java
this.createTicket = new SimpleJdbcCall(jdbcTemplate)
        .withCatalogName(PACKAGE).withProcedureName("CREATE_TICKET");
this.findTickets = new SimpleJdbcCall(jdbcTemplate)
        .withCatalogName(PACKAGE).withProcedureName("FIND_TICKETS")
        .returningResultSet("P_TICKETS", this::toTicket);
```

`CHANGE_STATUS` is built the same way as `CREATE_TICKET`. The methods read the answer:

```java
public long createTicket(String title, String description, String priority) {
    Map<String, Object> out = createTicket.execute(title, description, priority);
    return ((Number) out.get("P_TICKET_ID")).longValue();
}

public void changeStatus(long ticketId, String newStatus) {
    changeStatus.execute(ticketId, newStatus);
}
```

The row mapper `toTicket` reads one row into a `TicketResponse`. You will use it again in steps 7 and 8, for the parent row of every cursor. See the checkpoint for the full class.

### 5.3 The service and the controller

The service passes each call on. The controller has three routes:

```java
@GetMapping
public List<TicketResponse> find(@RequestParam(required = false) String status) { ... }

@PostMapping
@ResponseStatus(HttpStatus.CREATED)
public TicketCreated create(@RequestBody CreateTicketRequest request) { ... }

@PatchMapping("/{id}/status")
@ResponseStatus(HttpStatus.NO_CONTENT)
public void changeStatus(@PathVariable long id, @RequestBody StatusRequest request) { ... }
```

## Try it

Run the application. Create three tickets:

```bash
curl -X POST http://localhost:8080/api/tickets -H "Content-Type: application/json" \
  -d '{"title":"Printer is jammed","description":"Second floor printer","priority":"HIGH"}'
curl -X POST http://localhost:8080/api/tickets -H "Content-Type: application/json" \
  -d '{"title":"VPN drops every hour","priority":"MEDIUM"}'
curl -X POST http://localhost:8080/api/tickets -H "Content-Type: application/json" \
  -d '{"title":"Reset my password","priority":"LOW"}'
```

Expected, one line for each:

```json
{"id":1}
{"id":2}
{"id":3}
```

Move ticket 3 one step, and list the tickets that are in progress:

```bash
curl -X PATCH http://localhost:8080/api/tickets/3/status -H "Content-Type: application/json" \
  -d '{"status":"IN_PROGRESS"}'
curl "http://localhost:8080/api/tickets?status=IN_PROGRESS"
```

Expected: the `PATCH` prints nothing (status `204`). The list holds ticket 3 only:

```json
[{"id":3,"title":"Reset my password","description":null,"status":"IN_PROGRESS","priority":"LOW",
  "createdAt":"2026-10-17T02:09:44.892087","dueBy":"2026-10-24T02:09:44.892087"}]
```

The times on your screen are different. `dueBy` is seven days after `createdAt`, because the priority is `LOW`. The package worked that out.

## Checkpoint

`src/main/java/com/bsp/supportassistant/dto/CreateTicketRequest.java` - Matches [../support-assistant-plsql-relations/src/main/java/com/bsp/supportassistant/dto/CreateTicketRequest.java](../support-assistant-plsql-relations/src/main/java/com/bsp/supportassistant/dto/CreateTicketRequest.java) exactly.

<details>
<summary>Full <code>src/main/java/com/bsp/supportassistant/dto/CreateTicketRequest.java</code></summary>

```java
package com.bsp.supportassistant.dto;

// The body of POST /api/tickets.
public record CreateTicketRequest(String title, String description, String priority) {
}
```

</details>

`src/main/java/com/bsp/supportassistant/dto/TicketCreated.java` - Matches [../support-assistant-plsql-relations/src/main/java/com/bsp/supportassistant/dto/TicketCreated.java](../support-assistant-plsql-relations/src/main/java/com/bsp/supportassistant/dto/TicketCreated.java) exactly.

<details>
<summary>Full <code>src/main/java/com/bsp/supportassistant/dto/TicketCreated.java</code></summary>

```java
package com.bsp.supportassistant.dto;

// The answer to a request that creates a ticket: the id that the PL/SQL procedure gave back.
public record TicketCreated(long id) {
}
```

</details>

`src/main/java/com/bsp/supportassistant/dto/StatusRequest.java` - Matches [../support-assistant-plsql-relations/src/main/java/com/bsp/supportassistant/dto/StatusRequest.java](../support-assistant-plsql-relations/src/main/java/com/bsp/supportassistant/dto/StatusRequest.java) exactly.

<details>
<summary>Full <code>src/main/java/com/bsp/supportassistant/dto/StatusRequest.java</code></summary>

```java
package com.bsp.supportassistant.dto;

// The body of PATCH /api/tickets/7/status.
public record StatusRequest(String status) {
}
```

</details>

`src/main/java/com/bsp/supportassistant/dto/TicketResponse.java` - Matches [../support-assistant-plsql-relations/src/main/java/com/bsp/supportassistant/dto/TicketResponse.java](../support-assistant-plsql-relations/src/main/java/com/bsp/supportassistant/dto/TicketResponse.java) exactly.

<details>
<summary>Full <code>src/main/java/com/bsp/supportassistant/dto/TicketResponse.java</code></summary>

```java
package com.bsp.supportassistant.dto;

import java.time.LocalDateTime;

// One row of the ticket cursors. It is a plain record, not a JPA entity.
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

// Calls the programs of the PL/SQL package DESK_TICKET_PKG. There is no entity and no JPA here.
@Repository
public class TicketRepository {

    private static final String PACKAGE = "DESK_TICKET_PKG";

    private final SimpleJdbcCall createTicket;
    private final SimpleJdbcCall changeStatus;
    private final SimpleJdbcCall findTickets;

    public TicketRepository(JdbcTemplate jdbcTemplate) {
        this.createTicket = new SimpleJdbcCall(jdbcTemplate)
                .withCatalogName(PACKAGE).withProcedureName("CREATE_TICKET");
        this.changeStatus = new SimpleJdbcCall(jdbcTemplate)
                .withCatalogName(PACKAGE).withProcedureName("CHANGE_STATUS");
        // P_TICKETS is the SYS_REFCURSOR. Each row of it is turned into a TicketResponse by toTicket.
        this.findTickets = new SimpleJdbcCall(jdbcTemplate)
                .withCatalogName(PACKAGE).withProcedureName("FIND_TICKETS")
                .returningResultSet("P_TICKETS", this::toTicket);
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

    public long create(CreateTicketRequest request) {
        return ticketRepository.createTicket(request.title(), request.description(), request.priority());
    }

    public void changeStatus(long ticketId, String newStatus) {
        ticketRepository.changeStatus(ticketId, newStatus);
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
import com.bsp.supportassistant.dto.StatusRequest;
import com.bsp.supportassistant.dto.TicketCreated;
import com.bsp.supportassistant.dto.TicketResponse;
import com.bsp.supportassistant.service.TicketService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
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

    // Calls the procedure CHANGE_STATUS.
    @PatchMapping("/{id}/status")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void changeStatus(@PathVariable long id, @RequestBody StatusRequest request) {
        ticketService.changeStatus(id, request.status());
    }
}
```

</details>

## Common mistakes

| Symptom | Cause | Fix |
|---|---|---|
| `415 Unsupported Media Type` from `curl` | The `Content-Type: application/json` header is missing | Add the header |
| `ORA-01017: invalid username/password` in the log, and `500` | `DB_USERNAME` or `DB_PASSWORD` is not set where the application runs | Set both, and restart |
| `ArrayIndexOutOfBoundsException: Index 2 out of bounds for length 2` in the log, and `500` | `execute(...)` got fewer values than the procedure has `IN` parameters | Pass one value for each `IN` parameter, in the order of the specification |
| The list is `[]` for `?status=open` | The comparison is case sensitive. The status is `OPEN` | Send the status in upper case |

Next: **[Add a comment](06-add-a-comment.md)**.
