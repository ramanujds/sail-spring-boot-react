# Step 4 — Record the failure

> [Back to index](README.md) · Previous: [Call the log procedure](03-call-the-log-procedure.md) · Next: [A transaction of its own](05-a-transaction-of-its-own.md)

## Goal

The bulk change calls `ErrorLogService.record` when a ticket is refused, and then fails as before. You will see that the line does **not** reach the table.

## Why this matters

`@Transactional` on the bulk method puts every call inside it into one transaction. `record` is called from inside that method, so its line is part of the same transaction. When the bulk change rolls back, the line goes with it. Seeing this happen is the reason for step 5.

### 4.1 Receive the new service

In `TicketService`, add a field, and receive it in the constructor:

```java
private final TicketRepository ticketRepository;
private final ErrorLogService errorLogService;

public TicketService(TicketRepository ticketRepository, ErrorLogService errorLogService) {
    this.ticketRepository = ticketRepository;
    this.errorLogService = errorLogService;
}
```

### 4.2 Catch, record, and throw again

Change the loop of the bulk method:

```java
for (Long ticketId : ticketIds) {
    try {
        ticketRepository.changeStatus(ticketId, newStatus);
    } catch (DataAccessException ex) {
        // Saved in a transaction of its own, so the rollback of this method does not remove it.
        errorLogService.record("Bulk change to " + newStatus + " stopped at ticket " + ticketId);
        throw ex;
    }
}
```

The comment says what step 5 makes true. The import is `org.springframework.dao.DataAccessException`, the parent of every database exception from Spring.

`throw ex;` matters. If the exception does not leave the method, Spring sees a method that ended normally, and commits the tickets that did move.

## Try it

Restart the application. Create three tickets, and move the third to `IN_PROGRESS`. Here the new ids are 1, 2 and 3. Use the ids you get back.

```bash
curl -s -X POST http://localhost:8080/api/tickets \
  -H "Content-Type: application/json" -d '{"title":"Printer is jammed","priority":"LOW"}'
curl -s -X POST http://localhost:8080/api/tickets \
  -H "Content-Type: application/json" -d '{"title":"VPN drops every hour","priority":"LOW"}'
curl -s -X POST http://localhost:8080/api/tickets \
  -H "Content-Type: application/json" -d '{"title":"Reset my password","priority":"LOW"}'
curl -i -X PATCH http://localhost:8080/api/tickets/3/status \
  -H "Content-Type: application/json" -d '{"status":"IN_PROGRESS"}'
```

Expected: `{"id":1}`, `{"id":2}`, `{"id":3}`, then `HTTP/1.1 204`.

Ask for all three to move:

```bash
curl -i -X POST http://localhost:8080/api/tickets/bulk-status \
  -H "Content-Type: application/json" -d '{"ticketIds":[1,2,3],"status":"IN_PROGRESS"}'
```

Expected:

```text
HTTP/1.1 409
{"status":409,"error":"Conflict","message":"Ticket 3 is IN_PROGRESS. The next status is RESOLVED","timestamp":"..."}
```

Now look at the log in SQL Developer:

```sql
SELECT MESSAGE, LOGGED_AT FROM SUPPORT_ERROR_LOG;
```

Expected: no rows. `record` ran, and its line was rolled back with tickets 1 and 2.

## Checkpoint

`src/main/java/com/bsp/supportassistant/service/TicketService.java` - Matches [../support-assistant-plsql-transactions/src/main/java/com/bsp/supportassistant/service/TicketService.java](../support-assistant-plsql-transactions/src/main/java/com/bsp/supportassistant/service/TicketService.java) exactly.

<details>
<summary>Full <code>src/main/java/com/bsp/supportassistant/service/TicketService.java</code></summary>

```java
package com.bsp.supportassistant.service;

import com.bsp.supportassistant.dto.CreateTicketRequest;
import com.bsp.supportassistant.dto.TicketResponse;
import com.bsp.supportassistant.exception.InvalidRequestException;
import com.bsp.supportassistant.repository.TicketRepository;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

// The business rules are in the PL/SQL package. The service decides what happens together.
@Service
public class TicketService {

    private final TicketRepository ticketRepository;
    private final ErrorLogService errorLogService;

    public TicketService(TicketRepository ticketRepository, ErrorLogService errorLogService) {
        this.ticketRepository = ticketRepository;
        this.errorLogService = errorLogService;
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
    // The reason is written to SUPPORT_ERROR_LOG first, in a transaction of its own.
    @Transactional
    public void changeStatus(List<Long> ticketIds, String newStatus) {
        if (ticketIds == null || ticketIds.isEmpty()) {
            throw new InvalidRequestException("ticketIds must hold at least one id");
        }
        for (Long ticketId : ticketIds) {
            try {
                ticketRepository.changeStatus(ticketId, newStatus);
            } catch (DataAccessException ex) {
                // Saved in a transaction of its own, so the rollback of this method does not remove it.
                errorLogService.record("Bulk change to " + newStatus + " stopped at ticket " + ticketId);
                throw ex;
            }
        }
    }

    public List<TicketResponse> find(String status) {
        return ticketRepository.findTickets(status);
    }
}
```

</details>

`ErrorLogService.java` is as in step 3.

## Common mistakes

| Symptom | Cause | Fix |
|---|---|---|
| The bulk call answers `204`, and only tickets 1 and 2 moved | `throw ex;` is missing. The method ended normally, so Spring committed | End the `catch` block with `throw ex;` |
| `Parameter 1 of constructor in com.bsp.supportassistant.service.TicketService required a bean of type 'com.bsp.supportassistant.service.ErrorLogService' that could not be found.` | `ErrorLogService` has no `@Service` | Add `@Service` |
| The bulk call answers `500` with no `message`, and the log has `BadSqlGrammarException` and `PLS-00201: identifier 'SUPPORT_ERROR_PKG.LOG_ERROR' must be declared` | `03-error-log.sql` was not run, so the call to `LOG_ERROR` failed inside the `catch` | Run the script |

Next: **[A transaction of its own](05-a-transaction-of-its-own.md)**.
