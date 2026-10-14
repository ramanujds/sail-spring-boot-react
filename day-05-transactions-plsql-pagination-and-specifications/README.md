# Day 5: Transactions, PL/SQL, pagination and specifications

**Builds on:** [Day 4](../day-04-exception-handling-relationships-and-composite-keys/README.md)

Day 5 makes the `support-assistant` API safe when one request changes several things, gives tickets a due date, calls code that lives in the Oracle database, and lets the caller ask for exactly the tickets it needs, one page at a time. Every part changes one project in place.

## Parts

| Part | What | Notes |
|---|---|---|
| 1 | Transactions: close a ticket and add a note, or do neither | [01-transactions.md](code-and-walkthroughs/01-transactions.md) |
| 2 | SLA logic: a due date from the priority, and an overdue flag | [02-sla-logic.md](code-and-walkthroughs/02-sla-logic.md) |
| 3 | Calling PL/SQL: a function and a procedure | [03-calling-plsql.md](code-and-walkthroughs/03-calling-plsql.md) |
| 4 | Pagination: `page`, `size`, and `sort` | [04-pagination.md](code-and-walkthroughs/04-pagination.md) |
| 5 | Specifications: filters that combine | [05-specifications.md](code-and-walkthroughs/05-specifications.md) |

Part 3 comes after part 2 because the PL/SQL programs use the due date that part 2 adds.

## Project

| Project | Holds | Parts |
|---|---|---|
| [support-assistant-jpa](code-and-walkthroughs/support-assistant-jpa/) | Tickets and comments in Oracle, with a service layer | 1 to 5. Each part changes it in place |

The project has a README with build and run steps.

## Before you start

- Your project from the end of Day 4, part 4, running against your Oracle database, with the `TICKETS` and `COMMENTS` tables. The `TICKET_WATCHERS` table of part 5 is not used today.
- Two database changes are made during the day. Each part gives the statement to run:

| Change | Made in | Script |
|---|---|---|
| Two columns on `TICKETS`: `CREATED_AT` and `DUE_BY` | Part 2 | `support-assistant-jpa/db/oracle/schema.sql` |
| The function `OVERDUE_COUNT` and the procedure `ESCALATE_OVERDUE` | Part 3 | `support-assistant-jpa/db/oracle/plsql.sql` |
