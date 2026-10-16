# support-assistant-plsql-pagination

The ticket API of [support-assistant-plsql](../support-assistant-plsql/), where `GET /api/tickets` returns **one page** of tickets at a time. The page is read by a PL/SQL procedure with `OFFSET ... FETCH NEXT`. The same procedure gives back the total number of tickets as an `OUT` value.

This is the project of [part 6](../06-pagination.md). The step-by-step build is in [support-assistant-plsql-pagination-walkthrough](../support-assistant-plsql-pagination-walkthrough/README.md).

## What changed since support-assistant-plsql

| File | Change |
|---|---|
| `db/oracle/03-page-package.sql` | New. The package `SUPPORT_TICKET_PAGE_PKG` with the procedure `FIND_PAGE` |
| `db/oracle/04-sample-tickets.sql` | New, optional. Creates 12 tickets |
| `db/oracle/99-drop.sql` | Also drops the new package |
| `dto/PageResponse.java` | New. `content`, `page`, `size`, `totalElements`, `totalPages` |
| `repository/TicketRepository.java` | `findPage`, which calls `FIND_PAGE` |
| `service/TicketService.java` | `findPage` checks `page`, `size`, `sort` and `direction`. `find` is gone |
| `api/TicketController.java` | `GET /api/tickets` returns a `PageResponse` |
| `test/.../TicketPageTests.java` | New. Four tests that roll back |
| `pom.xml`, `application.yaml` | The project name |

`SUPPORT_TICKET_PKG` is not changed, so this project can share a schema with the other projects in this folder. `TicketRepository.findTickets` stays, because the tests use it.

## Before you build or run

1. Run the scripts in [db/oracle](db/oracle/), in order, as the user that your application connects as. If you already ran `01-tables.sql` and `02-package.sql` for another project in this folder, start at `03-page-package.sql`.

   | Script | What it does |
   |---|---|
   | `01-tables.sql` | Creates `SUPPORT_TICKETS_SEQ`, `SUPPORT_TICKETS`, and `SUPPORT_TICKET_LOG` |
   | `02-package.sql` | Creates the package `SUPPORT_TICKET_PKG` |
   | `03-page-package.sql` | Creates the package `SUPPORT_TICKET_PAGE_PKG`. Needs Oracle 12c or later |
   | `04-sample-tickets.sql` | Optional. Creates 12 tickets and commits |
   | `99-drop.sql` | Removes everything above, so that you can start again |

2. Set the connection in environment variables, as for the first project: `DB_USERNAME`, `DB_PASSWORD`, and, if your database is not `localhost:1521/XEPDB1`, `DB_URL`.

## Build and run

```bash
export DB_USERNAME=YOUR_USERNAME
export DB_PASSWORD=YOUR_PASSWORD
mvn clean verify
java -jar target/support-assistant-plsql-pagination-0.0.1-SNAPSHOT.jar
```

`mvn clean verify` runs eight tests against your database: the four of `TicketPackageTests` and the four of `TicketPageTests`. Each test rolls back.

## The list endpoint

`GET /api/tickets` takes five optional parameters:

| Parameter | Default | Allowed values |
|---|---|---|
| `status` | every status | `OPEN`, `IN_PROGRESS`, `RESOLVED`, `CLOSED` |
| `page` | `0` | `0` or more. The first page is `0` |
| `size` | `5` | `1` to `50` |
| `sort` | `dueBy` | `dueBy`, `createdAt` |
| `direction` | `asc` | `asc`, `desc` |

The answer:

```json
{"content":[ ...tickets... ],"page":0,"size":5,"totalElements":12,"totalPages":3}
```

A value that is not allowed answers `400`, for example `{"status":400,"error":"Bad Request","message":"Cannot sort by 'colour'. Use dueBy or createdAt",...}`.

The other four endpoints are the same as in [support-assistant-plsql](../support-assistant-plsql/README.md#endpoints).

## Layout

```text
db/oracle/03-page-package.sql                # SUPPORT_TICKET_PAGE_PKG.FIND_PAGE (new)
db/oracle/04-sample-tickets.sql              # 12 tickets (new, optional)
src/main/java/com/bsp/supportassistant
├── dto/PageResponse.java                    # (new)
├── repository/TicketRepository.java         # findPage
├── service/TicketService.java               # checks the paging values
└── api/TicketController.java                # GET /api/tickets returns a page
src/test/java/com/bsp/supportassistant/TicketPageTests.java   # (new)
```

## Running on Spring Boot 3.5

Change the same four lines of `pom.xml` as in [support-assistant-plsql](../support-assistant-plsql/README.md#running-on-spring-boot-35). The Java code is the same. This project was built and tested on Spring Boot 4.1.1 only.
