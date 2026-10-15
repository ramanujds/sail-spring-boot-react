# Walkthrough: support-assistant-plsql

Build a Spring Boot API that calls an Oracle PL/SQL package, from an empty folder, one capability at a time. There is no entity class and no JPA.

**What you are building:** [support-assistant-plsql](../support-assistant-plsql/), described in its [README](../support-assistant-plsql/README.md).

## What you will build

- A package `SUPPORT_TICKET_PKG` with one function and three procedures.
- Five endpoints that call it:

| Endpoint | Calls |
|---|---|
| `GET /api/tickets/open-count?priority=HIGH` | the function `OPEN_COUNT` |
| `POST /api/tickets` | the procedure `CREATE_TICKET`, with an `OUT` value |
| `GET /api/tickets?status=OPEN` | the procedure `FIND_TICKETS`, with a `SYS_REFCURSOR` |
| `PATCH /api/tickets/{id}/status` | the procedure `CHANGE_STATUS` |
| `POST /api/tickets/bulk-status` | `CHANGE_STATUS` for several tickets, all or none |

- Error numbers from `RAISE_APPLICATION_ERROR` turned into `400`, `404`, and `409`.
- Four tests that call the package and roll back.

## Prerequisites

- Java 21 and Maven.
- An Oracle database, and a user that can create tables, a sequence, and a package.
- A SQL tool: SQL Developer or SQL\*Plus.
- A Spring Boot application with a controller and a service, as in [Day 2](../../day-02-spring-boot-basics-and-jpa/README.md).
- Background reading, if you want it: [Entities or not?](../01-entities-or-not.md).

Time: about 2 hours.

## Steps

| Step | File | What you add | Est. time |
|---|---|---|---|
| 1 | [01-concepts-and-overview.md](01-concepts-and-overview.md) | The picture and the words used in every step | 5 min |
| 2 | [02-database-objects.md](02-database-objects.md) | Two tables, a sequence, and the package `SUPPORT_TICKET_PKG` | 15 min |
| 3 | [03-project-and-connection.md](03-project-and-connection.md) | `pom.xml`, `application.yaml`, and the main class | 10 min |
| 4 | [04-call-a-function.md](04-call-a-function.md) | `TicketRepository`, `TicketService`, `TicketController` for `OPEN_COUNT` | 10 min |
| 5 | [05-procedure-with-out-parameter.md](05-procedure-with-out-parameter.md) | `CREATE_TICKET` and its `OUT` value, with two records | 10 min |
| 6 | [06-read-a-result-set.md](06-read-a-result-set.md) | `FIND_TICKETS` and a row mapper | 10 min |
| 7 | [07-change-status.md](07-change-status.md) | `CHANGE_STATUS`, and what happens before an error handler exists | 10 min |
| 8 | [08-error-handling.md](08-error-handling.md) | `ApiError` and `GlobalExceptionHandler` | 10 min |
| 9 | [09-transactions.md](09-transactions.md) | `@Transactional` and the bulk change | 15 min |
| 10 | [10-test-the-package.md](10-test-the-package.md) | `TicketPackageTests` with rollback | 10 min |
| 11 | [11-recap-and-exercises.md](11-recap-and-exercises.md) | Quick reference, gotchas, exercises | 15 min |

## How each step is built

- **Goal** and **Why this matters** say what the step adds, and why.
- The numbered parts show only the new code.
- **Try it** gives a command and the output to expect.
- **Checkpoint** gives the complete files after the step. Compare yours with it before you go on.

## Relationship to the reference implementation

The last checkpoint of each file matches the file in [support-assistant-plsql](../support-assistant-plsql/) exactly. The build was checked step by step: every checkpoint was compiled, and the final files were compared with the reference.

| File | Final in step |
|---|---|
| `db/oracle/01-tables.sql`, `02-package.sql`, `99-drop.sql` | 2 |
| `pom.xml`, `application.yaml`, `SupportAssistantApplication.java` | 3 |
| `CreateTicketRequest.java`, `TicketCreated.java` | 5 |
| `TicketResponse.java` | 6 |
| `StatusRequest.java`, `TicketRepository.java` | 7 |
| `ApiError.java` | 8 |
| `BulkStatusRequest.java`, `InvalidRequestException.java`, `TicketService.java`, `TicketController.java`, `GlobalExceptionHandler.java` | 9 |
| `TicketPackageTests.java` | 10 |

Start with **[Step 1 — Concepts and overview](01-concepts-and-overview.md)**.
