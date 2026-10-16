# Step 6 — Add a comment

> [Back to index](README.md) · Previous: [Ticket calls](05-ticket-calls.md) · Next: [One ticket with its comments](07-one-ticket-with-comments.md)

## Goal

`POST /api/tickets/1/comments` writes a child row that belongs to ticket 1.

## Why this matters

The link between a comment and its ticket has to come from somewhere. Here it comes from the **URL**: `/api/tickets/1/comments` reads as "the comments of ticket 1". The id is not in the request body, so a request cannot say one thing in the URL and another in the body. The controller passes the id to the package. The package decides whether the ticket exists, whether it is closed, and whether the comment is complete. Java checks none of this.

### 6.1 The records

The body has the author and the text. The ticket id is not in it:

```java
public record CommentRequest(String author, String body) { }

public record CommentCreated(long id) { }
```

### 6.2 The repository

`ADD_COMMENT` has the same shape as `CREATE_TICKET`: values in, one `OUT` id back. Build the call in the constructor:

```java
this.addComment = new SimpleJdbcCall(jdbcTemplate)
        .withCatalogName(PACKAGE).withProcedureName("ADD_COMMENT");
```

The ticket id goes first, because it is the first `IN` parameter of the procedure:

```java
public long addComment(long ticketId, String author, String body) {
    Map<String, Object> out = addComment.execute(ticketId, author, body);
    return ((Number) out.get("P_COMMENT_ID")).longValue();
}
```

### 6.3 The service and the controller

The service passes the request fields on:

```java
public long addComment(long ticketId, CommentRequest request) {
    return ticketRepository.addComment(ticketId, request.author(), request.body());
}
```

The controller reads the id from the path and the comment from the body:

```java
@PostMapping("/{id}/comments")
@ResponseStatus(HttpStatus.CREATED)
public CommentCreated addComment(@PathVariable long id, @RequestBody CommentRequest request) {
    return new CommentCreated(ticketService.addComment(id, request));
}
```

## Try it

Restart the application. Add two comments to ticket 1:

```bash
curl -X POST http://localhost:8080/api/tickets/1/comments -H "Content-Type: application/json" \
  -d '{"author":"Meera","body":"The paper tray is stuck"}'
curl -X POST http://localhost:8080/api/tickets/1/comments -H "Content-Type: application/json" \
  -d '{"author":"Karan","body":"On my way"}'
```

Expected, one line for each:

```json
{"id":1}
{"id":2}
```

Add one to ticket 3 as well. Ticket 2 stays without a comment, on purpose:

```bash
curl -X POST http://localhost:8080/api/tickets/3/comments -H "Content-Type: application/json" \
  -d '{"author":"Meera","body":"Locked out since Monday"}'
```

Expected: `{"id":3}`.

Now a ticket that does not exist:

```bash
curl -X POST http://localhost:8080/api/tickets/999/comments -H "Content-Type: application/json" \
  -d '{"author":"Meera","body":"Hello"}'
```

Expected, for now:

```json
{"timestamp":"2026-10-16T20:38:54.161Z","status":500,"error":"Internal Server Error","path":"/api/tickets/999/comments"}
```

The package refused the call, and nothing turns its message into a good answer yet. The application log shows the real reason:

```text
Error Message = ORA-20003: Ticket 999 does not exist
```

A comment with no text gives the same `500`, with `ORA-20005: Author and text are required` in the log. Step 9 turns both into `404` and `400`.

## Checkpoint

`src/main/java/com/bsp/supportassistant/dto/CommentRequest.java` - Matches [../support-assistant-plsql-relations/src/main/java/com/bsp/supportassistant/dto/CommentRequest.java](../support-assistant-plsql-relations/src/main/java/com/bsp/supportassistant/dto/CommentRequest.java) exactly.

<details>
<summary>Full <code>src/main/java/com/bsp/supportassistant/dto/CommentRequest.java</code></summary>

```java
package com.bsp.supportassistant.dto;

// The body of POST /api/tickets/7/comments. The ticket id is in the URL, so it is not here.
public record CommentRequest(String author, String body) {
}
```

</details>

`src/main/java/com/bsp/supportassistant/dto/CommentCreated.java` - Matches [../support-assistant-plsql-relations/src/main/java/com/bsp/supportassistant/dto/CommentCreated.java](../support-assistant-plsql-relations/src/main/java/com/bsp/supportassistant/dto/CommentCreated.java) exactly.

<details>
<summary>Full <code>src/main/java/com/bsp/supportassistant/dto/CommentCreated.java</code></summary>

```java
package com.bsp.supportassistant.dto;

// The answer to POST /api/tickets/7/comments: the id that the PL/SQL procedure gave back.
public record CommentCreated(long id) {
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
    private final SimpleJdbcCall addComment;

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

import com.bsp.supportassistant.dto.CommentRequest;
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

    public long addComment(long ticketId, CommentRequest request) {
        return ticketRepository.addComment(ticketId, request.author(), request.body());
    }
}
```

</details>

`src/main/java/com/bsp/supportassistant/api/TicketController.java` - Grows in a later step.

<details>
<summary>Full <code>src/main/java/com/bsp/supportassistant/api/TicketController.java</code></summary>

```java
package com.bsp.supportassistant.api;

import com.bsp.supportassistant.dto.CommentCreated;
import com.bsp.supportassistant.dto.CommentRequest;
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

    // Calls the procedure ADD_COMMENT. The comment belongs to the ticket in the URL.
    @PostMapping("/{id}/comments")
    @ResponseStatus(HttpStatus.CREATED)
    public CommentCreated addComment(@PathVariable long id, @RequestBody CommentRequest request) {
        return new CommentCreated(ticketService.addComment(id, request));
    }
}
```

</details>

## Common mistakes

| Symptom | Cause | Fix |
|---|---|---|
| The author is saved as the text, and the text as the author. No error | `execute(ticketId, body, author)`: two `String` values in the wrong order. The compiler cannot tell | Match the order of the `IN` parameters in the specification: ticket, author, body |
| The comment goes to the wrong ticket | `ticketId` was read from the body, or the path variable has another name | Take the id from `@PathVariable`, and check the URL |
| `NullPointerException` on `out.get("P_COMMENT_ID")` | The key was written in lower case. The key is the parameter name in upper case | Use `P_COMMENT_ID` |
| `ORA-02291: ... parent key not found` instead of a clear message | The package inserted without checking the ticket first | Call `lock_ticket` before the `INSERT`, as in step 3 |

Next: **[One ticket with its comments](07-one-ticket-with-comments.md)**.
