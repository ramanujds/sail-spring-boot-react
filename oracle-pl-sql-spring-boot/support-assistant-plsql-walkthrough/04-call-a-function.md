# Step 4 — Call a function

> [Back to index](README.md) · Previous: [Project and connection](03-project-and-connection.md) · Next: [Call a procedure with an OUT parameter](05-procedure-with-out-parameter.md)

## Goal

`GET /api/tickets/open-count?priority=HIGH` calls the function `OPEN_COUNT`.

## Why this matters

This step builds the whole path once, with the smallest program. A function gives back one value. Oracle's `NUMBER` arrives in Java as a `BigDecimal`, so you read it as a `Number` and then ask for an `int`. The later steps add to the same three classes.

### 4.1 The repository

Create `TicketRepository` in the package `repository`. It builds one `SimpleJdbcCall` for the function:

```java
@Repository
public class TicketRepository {

    private static final String PACKAGE = "SUPPORT_TICKET_PKG";

    private final SimpleJdbcCall openCount;

    public TicketRepository(JdbcTemplate jdbcTemplate) {
        this.openCount = new SimpleJdbcCall(jdbcTemplate)
                .withCatalogName(PACKAGE).withFunctionName("OPEN_COUNT");
    }
```

| Part | Meaning |
|---|---|
| `withCatalogName` | The package name |
| `withFunctionName` | The function. Use `withProcedureName` for a procedure |

The method that calls it:

```java
    public int openCount(String priority) {
        Number count = openCount.executeFunction(Number.class, priority);
        return count.intValue();
    }
```

### 4.2 The service

Create `TicketService` in the package `service`. For now it passes the call on:

```java
public int openCount(String priority) {
    return ticketRepository.openCount(priority);
}
```

### 4.3 The controller

Create `TicketController` in the package `api`:

```java
@GetMapping("/open-count")
public int openCount(@RequestParam String priority) {
    return ticketService.openCount(priority);
}
```

## Try it

Restart, then:

```bash
curl "http://localhost:8080/api/tickets/open-count?priority=HIGH"
```

Expected:

```text
0
```

## Checkpoint

`src/main/java/com/bsp/supportassistant/repository/TicketRepository.java` - Grows in a later step.

<details>
<summary>Full <code>src/main/java/com/bsp/supportassistant/repository/TicketRepository.java</code></summary>

```java
package com.bsp.supportassistant.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.simple.SimpleJdbcCall;
import org.springframework.stereotype.Repository;

// Calls the programs of the PL/SQL package SUPPORT_TICKET_PKG. There is no entity and no JPA here.
@Repository
public class TicketRepository {

    private static final String PACKAGE = "SUPPORT_TICKET_PKG";

    private final SimpleJdbcCall openCount;

    public TicketRepository(JdbcTemplate jdbcTemplate) {
        this.openCount = new SimpleJdbcCall(jdbcTemplate)
                .withCatalogName(PACKAGE).withFunctionName("OPEN_COUNT");
    }

    // A function: the values go in order, and the return value is read as a Number.
    public int openCount(String priority) {
        Number count = openCount.executeFunction(Number.class, priority);
        return count.intValue();
    }
}
```

</details>

`src/main/java/com/bsp/supportassistant/service/TicketService.java` - Grows in a later step.

<details>
<summary>Full <code>src/main/java/com/bsp/supportassistant/service/TicketService.java</code></summary>

```java
package com.bsp.supportassistant.service;

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
}
```

</details>

`src/main/java/com/bsp/supportassistant/api/TicketController.java` - Grows in a later step.

<details>
<summary>Full <code>src/main/java/com/bsp/supportassistant/api/TicketController.java</code></summary>

```java
package com.bsp.supportassistant.api;

import com.bsp.supportassistant.service.TicketService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
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
}
```

</details>

## Common mistakes

| Symptom | Cause | Fix |
|---|---|---|
| `ClassCastException: class java.math.BigDecimal cannot be cast to class java.lang.Integer` | The call asked for `Integer.class`. Oracle's `NUMBER` is a `BigDecimal` | Ask for `Number.class`, then `intValue()` |
| `BadSqlGrammarException` and `PLS-00201: identifier 'NO_SUCH_PKG.CREATE_TICKET' must be declared` | The package name in `withCatalogName` is wrong, or the package belongs to another user | Check the spelling. `SELECT OBJECT_NAME, STATUS FROM USER_OBJECTS WHERE OBJECT_TYPE = 'PACKAGE'` lists your packages |
| `PLS-00306: wrong number or types of arguments in call to 'OPEN_COUNT'` | A function was built with `withProcedureName` | Use `withFunctionName` and `executeFunction` |

Next: **[Call a procedure with an OUT parameter](05-procedure-with-out-parameter.md)**.
