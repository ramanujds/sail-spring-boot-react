# Step 12 — Recap and exercises

> [Back to index](README.md) · Previous: [Test the relationship](11-test-the-relationship.md) · Next: none

## Quick reference

| Concept | Where it lives |
|---|---|
| The link between a comment and its ticket | `DESK_COMMENTS.TICKET_ID`, a foreign key, in `db/oracle/01-tables.sql` |
| An index on the foreign key | `DESK_COMMENTS_TICKET_IX` |
| The parent decides what the child may do | `ADD_COMMENT` and `LOCK_TICKET`, in `db/oracle/02-package.sql` |
| The id of the parent comes from the URL | `TicketController.addComment`, `@PathVariable` |
| One parent, two cursors | `GET_TICKET` and `TicketRepository.getTicket` |
| Many parents, one joined cursor | `FIND_TICKETS_WITH_COMMENTS` and `TicketRepository.findTicketsWithComments` |
| A `NULL` from a `LEFT JOIN` | `TicketRepository.toJoinedRow`, with `wasNull()` |
| Parent and children as one answer | `TicketWithComments` |
| A state of the parent becomes `409` | `GlobalExceptionHandler.statusFor`, number `20006` |
| Parent and child saved together | `TicketService.createWithFirstComment` with `@Transactional` |
| A rollback you can see | `TicketTransactionTests` |

## Which read for which job

| Job | Use | Calls |
|---|---|---|
| A list of tickets, no comments | `FIND_TICKETS` | 1 |
| One ticket and its comments | `GET_TICKET` | 1, two cursors |
| Many tickets and their comments | `FIND_TICKETS_WITH_COMMENTS` | 1, one joined cursor |
| Many tickets, and `GET_TICKET` for each | Avoid | 1 + N |

## Gotchas

| Gotcha | What happens | Fix |
|---|---|---|
| `JOIN` instead of `LEFT JOIN` | Tickets without comments vanish from the list | `LEFT JOIN` |
| `getLong` on a `NULL` column | `0`, which looks like a real id | Check `wasNull()` straight after |
| A cursor name that is wrong or in lower case | A list of maps (`ClassCastException`), or `null` | The parameter name from the specification, in upper case |
| A missing parent that opens an empty cursor | The client sees an empty answer, not a `404` | Check the parent first and raise an error |
| No index on the foreign key | Slow comment queries on a big table | Create the index yourself |
| Dropping the parent table first | `ORA-02449` | Drop the child first |
| A rolled-back create | The sequence number is used. The next id is higher | Normal for an Oracle sequence |
| `@Transactional` on the test class that checks a rollback | The service joins the test's transaction, and the data stays visible | Leave the test class without it |

## Discussion questions

1. The comment's ticket id comes from the URL, not the body. What could go wrong if it came from the body?
2. `ADD_COMMENT` locks the ticket row with `FOR UPDATE`. What could happen to a comment if it did not?
3. Why is `20006` a `409` and not a `400`?
4. The joined cursor repeats the ticket columns in every row. When would you prefer two calls to one joined call?
5. JPA would give you `ticket.getComments()`. What does this project give you instead, and what does it cost to write?

## Exercises

1. Call `GET /api/tickets/with-comments?status=IN_PROGRESS`. Say which tickets you get, and whether each one has all its comments or only some.
2. Add a rule to `add_comment`: a text longer than 500 characters raises `-20007`, and the API answers `400`. Where do you add the number in Java?
3. Add `GET /api/tickets/{id}/comments`, which answers only the comments of a ticket. Write the package program first, and try it in SQL. Reuse `toComment`.
4. Add the number of comments to `FIND_TICKETS`: a new column `COMMENT_COUNT` from a `COUNT(*)` with `GROUP BY`. Which records change?
5. Write `delete_ticket (p_ticket_id IN NUMBER)` in the package. Decide what happens to the comments: delete them first in the program, or add `ON DELETE CASCADE` to the foreign key. Say which you chose and why.
6. Take `@Transactional` off `createWithFirstComment`. Run the tests and read the failure. Put it back.
7. Delete `TicketRepository`. Rebuild it from memory, then compare it with the reference file.

## What's next

- The notes behind this walkthrough: [Calling PL/SQL programs](../02-calling-plsql-programs.md) and [Errors and transactions](../03-errors-and-transactions.md).
- The first project and its walkthrough, with no relationship: [support-assistant-plsql](../support-assistant-plsql/) and [its walkthrough](../support-assistant-plsql-walkthrough/README.md).
