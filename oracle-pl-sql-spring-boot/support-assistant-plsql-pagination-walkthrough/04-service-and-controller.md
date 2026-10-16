# Step 4 — The service and the controller

> [Back to index](README.md) · Previous: [PageResponse and the repository](03-page-response-and-repository.md) · Next: [Test the pages](05-test-the-pages.md)

## Goal

`GET /api/tickets` takes `page`, `size`, `sort` and `direction`, and returns a `PageResponse`. A value that is not allowed answers `400`.

## Why this matters

The four values come from the caller, as text in the URL. The service checks them before the package is called:

| Value | Rule | Why |
|---|---|---|
| `page` | `0` or more | There is no page `-1` |
| `size` | `1` to `50` | `size=100000` would read the whole table again |
| `sort` | `dueBy` or `createdAt` | The caller uses the JSON names. The service turns them into column names. Any other name is refused |
| `direction` | `asc` or `desc` | Upper or lower case |

### 4.1 The allowed sorts and the limit

In `TicketService`, above the fields:

```java
// The fields a list may be sorted by, and the column each one stands for.
private static final Map<String, String> SORT_COLUMNS = Map.of("dueBy", "DUE_BY", "createdAt", "CREATED_AT");
private static final int MAX_SIZE = 50;
```

The imports are `java.util.Map` and `com.bsp.supportassistant.dto.PageResponse`.

### 4.2 The checks

Replace the method `find` with `findPage`:

```java
// Checks what the caller asked for, then lets the package read the page.
public PageResponse<TicketResponse> findPage(String status, int page, int size, String sort, String direction) {
    if (page < 0) {
        throw new InvalidRequestException("page must be 0 or more");
    }
    if (size < 1 || size > MAX_SIZE) {
        throw new InvalidRequestException("size must be from 1 to " + MAX_SIZE);
    }
    String column = SORT_COLUMNS.get(sort);
    if (column == null) {
        throw new InvalidRequestException("Cannot sort by '" + sort + "'. Use dueBy or createdAt");
    }
    if (!direction.equalsIgnoreCase("asc") && !direction.equalsIgnoreCase("desc")) {
        throw new InvalidRequestException("direction must be asc or desc");
    }
    return ticketRepository.findPage(status, page, size, column, direction.toUpperCase());
}
```

`InvalidRequestException` already answers `400`, through the handler from the first project.

### 4.3 The endpoint

In `TicketController`, replace the method `find`:

```java
// GET /api/tickets?status=OPEN&page=0&size=5&sort=dueBy&direction=asc returns one page.
// Every parameter is optional. Without them you get the first 5 tickets, the most urgent first.
@GetMapping
public PageResponse<TicketResponse> find(@RequestParam(required = false) String status,
                                         @RequestParam(defaultValue = "0") int page,
                                         @RequestParam(defaultValue = "5") int size,
                                         @RequestParam(defaultValue = "dueBy") String sort,
                                         @RequestParam(defaultValue = "asc") String direction) {
    return ticketService.findPage(status, page, size, sort, direction);
}
```

`defaultValue` is used when the caller does not send the parameter. Add the import of `PageResponse`, and remove the import of `java.util.List`: nothing in the controller uses it now.

## Try it

```bash
mvn -q clean verify
java -jar target/support-assistant-plsql-pagination-0.0.1-SNAPSHOT.jar
```

In a second terminal:

```bash
curl "http://localhost:8080/api/tickets?size=2"
```

Expected, with only the 12 sample tickets:

```json
{"content":[{"id":3,"title":"Sample ticket 3","description":null,"status":"OPEN","priority":"HIGH","createdAt":"...","dueBy":"..."},
            {"id":6,"title":"Sample ticket 6","description":null,"status":"OPEN","priority":"HIGH","createdAt":"...","dueBy":"..."}],
 "page":0,"size":2,"totalElements":12,"totalPages":6}
```

Then try these:

| Call | Expected (12 sample tickets) |
|---|---|
| `/api/tickets` | 5 tickets: ids 3, 6, 9, 12 (`HIGH`), then 1. `totalPages` is `3` |
| `/api/tickets?page=1` | Ids 4, 7, 10, 2, 5 |
| `/api/tickets?page=2` | Ids 8, 11, both `LOW` |
| `/api/tickets?page=3` | `"content":[]`. `totalElements` is still `12` |
| `/api/tickets?size=4&sort=createdAt&direction=desc` | Ids 12, 11, 10, 9: the newest first |
| `/api/tickets?status=OPEN&size=20` | All 12, on one page. `totalPages` is `1` |
| `/api/tickets?status=CLOSED` | `"content":[]`, `"totalElements":0`, `"totalPages":0` |
| `/api/tickets?sort=colour` | `400` with `Cannot sort by 'colour'. Use dueBy or createdAt` |
| `/api/tickets?size=100` | `400` with `size must be from 1 to 50` |
| `/api/tickets?page=-1` | `400` with `page must be 0 or more` |
| `/api/tickets?direction=up` | `400` with `direction must be asc or desc` |

## Checkpoint

`src/main/java/com/bsp/supportassistant/service/TicketService.java` - Matches [../support-assistant-plsql-pagination/src/main/java/com/bsp/supportassistant/service/TicketService.java](../support-assistant-plsql-pagination/src/main/java/com/bsp/supportassistant/service/TicketService.java) exactly.

<details>
<summary>Full <code>src/main/java/com/bsp/supportassistant/service/TicketService.java</code></summary>

```java
package com.bsp.supportassistant.service;

import com.bsp.supportassistant.dto.CreateTicketRequest;
import com.bsp.supportassistant.dto.PageResponse;
import com.bsp.supportassistant.dto.TicketResponse;
import com.bsp.supportassistant.exception.InvalidRequestException;
import com.bsp.supportassistant.repository.TicketRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

// The business rules are in the PL/SQL package. The service decides what happens together.
@Service
public class TicketService {

    // The fields a list may be sorted by, and the column each one stands for.
    private static final Map<String, String> SORT_COLUMNS = Map.of("dueBy", "DUE_BY", "createdAt", "CREATED_AT");
    private static final int MAX_SIZE = 50;

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

    public void changeStatus(long ticketId, String newStatus) {
        ticketRepository.changeStatus(ticketId, newStatus);
    }

    // All or nothing: if one ticket cannot change, the tickets before it are put back.
    @Transactional
    public void changeStatus(List<Long> ticketIds, String newStatus) {
        if (ticketIds == null || ticketIds.isEmpty()) {
            throw new InvalidRequestException("ticketIds must hold at least one id");
        }
        for (Long ticketId : ticketIds) {
            ticketRepository.changeStatus(ticketId, newStatus);
        }
    }

    // Checks what the caller asked for, then lets the package read the page.
    public PageResponse<TicketResponse> findPage(String status, int page, int size, String sort, String direction) {
        if (page < 0) {
            throw new InvalidRequestException("page must be 0 or more");
        }
        if (size < 1 || size > MAX_SIZE) {
            throw new InvalidRequestException("size must be from 1 to " + MAX_SIZE);
        }
        String column = SORT_COLUMNS.get(sort);
        if (column == null) {
            throw new InvalidRequestException("Cannot sort by '" + sort + "'. Use dueBy or createdAt");
        }
        if (!direction.equalsIgnoreCase("asc") && !direction.equalsIgnoreCase("desc")) {
            throw new InvalidRequestException("direction must be asc or desc");
        }
        return ticketRepository.findPage(status, page, size, column, direction.toUpperCase());
    }
}
```

</details>

`src/main/java/com/bsp/supportassistant/api/TicketController.java` - Matches [../support-assistant-plsql-pagination/src/main/java/com/bsp/supportassistant/api/TicketController.java](../support-assistant-plsql-pagination/src/main/java/com/bsp/supportassistant/api/TicketController.java) exactly.

<details>
<summary>Full <code>src/main/java/com/bsp/supportassistant/api/TicketController.java</code></summary>

```java
package com.bsp.supportassistant.api;

import com.bsp.supportassistant.dto.BulkStatusRequest;
import com.bsp.supportassistant.dto.CreateTicketRequest;
import com.bsp.supportassistant.dto.PageResponse;
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

    // GET /api/tickets?status=OPEN&page=0&size=5&sort=dueBy&direction=asc returns one page.
    // Every parameter is optional. Without them you get the first 5 tickets, the most urgent first.
    @GetMapping
    public PageResponse<TicketResponse> find(@RequestParam(required = false) String status,
                                             @RequestParam(defaultValue = "0") int page,
                                             @RequestParam(defaultValue = "5") int size,
                                             @RequestParam(defaultValue = "dueBy") String sort,
                                             @RequestParam(defaultValue = "asc") String direction) {
        return ticketService.findPage(status, page, size, sort, direction);
    }

    // Calls the procedure CREATE_TICKET.
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TicketCreated create(@RequestBody CreateTicketRequest request) {
        return new TicketCreated(ticketService.create(request));
    }

    // Calls the procedure CHANGE_STATUS once.
    @PatchMapping("/{id}/status")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void changeStatus(@PathVariable long id, @RequestBody StatusRequest request) {
        ticketService.changeStatus(id, request.status());
    }

    // Calls CHANGE_STATUS for each id, in one transaction.
    @PostMapping("/bulk-status")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void changeStatus(@RequestBody BulkStatusRequest request) {
        ticketService.changeStatus(request.ticketIds(), request.status());
    }
}
```

</details>

## Common mistakes

| Symptom | Cause | Fix |
|---|---|---|
| `?page=1` returns the second page | Pages start at `0` | Ask for `page=0` first |
| `?size=abc` answers `400` with no `message` | Spring cannot turn `abc` into an `int`, and answers before your code runs | Send a number |
| `?sort=createdAt&direction=desc` comes back in id order: 1, 2, 3, ... | `direction.toUpperCase()` is missing, so the package received `desc`, which matches no `CASE` line | Pass `direction.toUpperCase()` |
| `mvn` fails with `cannot find symbol ... method find(java.lang.String)` | The controller still calls `ticketService.find`, which is gone | Replace the controller method as in 4.3 |

Next: **[Test the pages](05-test-the-pages.md)**.
