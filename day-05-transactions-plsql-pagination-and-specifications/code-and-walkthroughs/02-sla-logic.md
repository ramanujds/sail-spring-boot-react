# SLA logic: due dates and overdue tickets

A support team promises answers within a time. This promise is called a **service level agreement** (SLA). In this note you give every ticket a due date from its priority, and you tell the caller whether a ticket is overdue.

**Starting point:** your project at the end of [part 1](01-transactions.md).
**Project:** [support-assistant-jpa](support-assistant-jpa/) in this folder.

## What you will build

- Two new columns in `TICKETS`: `CREATED_AT` and `DUE_BY`.
- A `SlaPolicy` class that holds the two rules.
- `dueBy` and `overdue` in every `TicketResponse`.

---

## 1. The rules

| Priority | Due in |
|---|---|
| `HIGH` | 1 day after the ticket is created |
| `MEDIUM` | 3 days |
| `LOW` | 7 days |

A ticket is **overdue** when both are true:

- it is not finished: its status is not `RESOLVED` or `CLOSED`, and
- its due date is earlier than now.

Two things about the rules:

- The due date is **stored** when the ticket is created. If it were worked out every time, a change to the rule would change old tickets.
- `overdue` is **worked out** at the moment of the request, because it depends on the time now. Storing it would make it wrong an hour later.

---

## Build steps

### Step 1: add the columns

Run this at your `SQL>` prompt:

```sql
ALTER TABLE TICKETS ADD (
    CREATED_AT  TIMESTAMP  DEFAULT SYSTIMESTAMP NOT NULL,
    DUE_BY      TIMESTAMP  DEFAULT SYSTIMESTAMP NOT NULL
);
```

`DEFAULT SYSTIMESTAMP` gives the tickets that already exist a value, which is the time of this statement. Their due date is now, so they will soon count as overdue. That is useful for the next steps.

[db/oracle/schema.sql](support-assistant-jpa/db/oracle/schema.sql) has the same two columns in `CREATE TABLE TICKETS`, for a new database.

**Check:** `DESC TICKETS` lists seven columns, and the last two are `CREATED_AT` and `DUE_BY`.

### Step 2: add the fields to `Ticket`

```java
private LocalDateTime createdAt;
private LocalDateTime dueBy;
```

Add a getter and a setter for each, as for the other fields.

**Check:** `mvn -q compile` prints no errors. If you start the application before step 1, it stops with `Schema validation: missing column [created_at] in table [tickets]`.

### Step 3: the rules in one class

Create `SlaPolicy` in `service`:

```java
public class SlaPolicy {

    // HIGH is due in 1 day, MEDIUM in 3 days, LOW in 7 days.
    public static LocalDateTime dueBy(Priority priority, LocalDateTime createdAt) {
        if (priority == Priority.HIGH) {
            return createdAt.plusDays(1);
        }
        if (priority == Priority.MEDIUM) {
            return createdAt.plusDays(3);
        }
        return createdAt.plusDays(7);
    }

    // A ticket is overdue when it is not finished and its due date has passed.
    public static boolean isOverdue(Ticket ticket, LocalDateTime now) {
        boolean finished = ticket.getStatus() == TicketStatus.RESOLVED
                || ticket.getStatus() == TicketStatus.CLOSED;
        return !finished && ticket.getDueBy().isBefore(now);
    }
}
```

The class has no database and no Spring in it. A rule that is plain Java is easy to read, and easy to test.

`now` is a parameter, not read inside the method. Anyone can then ask "would this ticket be overdue next week?" by passing another time.

### Step 4: set the due date when a ticket is created

The rules for creating and changing a ticket now belong in `TicketService`. Add two methods:

```java
// A new ticket starts OPEN, and gets its due date from its priority.
public Ticket create(TicketRequest request) {
    Ticket ticket = TicketMapper.toEntity(request);
    LocalDateTime now = LocalDateTime.now();
    ticket.setStatus(TicketStatus.OPEN);
    ticket.setCreatedAt(now);
    ticket.setDueBy(SlaPolicy.dueBy(ticket.getPriority(), now));
    return ticketRepository.save(ticket);
}

// A new priority gives the ticket a new due date, counted from when it was created.
public Ticket update(UUID id, TicketRequest request) {
    Ticket ticket = ticketRepository.findById(id)
            .orElseThrow(() -> new TicketNotFoundException(id));
    TicketMapper.applyChanges(ticket, request);
    if (request.priority() != null) {
        ticket.setDueBy(SlaPolicy.dueBy(ticket.getPriority(), ticket.getCreatedAt()));
    }
    ticketRepository.save(ticket);
    return ticket;
}
```

In `TicketController`, the two methods call the service:

```java
@PostMapping
public ResponseEntity<TicketResponse> create(@RequestBody TicketRequest request) {
    TicketResponse response = TicketMapper.toResponse(ticketService.create(request));
    return ResponseEntity.status(HttpStatus.CREATED).body(response);
}

// Send only the fields you want to change.
@PatchMapping("/{id}")
public TicketResponse update(@PathVariable UUID id, @RequestBody TicketRequest request) {
    return TicketMapper.toResponse(ticketService.update(id, request));
}
```

`update` no longer returns a `ResponseEntity`. A plain `TicketResponse` is answered with `200`, and an exception answers the errors.

### Step 5: show the result in the response

Add two fields to `TicketResponse`:

```java
public record TicketResponse(
        UUID id,
        String title,
        String description,
        TicketStatus status,
        Priority priority,
        int commentCount,
        LocalDateTime dueBy,
        boolean overdue) {
}
```

In `TicketMapper.toResponse`, pass the two values at the end:

```java
ticket.getComments().size(),
ticket.getDueBy(),
SlaPolicy.isOverdue(ticket, LocalDateTime.now()));
```

**Check:** `mvn -q clean verify` ends with no errors.

### Step 6: try it

Restart, and create one ticket of each priority:

```bash
curl -X POST http://localhost:8080/api/v1/tickets -H "Content-Type: application/json" \
  -d '{"title":"SLA HIGH","priority":"HIGH"}'
curl -X POST http://localhost:8080/api/v1/tickets -H "Content-Type: application/json" \
  -d '{"title":"SLA MEDIUM","priority":"MEDIUM"}'
curl -X POST http://localhost:8080/api/v1/tickets -H "Content-Type: application/json" \
  -d '{"title":"SLA LOW","priority":"LOW"}'
```

**Check:** the three `dueBy` values are 1, 3, and 7 days after now, and `"overdue":false`:

```text
SLA HIGH    dueBy=2026-10-15T23:04:32.889596   overdue=false
SLA MEDIUM  dueBy=2026-10-17T23:04:33.170091   overdue=false
SLA LOW     dueBy=2026-10-21T23:04:33.202442   overdue=false
```

Change the priority of the `LOW` ticket, using its `id`:

```bash
curl -X PATCH http://localhost:8080/api/v1/tickets/TICKET_ID \
  -H "Content-Type: application/json" -d '{"priority":"HIGH"}'
```

**Check:** its `dueBy` is now one day after its creation: `2026-10-15T23:04:33.202442`, the same time of day as before, six days earlier.

### Step 7: make a ticket overdue

You cannot wait a day. Move the due date back in the database instead, at the `SQL>` prompt:

```sql
UPDATE TICKETS SET DUE_BY = SYSTIMESTAMP - INTERVAL '2' DAY WHERE ID = 'TICKET_ID';
COMMIT;
```

**Check:** `GET /api/v1/tickets/TICKET_ID` shows `"overdue":true`.

Now close it, with the endpoint from part 1:

```bash
curl -X POST http://localhost:8080/api/v1/tickets/TICKET_ID/close \
  -H "Content-Type: application/json" -d '{"agent":"Karan","note":"Done"}'
```

**Check:** the answer has `"status":"CLOSED"` and `"overdue":false`. The due date is still in the past, but a finished ticket is never overdue.

---

## Common mistakes

| Symptom | Cause | Fix |
|---|---|---|
| `Schema validation: missing column [created_at] in table [tickets]` when the application starts | The entity has the fields, and the table does not have the columns | Run the `ALTER TABLE` statement from step 1 |
| `ORA-01400: cannot insert NULL into ("SUPPORT"."TICKETS"."DUE_BY")` | A ticket was saved without going through `TicketService.create` | Create tickets with the service, so the due date is set |
| Every old ticket is overdue after step 1 | The `DEFAULT` gave them a due date of the time you ran the statement | Expected. Set their `DUE_BY` with an `UPDATE` if you want other dates |

---

## Practice

1. Create a `MEDIUM` ticket. Change its priority to `HIGH`, then back to `MEDIUM`. Write down `dueBy` each time and say why the third value equals the first.
2. Change the `LOW` rule from 7 days to 5 days in `SlaPolicy`. Create a new `LOW` ticket. Check its `dueBy`. Check that the older `LOW` tickets did not change.
3. Add a rule: a ticket whose `priority` is `HIGH` and whose status is `OPEN` is "urgent" when its due date is less than 4 hours away. Write a method `isUrgent(Ticket ticket, LocalDateTime now)` in `SlaPolicy` and add `urgent` to the response.

---

## Recap

- An SLA rule maps a priority to a time. Keep the rule in one small class with no database in it.
- Store the due date when the ticket is created. Work out `overdue` when the request arrives, because it depends on the time now.
- The service decides the values (status, dates). The controller does not.
- Adding a column means two changes: the table with `ALTER TABLE`, and the entity with a field.

### State at end of this part

| File | What changed |
|---|---|
| `db/oracle/schema.sql` | `TICKETS` has `CREATED_AT` and `DUE_BY` |
| `model/Ticket.java` | `createdAt` and `dueBy` |
| `service/SlaPolicy.java` | `dueBy` and `isOverdue` |
| `service/TicketService.java` | `create` and `update` set and change the due date |
| `dto/TicketResponse.java`, `mapper/TicketMapper.java` | `dueBy` and `overdue` |

| Request | Result |
|---|---|
| `POST /api/v1/tickets` | `201`, with `dueBy` 1, 3, or 7 days ahead |
| `PATCH /api/v1/tickets/{id}` with a `priority` | `200`, with a new `dueBy` |
| `GET /api/v1/tickets/{id}` | `overdue` is `true` for an unfinished ticket past its due date |
