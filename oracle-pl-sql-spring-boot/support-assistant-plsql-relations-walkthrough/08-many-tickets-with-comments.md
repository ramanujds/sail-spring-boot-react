# Step 8 — Many tickets with comments

> [Back to index](README.md) · Previous: [One ticket with its comments](07-one-ticket-with-comments.md) · Next: [Error handling](09-error-handling.md)

## Goal

`GET /api/tickets/with-comments` answers with every ticket and its comments, using **one** call to the database.

## Why this matters

You can already show many tickets with their comments: list the tickets, then call `GET_TICKET` for each. That is the **N+1 problem**: one call for the list, plus one more for each of the N tickets. With 50 tickets it is 51 calls. Each call is a round trip to the database, and the cost grows with the list.

| Way | Calls for 50 tickets | Where the loop runs |
|---|---|---|
| `FIND_TICKETS`, then `GET_TICKET` for each | 51 | Java |
| `FIND_TICKETS_WITH_COMMENTS` | 1 | Nowhere. Oracle joins the tables once |

A relationship does not make N+1 happen. A loop does. In JPA the loop is hidden inside lazy loading. Here the loop is in your code, so you can see it and avoid it.

The price of the join is that the answer comes back flat. The next parts turn it into nested objects.

### 8.1 What the cursor returns

The cursor from step 3 gives one row for each comment, and one row for a ticket that has none:

| ID | TITLE | COMMENT_ID | AUTHOR | BODY |
|---|---|---|---|---|
| 1 | Printer is jammed | 1 | Meera | The paper tray is stuck |
| 1 | Printer is jammed | 2 | Karan | On my way |
| 2 | VPN drops every hour | `NULL` | `NULL` | `NULL` |
| 3 | Reset my password | 3 | Meera | Locked out since Monday |

Ticket 1 is in two rows. The repository must turn four rows into three tickets.

### 8.2 One row: a ticket and maybe a comment

A small private record holds one row. The comment is `null` for a ticket that has none:

```java
private record JoinedRow(TicketResponse ticket, CommentResponse comment) {
}
```

The row mapper builds the ticket with `toTicket`, which reads the ticket columns. It builds the comment only when `COMMENT_ID` is not `NULL`:

```java
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
```

`getLong` returns `0` for a `NULL`, which looks like a real value. `wasNull()` asks whether the last column read was `NULL`. Call it straight after the column.

### 8.3 Group the rows

Register the cursor, then read it. A `LinkedHashMap` keeps the tickets in the order they arrive. `computeIfAbsent` creates the ticket's entry the first time its id is seen:

```java
this.findTicketsWithComments = new SimpleJdbcCall(jdbcTemplate)
        .withCatalogName(PACKAGE).withProcedureName("FIND_TICKETS_WITH_COMMENTS")
        .returningResultSet("P_ROWS", this::toJoinedRow);
```

```java
Map<Long, TicketWithComments> tickets = new LinkedHashMap<>();
for (JoinedRow row : rows) {
    TicketWithComments entry = tickets.computeIfAbsent(row.ticket().id(),
            id -> new TicketWithComments(row.ticket(), new ArrayList<>()));
    if (row.comment() != null) {
        entry.comments().add(row.comment());
    }
}
return new ArrayList<>(tickets.values());
```

For each row: find or create the ticket's entry, then add the comment to it if there is one. See the checkpoint for the whole method.

### 8.4 The service and the controller

```java
public List<TicketWithComments> findWithComments(String status) {
    return ticketRepository.findTicketsWithComments(status);
}
```

```java
@GetMapping("/with-comments")
public List<TicketWithComments> findWithComments(@RequestParam(required = false) String status) {
    return ticketService.findWithComments(status);
}
```

The controller now has `GET /{id}` and `GET /with-comments`. Spring chooses the fixed path `with-comments` over the pattern `{id}`, so the two do not clash.

## Try it

Restart the application:

```bash
curl http://localhost:8080/api/tickets/with-comments
```

Expected, formatted for reading. Ticket 1 has two comments, ticket 2 has an empty list, and ticket 3 has one:

```json
[{"ticket":{"id":1,"title":"Printer is jammed", ...},
  "comments":[{"id":1,"author":"Meera","body":"The paper tray is stuck", ...},
              {"id":2,"author":"Karan","body":"On my way", ...}]},
 {"ticket":{"id":2,"title":"VPN drops every hour", ...},
  "comments":[]},
 {"ticket":{"id":3,"title":"Reset my password", ...},
  "comments":[{"id":3,"author":"Meera","body":"Locked out since Monday", ...}]}]
```

The optional status works as before: `?status=OPEN` keeps only the tickets with that status, each with all its comments.

## Checkpoint

`src/main/java/com/bsp/supportassistant/repository/TicketRepository.java` - Matches [../support-assistant-plsql-relations/src/main/java/com/bsp/supportassistant/repository/TicketRepository.java](../support-assistant-plsql-relations/src/main/java/com/bsp/supportassistant/repository/TicketRepository.java) exactly.

<details>
<summary>Full <code>src/main/java/com/bsp/supportassistant/repository/TicketRepository.java</code></summary>

```java
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
import com.bsp.supportassistant.dto.TicketWithComments;
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

    public TicketWithComments get(long ticketId) {
        return ticketRepository.getTicket(ticketId);
    }

    public List<TicketWithComments> findWithComments(String status) {
        return ticketRepository.findTicketsWithComments(status);
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
import com.bsp.supportassistant.dto.TicketWithComments;
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

    // Calls the procedure GET_TICKET: one ticket and its comments.
    @GetMapping("/{id}")
    public TicketWithComments get(@PathVariable long id) {
        return ticketService.get(id);
    }

    // Calls the procedure FIND_TICKETS_WITH_COMMENTS: many tickets, each with its comments.
    // The fixed path "with-comments" wins over "/{id}", so this does not clash with the route above.
    @GetMapping("/with-comments")
    public List<TicketWithComments> findWithComments(@RequestParam(required = false) String status) {
        return ticketService.findWithComments(status);
    }
}
```

</details>

## Common mistakes

| Symptom | Cause | Fix |
|---|---|---|
| `NullPointerException: ... getTimestamp(String) is null` | The comment is built for a ticket that has no comment. `wasNull()` was not asked | Check `rs.wasNull()` right after `getLong("COMMENT_ID")` |
| A ticket without comments is missing from the answer | The package used `JOIN` and not `LEFT JOIN` | Use `LEFT JOIN` in `find_tickets_with_comments` |
| The same ticket appears three times | The rows were turned into tickets one by one, with no grouping | Group by ticket id, as in 8.3 |
| The page is slow with many tickets | A loop calls `getTicket` for each ticket: N+1 | Use `findTicketsWithComments` |

Next: **[Error handling](09-error-handling.md)**.
