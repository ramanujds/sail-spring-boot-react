# Step 5 — Call a procedure with an OUT parameter

> [Back to index](README.md) · Previous: [Call a function](04-call-a-function.md) · Next: [Read a result set](06-read-a-result-set.md)

## Goal

`POST /api/tickets` calls `CREATE_TICKET` and returns the id that the procedure gives back.

## Why this matters

A procedure has no return value. It gives its answer in an `OUT` parameter, and Spring puts it in a `Map` under the parameter name in upper case. The values you pass go in the order of the `IN` parameters, and the `OUT` parameter is not passed.

### 5.1 Two records

A record only holds values. One is the request body, one is the answer:

```java
public record CreateTicketRequest(String title, String description, String priority) {
}
```

```java
public record TicketCreated(long id) {
}
```

Put both in the package `dto`.

### 5.2 The repository

Add a field and build it in the constructor:

```java
private final SimpleJdbcCall createTicket;
```

```java
this.createTicket = new SimpleJdbcCall(jdbcTemplate)
        .withCatalogName(PACKAGE).withProcedureName("CREATE_TICKET");
```

Add the method:

```java
public long createTicket(String title, String description, String priority) {
    Map<String, Object> out = createTicket.execute(title, description, priority);
    return ((Number) out.get("P_TICKET_ID")).longValue();
}
```

### 5.3 The service and the controller

```java
public long create(CreateTicketRequest request) {
    return ticketRepository.createTicket(request.title(), request.description(), request.priority());
}
```

```java
@PostMapping
@ResponseStatus(HttpStatus.CREATED)
public TicketCreated create(@RequestBody CreateTicketRequest request) {
    return new TicketCreated(ticketService.create(request));
}
```

## Try it

Restart, then create three tickets and count again:

```bash
curl -X POST http://localhost:8080/api/tickets -H "Content-Type: application/json" \
  -d '{"title":"Printer is jammed","description":"Second floor printer","priority":"HIGH"}'
curl -X POST http://localhost:8080/api/tickets -H "Content-Type: application/json" \
  -d '{"title":"VPN drops every hour","priority":"MEDIUM"}'
curl -X POST http://localhost:8080/api/tickets -H "Content-Type: application/json" \
  -d '{"title":"Reset my password","priority":"LOW"}'
curl "http://localhost:8080/api/tickets/open-count?priority=HIGH"
```

Expected:

```text
{"id":1}
{"id":2}
{"id":3}
1
```

## Checkpoint

`src/main/java/com/bsp/supportassistant/dto/CreateTicketRequest.java` - Matches [../support-assistant-plsql/src/main/java/com/bsp/supportassistant/dto/CreateTicketRequest.java](../support-assistant-plsql/src/main/java/com/bsp/supportassistant/dto/CreateTicketRequest.java) exactly.

<details>
<summary>Full <code>src/main/java/com/bsp/supportassistant/dto/CreateTicketRequest.java</code></summary>

```java
package com.bsp.supportassistant.dto;

// The body of POST /api/tickets.
public record CreateTicketRequest(String title, String description, String priority) {
}
```

</details>

`src/main/java/com/bsp/supportassistant/dto/TicketCreated.java` - Matches [../support-assistant-plsql/src/main/java/com/bsp/supportassistant/dto/TicketCreated.java](../support-assistant-plsql/src/main/java/com/bsp/supportassistant/dto/TicketCreated.java) exactly.

<details>
<summary>Full <code>src/main/java/com/bsp/supportassistant/dto/TicketCreated.java</code></summary>

```java
package com.bsp.supportassistant.dto;

// The answer to POST /api/tickets: the id that the PL/SQL procedure gave back.
public record TicketCreated(long id) {
}
```

</details>

`src/main/java/com/bsp/supportassistant/repository/TicketRepository.java` - Grows in a later step.

<details>
<summary>Full <code>src/main/java/com/bsp/supportassistant/repository/TicketRepository.java</code></summary>

```java
package com.bsp.supportassistant.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.simple.SimpleJdbcCall;
import org.springframework.stereotype.Repository;

import java.util.Map;

// Calls the programs of the PL/SQL package SUPPORT_TICKET_PKG. There is no entity and no JPA here.
@Repository
public class TicketRepository {

    private static final String PACKAGE = "SUPPORT_TICKET_PKG";

    private final SimpleJdbcCall openCount;
    private final SimpleJdbcCall createTicket;

    public TicketRepository(JdbcTemplate jdbcTemplate) {
        this.openCount = new SimpleJdbcCall(jdbcTemplate)
                .withCatalogName(PACKAGE).withFunctionName("OPEN_COUNT");
        this.createTicket = new SimpleJdbcCall(jdbcTemplate)
                .withCatalogName(PACKAGE).withProcedureName("CREATE_TICKET");
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
}
```

</details>

`src/main/java/com/bsp/supportassistant/service/TicketService.java` - Grows in a later step.

<details>
<summary>Full <code>src/main/java/com/bsp/supportassistant/service/TicketService.java</code></summary>

```java
package com.bsp.supportassistant.service;

import com.bsp.supportassistant.dto.CreateTicketRequest;
import com.bsp.supportassistant.repository.TicketRepository;
import org.springframework.stereotype.Service;

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
import com.bsp.supportassistant.service.TicketService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

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
| `NullPointerException` on the line that reads the `OUT` value | The key was written in lower case: `out.get("p_ticket_id")` returns `null` | Use the upper-case name: `P_TICKET_ID` |
| `415 Unsupported Media Type` | The request has no `Content-Type: application/json` header | Add `-H "Content-Type: application/json"` to the `curl` command |
| `BadSqlGrammarException` and `PLS-00302: component 'CREATE_TICKET_X' must be declared` | The package exists, but the program name is wrong | Check the name in the specification |

Next: **[Read a result set](06-read-a-result-set.md)**.
