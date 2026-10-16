# Step 7 — Recap and exercises

> [Back to index](README.md) · Previous: [Test after the rollback](06-test-after-the-rollback.md) · Next: none

## Quick reference

| Concept | Where it lives |
|---|---|
| A procedure with no `COMMIT`, so Spring decides | `SUPPORT_ERROR_PKG.log_error` in `db/oracle/03-error-log.sql` |
| All or none for several calls | `@Transactional` on `TicketService.changeStatus(List, String)` |
| Join the running transaction (the default) | `@Transactional`, which means `Propagation.REQUIRED` |
| A transaction of its own, committed at once | `@Transactional(propagation = Propagation.REQUIRES_NEW)` on `ErrorLogService.record` |
| Record, then let the rollback happen | `catch (DataAccessException ex) { ...; throw ex; }` in `TicketService` |
| Check the database after the service's transaction | `TicketTransactionTests`, which is **not** `@Transactional` |

## Gotchas

Each row was tried against Oracle with this project.

| Gotcha | What happens | Fix |
|---|---|---|
| `REQUIRES_NEW` on a method called from the same class | The annotation is ignored. The line is rolled back | Put the method in another class |
| The `catch` block does not throw again | The method ends normally. Spring commits the tickets that moved, and the answer is `204` | End the `catch` with `throw ex;` |
| A checked exception leaves a `@Transactional` method | Spring commits. It rolls back only for `RuntimeException` and `Error` | Throw a `RuntimeException`, or use `rollbackFor = Exception.class` |
| `@Transactional(readOnly = true)` on a method that calls a writing procedure | The change is saved. The Oracle driver treats `readOnly` as a hint | Use `readOnly` only on methods that read |
| A procedure with `PRAGMA AUTONOMOUS_TRANSACTION` | Its work stays after a Spring rollback, like `REQUIRES_NEW` | Choose one place to decide. In this module, Spring decides |
| A test class with `@Transactional` | The service joins the test's transaction. You cannot see the service's rollback | Leave `@Transactional` off a test that checks a rollback |

## Discussion questions

1. The bulk change and the log line use two transactions. How many database connections does one refused request need at the moment `record` runs?
2. Why is `record` a method of a service and not of `ErrorLogRepository`?
3. What would change if `log_error` had a `COMMIT` at the end, and `record` had a plain `@Transactional`?
4. Why does the test in step 6 count lines with the full message, and not count all lines?

## Exercises

1. Copy `record` into `TicketService` with `REQUIRES_NEW`, and call it there. Send a refused bulk request. Count the lines in `SUPPORT_ERROR_LOG` before and after. Put the code back.
2. Remove `throw ex;`. Create two tickets, move the second to `IN_PROGRESS`, and send both in a bulk request. Write down the status code and the status of each ticket. Put `throw ex;` back.
3. Add the reason to the log line: the first line of the Oracle message, such as `ORA-20004: Ticket 3 is IN_PROGRESS...`. Hint: `ex.getMostSpecificCause().getMessage()`.
4. Add `POST /api/tickets/bulk-status-each`. It moves every ticket it can, records one line for each ticket it cannot move, and answers `204`. Decide where `@Transactional` goes, if anywhere.
5. Add `GET /api/error-log`, which returns the last 10 lines. Add a procedure with a `SYS_REFCURSOR` to `SUPPORT_ERROR_PKG`, and reuse what you know from `FIND_TICKETS`.

## What's next

- The notes behind this walkthrough: [part 5, Transaction management](../05-transaction-management.md).
- The other topic that starts from the same project: [part 6, Pagination](../06-pagination.md), and its [walkthrough](../support-assistant-plsql-pagination-walkthrough/README.md).
