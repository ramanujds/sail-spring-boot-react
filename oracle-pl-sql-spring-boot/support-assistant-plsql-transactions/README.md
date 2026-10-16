# support-assistant-plsql-transactions

The ticket API of [support-assistant-plsql](../support-assistant-plsql/), with more control over transactions. When a bulk status change is refused, every ticket is put back, **and** one line about the failure is saved in `SUPPORT_ERROR_LOG`. That line is saved in a transaction of its own, so the rollback does not remove it.

This is the project of [part 5](../05-transaction-management.md). The step-by-step build is in [support-assistant-plsql-transactions-walkthrough](../support-assistant-plsql-transactions-walkthrough/README.md).

## What changed since support-assistant-plsql

| File | Change |
|---|---|
| `db/oracle/03-error-log.sql` | New. The table `SUPPORT_ERROR_LOG` and the package `SUPPORT_ERROR_PKG` |
| `db/oracle/99-drop.sql` | Also drops the two new objects |
| `repository/ErrorLogRepository.java` | New. Calls `SUPPORT_ERROR_PKG.LOG_ERROR` |
| `service/ErrorLogService.java` | New. `record` runs with `Propagation.REQUIRES_NEW` |
| `service/TicketService.java` | The bulk change records the failure, then lets the exception go on |
| `test/.../TicketTransactionTests.java` | New. Two tests that check what is left after a rollback |
| `pom.xml`, `application.yaml` | The project name |

`SUPPORT_TICKET_PKG` is not changed, so this project can share a schema with the other projects in this folder.

## Before you build or run

1. Run the scripts in [db/oracle](db/oracle/), in order, as the user that your application connects as. If you already ran `01-tables.sql` and `02-package.sql` for another project in this folder, run only `03-error-log.sql`.

   | Script | What it does |
   |---|---|
   | `01-tables.sql` | Creates `SUPPORT_TICKETS_SEQ`, `SUPPORT_TICKETS`, and `SUPPORT_TICKET_LOG` |
   | `02-package.sql` | Creates the package `SUPPORT_TICKET_PKG` |
   | `03-error-log.sql` | Creates `SUPPORT_ERROR_LOG` and the package `SUPPORT_ERROR_PKG` |
   | `99-drop.sql` | Removes everything above, so that you can start again |

2. Set the connection in environment variables, as for the first project: `DB_USERNAME`, `DB_PASSWORD`, and, if your database is not `localhost:1521/XEPDB1`, `DB_URL`.

## Build and run

```bash
export DB_USERNAME=YOUR_USERNAME
export DB_PASSWORD=YOUR_PASSWORD
mvn clean verify
java -jar target/support-assistant-plsql-transactions-0.0.1-SNAPSHOT.jar
```

`mvn clean verify` runs six tests against your database:

| Test class | Tests | Leaves rows behind? |
|---|---|---|
| `TicketPackageTests` | 4 | No. The class is `@Transactional`, so each test rolls back |
| `TicketTransactionTests` | 2 | Yes. The class is not `@Transactional` on purpose. Each run leaves three tickets whose titles start with `Transaction test` and two lines in `SUPPORT_ERROR_LOG` |

## Endpoints

The same five endpoints as [support-assistant-plsql](../support-assistant-plsql/README.md#endpoints). Only the bulk change behaves differently:

| Request | Result |
|---|---|
| `POST /api/tickets/bulk-status`, every ticket can move | `204`. Every ticket moved |
| The same, one ticket is refused | `409` or `404`. No ticket moved. One line in `SUPPORT_ERROR_LOG` |

Read the error log in SQL Developer:

```sql
SELECT MESSAGE, LOGGED_AT FROM SUPPORT_ERROR_LOG ORDER BY LOGGED_AT;
```

## Layout

```text
db/oracle/03-error-log.sql                   # SUPPORT_ERROR_LOG and SUPPORT_ERROR_PKG (new)
src/main/java/com/bsp/supportassistant
├── repository/ErrorLogRepository.java       # calls LOG_ERROR (new)
├── service/ErrorLogService.java             # REQUIRES_NEW (new)
└── service/TicketService.java               # records the failure, then rolls back
src/test/java/com/bsp/supportassistant/TicketTransactionTests.java   # (new)
```

## Running on Spring Boot 3.5

Change the same four lines of `pom.xml` as in [support-assistant-plsql](../support-assistant-plsql/README.md#running-on-spring-boot-35). The Java code is the same. This project was built and tested on Spring Boot 4.1.1 only.
