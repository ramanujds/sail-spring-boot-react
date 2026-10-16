# Step 10 — Transactions

> [Back to index](README.md) · Previous: [Error handling](09-error-handling.md) · Next: [Test the relationship](11-test-the-relationship.md)

## Goal

`POST /api/tickets/with-first-comment` creates a ticket and its first comment together. If the comment is refused, there is no ticket either.

## Why this matters

Creating a parent and its child takes two calls: `CREATE_TICKET`, then `ADD_COMMENT`. The second needs the id that the first gives back, so they cannot be one statement. If the second fails, the first has already happened, and a ticket is left behind that the user thought was never created.

| | Without `@Transactional` | With `@Transactional` |
|---|---|---|
| When the data is saved | After each call | When the Java method ends |
| `CREATE_TICKET` works, `ADD_COMMENT` fails | The ticket stays, with no comment | Both are undone |

The programs in the package have no `COMMIT`, so Spring decides.

### 10.1 The request

One body with both parts:

```java
public record CreateTicketWithCommentRequest(
        String title, String description, String priority, String author, String comment) { }
```

### 10.2 The service

`@Transactional` goes on the service method, where the two calls meet. The comment needs the id of the ticket, so the ticket comes first:

```java
@Transactional
public long createWithFirstComment(CreateTicketWithCommentRequest request) {
    long ticketId = ticketRepository.createTicket(request.title(), request.description(), request.priority());
    ticketRepository.addComment(ticketId, request.author(), request.comment());
    return ticketId;
}
```

If `addComment` throws, the exception leaves the method, and Spring undoes everything that this method did. The exception then goes on to the handler of step 9, so the client still gets its `400`.

### 10.3 The controller

```java
@PostMapping("/with-first-comment")
@ResponseStatus(HttpStatus.CREATED)
public TicketCreated createWithFirstComment(@RequestBody CreateTicketWithCommentRequest request) {
    return new TicketCreated(ticketService.createWithFirstComment(request));
}
```

## Try it

Restart the application. A ticket with a good first comment:

```bash
curl -X POST http://localhost:8080/api/tickets/with-first-comment -H "Content-Type: application/json" \
  -d '{"title":"Badge reader is broken","priority":"HIGH","author":"Meera","comment":"Door 2 does not open"}'
```

Expected, status `201`:

```json
{"id":4}
```

Read it back. The comment is there:

```bash
curl http://localhost:8080/api/tickets/4
```

Now a first comment with no text. Send no `comment` field:

```bash
curl -X POST http://localhost:8080/api/tickets/with-first-comment -H "Content-Type: application/json" \
  -d '{"title":"Monitor flickers","priority":"LOW","author":"Meera"}'
```

Expected, status `400`:

```json
{"status":400,"error":"Bad Request","message":"Author and text are required","timestamp":"2026-10-17T02:09:45.850916"}
```

The ticket was created before the comment was refused. Check that it was undone:

```bash
curl http://localhost:8080/api/tickets
```

Expected: tickets 1 to 4 only. "Monitor flickers" is not in the list.

> **Note:** the undone ticket still used a number from the sequence. The next ticket gets id 6, not 5. An Oracle sequence does not go back.

## Checkpoint

`src/main/java/com/bsp/supportassistant/dto/CreateTicketWithCommentRequest.java` - Matches [../support-assistant-plsql-relations/src/main/java/com/bsp/supportassistant/dto/CreateTicketWithCommentRequest.java](../support-assistant-plsql-relations/src/main/java/com/bsp/supportassistant/dto/CreateTicketWithCommentRequest.java) exactly.

<details>
<summary>Full <code>src/main/java/com/bsp/supportassistant/dto/CreateTicketWithCommentRequest.java</code></summary>

```java
package com.bsp.supportassistant.dto;

// The body of POST /api/tickets/with-first-comment: a ticket and its first comment.
public record CreateTicketWithCommentRequest(
        String title,
        String description,
        String priority,
        String author,
        String comment) {
}
```

</details>

`src/main/java/com/bsp/supportassistant/service/TicketService.java` - Matches [../support-assistant-plsql-relations/src/main/java/com/bsp/supportassistant/service/TicketService.java](../support-assistant-plsql-relations/src/main/java/com/bsp/supportassistant/service/TicketService.java) exactly.

<details>
<summary>Full <code>src/main/java/com/bsp/supportassistant/service/TicketService.java</code></summary>

```java
package com.bsp.supportassistant.service;

import com.bsp.supportassistant.dto.CommentRequest;
import com.bsp.supportassistant.dto.CreateTicketRequest;
import com.bsp.supportassistant.dto.CreateTicketWithCommentRequest;
import com.bsp.supportassistant.dto.TicketResponse;
import com.bsp.supportassistant.dto.TicketWithComments;
import com.bsp.supportassistant.repository.TicketRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    // All or nothing: if the comment is refused, the ticket is put back too.
    // The comment needs the id of the ticket, so the ticket must be created first.
    @Transactional
    public long createWithFirstComment(CreateTicketWithCommentRequest request) {
        long ticketId = ticketRepository.createTicket(request.title(), request.description(), request.priority());
        ticketRepository.addComment(ticketId, request.author(), request.comment());
        return ticketId;
    }
}
```

</details>

`src/main/java/com/bsp/supportassistant/api/TicketController.java` - Matches [../support-assistant-plsql-relations/src/main/java/com/bsp/supportassistant/api/TicketController.java](../support-assistant-plsql-relations/src/main/java/com/bsp/supportassistant/api/TicketController.java) exactly.

<details>
<summary>Full <code>src/main/java/com/bsp/supportassistant/api/TicketController.java</code></summary>

```java
package com.bsp.supportassistant.api;

import com.bsp.supportassistant.dto.CommentCreated;
import com.bsp.supportassistant.dto.CommentRequest;
import com.bsp.supportassistant.dto.CreateTicketRequest;
import com.bsp.supportassistant.dto.CreateTicketWithCommentRequest;
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

    // Creates a ticket and its first comment, in one transaction.
    @PostMapping("/with-first-comment")
    @ResponseStatus(HttpStatus.CREATED)
    public TicketCreated createWithFirstComment(@RequestBody CreateTicketWithCommentRequest request) {
        return new TicketCreated(ticketService.createWithFirstComment(request));
    }
}
```

</details>

## Common mistakes

| Symptom | Cause | Fix |
|---|---|---|
| The ticket stays after a refused comment | `@Transactional` is missing, or it is on a method that is called from another method of the same class | Put it on a public service method that the controller calls |
| The ticket stays after an error that is caught | A `try`/`catch` in the method swallowed the exception, so Spring never saw it | Let the exception leave the method |
| A procedure has its own `COMMIT` | The package saved the ticket before Spring could undo it | Leave `COMMIT` out of the package. This package has none |

Next: **[Test the relationship](11-test-the-relationship.md)**.
