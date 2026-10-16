# Walkthrough: support-assistant-plsql-pagination

Take the finished ticket API, and make its list return one page at a time. You do not start from an empty folder: step 1 copies the project you already have, and every later step adds to the copy.

**What you are building:** [support-assistant-plsql-pagination](../support-assistant-plsql-pagination/), described in its [README](../support-assistant-plsql-pagination/README.md). The notes behind it are [part 6, Pagination](../06-pagination.md).

## What you will build

- A package `SUPPORT_TICKET_PAGE_PKG` with one procedure, `FIND_PAGE`. It opens a cursor for one page, and gives back the total as a second `OUT` value.
- A record `PageResponse` for the answer.
- `GET /api/tickets` with `page`, `size`, `sort` and `direction`, and a `400` for a value that is not allowed.
- Four tests that walk through the pages and roll back.

| Request | Before this walkthrough | After |
|---|---|---|
| `GET /api/tickets` | Every ticket, as a JSON list | The first 5 tickets, the most urgent first, with `page`, `size`, `totalElements` and `totalPages` |

## Prerequisites

- The finished [support-assistant-plsql](../support-assistant-plsql/) project, from the [first walkthrough](../support-assistant-plsql-walkthrough/README.md), working against your database. Its four tests pass.
- Its database objects: `01-tables.sql` and `02-package.sql` have been run.
- Oracle 12c or later. `OFFSET ... FETCH NEXT` does not exist in older versions.
- Java 21, Maven, and SQL Developer or SQL\*Plus.

This walkthrough does not need the [transactions walkthrough](../support-assistant-plsql-transactions-walkthrough/README.md). Both start from the same project.

Time: about 1 hour.

## Steps

| Step | File | What you add | Est. time |
|---|---|---|---|
| 1 | [01-copy-the-project.md](01-copy-the-project.md) | A copy of the project, with its own name | 10 min |
| 2 | [02-the-page-procedure.md](02-the-page-procedure.md) | `SUPPORT_TICKET_PAGE_PKG.FIND_PAGE`, tried in SQL, and 12 sample tickets | 15 min |
| 3 | [03-page-response-and-repository.md](03-page-response-and-repository.md) | `PageResponse`, and the call in `TicketRepository` | 10 min |
| 4 | [04-service-and-controller.md](04-service-and-controller.md) | The checks, and `GET /api/tickets` with paging | 15 min |
| 5 | [05-test-the-pages.md](05-test-the-pages.md) | `TicketPageTests` | 10 min |
| 6 | [06-recap-and-exercises.md](06-recap-and-exercises.md) | Quick reference, gotchas, exercises | 10 min |

## How each step is built

- **Goal** and **Why this matters** say what the step adds, and why.
- The numbered parts show only the new code.
- **Try it** gives a command and the output to expect.
- **Checkpoint** gives the complete files after the step. Compare yours with it before you go on.

## Relationship to the reference implementation

The last checkpoint of each file matches the file in [support-assistant-plsql-pagination](../support-assistant-plsql-pagination/) exactly. The project was compiled at the end of steps 1 and 3, and the finished project was built, tested, and run against Oracle.

| File | Final in step |
|---|---|
| `pom.xml`, `application.yaml` | 1 |
| `db/oracle/03-page-package.sql`, `04-sample-tickets.sql`, `99-drop.sql` | 2 |
| `PageResponse.java`, `TicketRepository.java` | 3 |
| `TicketService.java`, `TicketController.java` | 4 |
| `TicketPageTests.java` | 5 |

Every other file is the same as in [support-assistant-plsql](../support-assistant-plsql/).

Start with **[Step 1 — Copy the project](01-copy-the-project.md)**.
