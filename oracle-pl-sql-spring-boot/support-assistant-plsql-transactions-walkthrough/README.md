# Walkthrough: support-assistant-plsql-transactions

Take the finished ticket API, and control which work a rollback undoes. You do not start from an empty folder: step 1 copies the project you already have, and every later step adds to the copy.

**What you are building:** [support-assistant-plsql-transactions](../support-assistant-plsql-transactions/), described in its [README](../support-assistant-plsql-transactions/README.md). The notes behind it are [part 5, Transaction management](../05-transaction-management.md).

## What you will build

- A table `SUPPORT_ERROR_LOG`, and a package `SUPPORT_ERROR_PKG` with one procedure that writes to it.
- An `ErrorLogService` whose method runs in a transaction of its own (`REQUIRES_NEW`).
- A bulk status change that puts every ticket back when one is refused, and keeps one line that says why.
- Two tests that look at the database after the service's transaction has ended.

| Request | Before this walkthrough | After |
|---|---|---|
| `POST /api/tickets/bulk-status`, one ticket refused | `409`. No ticket moved | `409`. No ticket moved. One line in `SUPPORT_ERROR_LOG` |

## Prerequisites

- The finished [support-assistant-plsql](../support-assistant-plsql/) project, from the [first walkthrough](../support-assistant-plsql-walkthrough/README.md), working against your database. Its four tests pass.
- Its database objects: `01-tables.sql` and `02-package.sql` have been run.
- Java 21, Maven, and SQL Developer or SQL\*Plus.

Time: about 1 hour.

## Steps

| Step | File | What you add | Est. time |
|---|---|---|---|
| 1 | [01-copy-the-project.md](01-copy-the-project.md) | A copy of the project, with its own name | 10 min |
| 2 | [02-the-error-log.md](02-the-error-log.md) | `SUPPORT_ERROR_LOG` and `SUPPORT_ERROR_PKG` | 10 min |
| 3 | [03-call-the-log-procedure.md](03-call-the-log-procedure.md) | `ErrorLogRepository` and `ErrorLogService` | 5 min |
| 4 | [04-record-the-failure.md](04-record-the-failure.md) | The bulk change records a failure. The line is lost | 10 min |
| 5 | [05-a-transaction-of-its-own.md](05-a-transaction-of-its-own.md) | `REQUIRES_NEW`. The line stays | 10 min |
| 6 | [06-test-after-the-rollback.md](06-test-after-the-rollback.md) | `TicketTransactionTests` | 10 min |
| 7 | [07-recap-and-exercises.md](07-recap-and-exercises.md) | Quick reference, gotchas, exercises | 10 min |

## How each step is built

- **Goal** and **Why this matters** say what the step adds, and why.
- The numbered parts show only the new code.
- **Try it** gives a command and the output to expect.
- **Checkpoint** gives the complete files after the step. Compare yours with it before you go on.

## Relationship to the reference implementation

The last checkpoint of each file matches the file in [support-assistant-plsql-transactions](../support-assistant-plsql-transactions/) exactly. The project was compiled at the end of steps 1, 3 and 4, and the finished project was built, tested, and run against Oracle.

| File | Final in step |
|---|---|
| `pom.xml`, `application.yaml` | 1 |
| `db/oracle/03-error-log.sql`, `99-drop.sql` | 2 |
| `ErrorLogRepository.java` | 3 |
| `TicketService.java` | 4 |
| `ErrorLogService.java` | 5 |
| `TicketTransactionTests.java` | 6 |

Every other file is the same as in [support-assistant-plsql](../support-assistant-plsql/).

Start with **[Step 1 — Copy the project](01-copy-the-project.md)**.
