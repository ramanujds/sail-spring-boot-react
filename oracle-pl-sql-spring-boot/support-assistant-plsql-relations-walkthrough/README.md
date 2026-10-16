# Walkthrough: support-assistant-plsql-relations

Build a Spring Boot API for tickets **and their comments**, from an empty folder, one capability at a time. A ticket has many comments. This walkthrough shows how that one-to-many link is stored, written, and read when an Oracle PL/SQL package does the work. There is no entity class and no JPA.

**What you are building:** [support-assistant-plsql-relations](../support-assistant-plsql-relations/), described in its [README](../support-assistant-plsql-relations/README.md).

## What you will build

- Two tables joined by a foreign key, and a package `DESK_TICKET_PKG` with six programs.
- Seven endpoints:

| Endpoint | Calls | What it shows |
|---|---|---|
| `POST /api/tickets` | `CREATE_TICKET` | The parent, on its own |
| `GET /api/tickets?status=OPEN` | `FIND_TICKETS` | A list of parents, with no children |
| `PATCH /api/tickets/{id}/status` | `CHANGE_STATUS` | A parent changes, and the children feel it |
| `POST /api/tickets/{id}/comments` | `ADD_COMMENT` | Write a child. The parent decides if it is allowed |
| `GET /api/tickets/{id}` | `GET_TICKET` | One parent and its children: **two cursors** |
| `GET /api/tickets/with-comments` | `FIND_TICKETS_WITH_COMMENTS` | Many parents and their children: **one joined cursor** |
| `POST /api/tickets/with-first-comment` | `CREATE_TICKET`, then `ADD_COMMENT` | Parent and child saved together, or not at all |

- Error numbers turned into `400`, `404`, and `409` answers, including a new rule: a closed ticket takes no comments.
- Six tests that call the package. Five roll back. One checks that a failed transaction leaves nothing behind.

## Prerequisites

- Java 21 and Maven.
- An Oracle database, and a user that can create tables, sequences, an index, and a package.
- A SQL tool: SQL Developer or SQL\*Plus.
- The first walkthrough, [support-assistant-plsql-walkthrough](../support-assistant-plsql-walkthrough/README.md), or the same knowledge: `SimpleJdbcCall`, an `OUT` parameter, a `SYS_REFCURSOR`, `RAISE_APPLICATION_ERROR`, and `@Transactional`. This walkthrough does not teach them again. It shows how to use them when two tables are linked.

Time: about 3 hours.

## Steps

| Step | File | What you add | Est. time |
|---|---|---|---|
| 1 | [01-concepts-and-overview.md](01-concepts-and-overview.md) | The three questions that a relationship raises | 5 min |
| 2 | [02-tables-and-relationship.md](02-tables-and-relationship.md) | Two tables, a foreign key, an index | 15 min |
| 3 | [03-the-package.md](03-the-package.md) | `DESK_TICKET_PKG`, tried in SQL | 25 min |
| 4 | [04-project-and-connection.md](04-project-and-connection.md) | `pom.xml`, `application.yaml`, and the main class | 10 min |
| 5 | [05-ticket-calls.md](05-ticket-calls.md) | Create, list, and change the status of a ticket | 15 min |
| 6 | [06-add-a-comment.md](06-add-a-comment.md) | `ADD_COMMENT`: write a child | 10 min |
| 7 | [07-one-ticket-with-comments.md](07-one-ticket-with-comments.md) | `GET_TICKET`: two cursors in one call | 15 min |
| 8 | [08-many-tickets-with-comments.md](08-many-tickets-with-comments.md) | One joined cursor, grouped in Java. The N+1 trap | 20 min |
| 9 | [09-error-handling.md](09-error-handling.md) | `ApiError` and `GlobalExceptionHandler` | 10 min |
| 10 | [10-transactions.md](10-transactions.md) | A ticket and its first comment, all or nothing | 15 min |
| 11 | [11-test-the-relationship.md](11-test-the-relationship.md) | `TicketRelationTests` and `TicketTransactionTests` | 15 min |
| 12 | [12-recap-and-exercises.md](12-recap-and-exercises.md) | Quick reference, gotchas, exercises | 15 min |

## How each step is built

- **Goal** and **Why this matters** say what the step adds, and why.
- The numbered parts show only the new code.
- **Try it** gives a command and the output to expect.
- **Checkpoint** gives the complete files after the step. Compare yours with it before you go on.

## Relationship to the reference implementation

The last checkpoint of each file matches the file in [support-assistant-plsql-relations](../support-assistant-plsql-relations/) exactly. The build was checked step by step: the project as it stands at the end of steps 4 to 11 was compiled, and the finished project was built and run against Oracle.

| File | Final in step |
|---|---|
| `db/oracle/01-tables.sql` | 2 |
| `db/oracle/02-package.sql`, `99-drop.sql` | 3 |
| `pom.xml`, `application.yaml`, `SupportAssistantApplication.java` | 4 |
| `CreateTicketRequest.java`, `TicketCreated.java`, `StatusRequest.java`, `TicketResponse.java` | 5 |
| `CommentRequest.java`, `CommentCreated.java` | 6 |
| `CommentResponse.java`, `TicketWithComments.java` | 7 |
| `TicketRepository.java` | 8 |
| `ApiError.java`, `GlobalExceptionHandler.java` | 9 |
| `CreateTicketWithCommentRequest.java`, `TicketService.java`, `TicketController.java` | 10 |
| `TicketRelationTests.java`, `TicketTransactionTests.java` | 11 |

`TicketRepository.java`, `TicketService.java`, and `TicketController.java` grow over several steps. Each step shows the whole file as it stands then, and says "Grows in a later step" until the last one.

Start with **[Step 1 — Concepts and overview](01-concepts-and-overview.md)**.
