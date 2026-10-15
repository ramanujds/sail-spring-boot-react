# Step 11 — Recap and exercises

> [Back to index](README.md) · Previous: [Test the package](10-test-the-package.md) · Next: none

## Quick reference

| Concept | Where it lives |
|---|---|
| The rules and the SQL | `db/oracle/02-package.sql` |
| Connection from environment variables | `src/main/resources/application.yaml` |
| One `SimpleJdbcCall` for each program | `TicketRepository` |
| `executeFunction` and `Number` | `TicketRepository.openCount` |
| `OUT` value from a `Map`, upper-case key | `TicketRepository.createTicket` |
| Cursor and row mapper | `TicketRepository.findTickets` and `toTicket` |
| Error number to status code | `GlobalExceptionHandler.statusFor` |
| All or none | `TicketService.changeStatus(List, String)` with `@Transactional` |
| Rollback in tests | `TicketPackageTests` |

## Gotchas

| Gotcha | What happens | Fix |
|---|---|---|
| A function returns `BOOLEAN` | `ORA-17004: Invalid column type: 1111` | Return `NUMBER` (1 or 0) or `VARCHAR2` (`'Y'` or `'N'`) |
| A procedure has its own `COMMIT` | A Spring rollback does not undo its work | Leave the `COMMIT` out. Let Spring decide |
| The package body does not compile | `ORA-04063: package body ... has errors`, and `500` | `SHOW ERRORS PACKAGE BODY SUPPORT_TICKET_PKG` |
| `NUMBER` read as `Integer` | `ClassCastException`, `BigDecimal` to `Integer` | Read it as a `Number` |
| A ticket made in SQL Developer is not seen by the API | The SQL Developer session did not commit | `COMMIT` in SQL Developer |
| A rolled-back create | The sequence number is used. The next id is higher | Normal for an Oracle sequence |

## Discussion questions

1. Why is there no entity class in this project? What would an entity add?
2. The due date is worked out in the package. What would go wrong if Java worked it out too?
3. Why does `statusFor` live in Java and not in the package?
4. What changes for the caller if `CHANGE_STATUS` ends with a `COMMIT`?
5. Why does the handler show the client a general message for `ORA-04063` but the package text for `ORA-20004`?

## Exercises

1. Call `GET /api/tickets/open-count?priority=high`. Change `OPEN_COUNT` so that it accepts lower case, and run the package script again. Say why Java needs no change.
2. Add a rule to `create_ticket`: a title shorter than 5 characters raises `-20005`. Check that the API answers `400`.
3. Add `due_in_hours (p_ticket_id IN NUMBER) RETURN NUMBER` to the package. Call it from `GET /api/tickets/{id}/due-in-hours`.
4. Add a procedure `find_overdue (p_tickets OUT SYS_REFCURSOR)` and an endpoint for it. Reuse `toTicket`.
5. Remove `@Transactional` from the bulk method. Send `[2,3,1]` again. Say which tickets changed. Put it back.
6. Delete `TicketRepository`. Rebuild it from memory, then compare it with the reference file.

## What's next

- The notes behind this walkthrough: [Entities or not?](../01-entities-or-not.md), [Calling PL/SQL programs](../02-calling-plsql-programs.md), and [Errors and transactions](../03-errors-and-transactions.md).
- Optional: the same calls through Spring Data JPA, in [part 4](../04-the-same-calls-with-spring-data-jpa.md) and the project [support-assistant-plsql-jpa](../support-assistant-plsql-jpa/).
