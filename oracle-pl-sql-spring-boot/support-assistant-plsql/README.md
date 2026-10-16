# support-assistant-plsql

A ticket API that calls an Oracle PL/SQL package. The business rules (the due date, the order of statuses) are in the package. The Spring Boot application receives the request, calls the package, and returns JSON. There is **no entity class and no JPA** in this project.

This is the project of [part 2](../02-calling-plsql-programs.md) and [part 3](../03-errors-and-transactions.md). The optional [part 4](../04-the-same-calls-with-mybatis.md) has the same API through MyBatis annotations, in [support-assistant-plsql-mybatis](../support-assistant-plsql-mybatis/).

## Versions

| Item | Version |
|---|---|
| Java | 21 |
| Spring Boot | 4.1.1 |
| Oracle JDBC driver | `ojdbc17` (comes with Spring Boot) |
| Oracle Database | 19c, or 21c Express Edition |

## Before you build or run

1. Run the scripts in [db/oracle](db/oracle/), in order, as the user that your application connects as:

   | Script | What it does |
   |---|---|
   | `01-tables.sql` | Creates `SUPPORT_TICKETS_SEQ`, `SUPPORT_TICKETS`, and `SUPPORT_TICKET_LOG` |
   | `02-package.sql` | Creates the package `SUPPORT_TICKET_PKG`. You can run it again at any time |
   | `99-drop.sql` | Removes everything above, so that you can start again |

2. Set the connection in environment variables. Do not write a password in a file.

   | Variable | Meaning |
   |---|---|
   | `DB_USERNAME` | The Oracle user |
   | `DB_PASSWORD` | The password of that user |
   | `DB_URL` | Optional. Default `jdbc:oracle:thin:@//localhost:1521/XEPDB1` |

> **Note:** a variable that is not set reaches Oracle as the text `${DB_PASSWORD}`. The application still starts, and the first call fails with `ORA-01017`.

## Build and run

```bash
export DB_USERNAME=YOUR_USERNAME
export DB_PASSWORD=YOUR_PASSWORD
mvn clean verify
java -jar target/support-assistant-plsql-0.0.1-SNAPSHOT.jar
```

The application starts on port `8080`. `mvn clean verify` runs four tests that call the package in your database. Each test rolls back, so the tables stay as they were. The database must be running, with the scripts applied.

## Layout

```text
db/oracle/01-tables.sql                      # sequence and two tables
db/oracle/02-package.sql                     # SUPPORT_TICKET_PKG
db/oracle/99-drop.sql                        # start again
src/main/resources/application.yaml          # connection from environment variables
src/main/java/com/bsp/supportassistant
├── SupportAssistantApplication.java
├── dto/                                     # records for requests and responses
├── repository/TicketRepository.java         # one SimpleJdbcCall for each program
├── service/TicketService.java               # @Transactional for the bulk change
├── exception/                               # ApiError, GlobalExceptionHandler
└── api/TicketController.java
src/test/java/com/bsp/supportassistant/TicketPackageTests.java
```

## The package

| Program | Kind | Called by |
|---|---|---|
| `open_count(p_priority)` | Function | `GET /api/tickets/open-count?priority=HIGH` |
| `create_ticket(p_title, p_description, p_priority, p_ticket_id OUT)` | Procedure | `POST /api/tickets` |
| `change_status(p_ticket_id, p_new_status)` | Procedure | `PATCH /api/tickets/{id}/status`, `POST /api/tickets/bulk-status` |
| `find_tickets(p_status, p_tickets OUT SYS_REFCURSOR)` | Procedure | `GET /api/tickets?status=OPEN` |

| Error number | Meaning | Status |
|---|---|---|
| `20001` | The title is missing | `400` |
| `20002` | The priority is not `HIGH`, `MEDIUM` or `LOW` | `400` |
| `20003` | The ticket does not exist | `404` |
| `20004` | The status change is not allowed | `409` |

## Endpoints

| Method | URL | Status codes |
|---|---|---|
| `GET` | `/api/tickets/open-count?priority=HIGH` | `200` |
| `GET` | `/api/tickets?status=OPEN` (status is optional) | `200` |
| `POST` | `/api/tickets`, body `{"title":"...","description":"...","priority":"HIGH"}` | `201`, `400` |
| `PATCH` | `/api/tickets/{id}/status`, body `{"status":"IN_PROGRESS"}` | `204`, `404`, `409` |
| `POST` | `/api/tickets/bulk-status`, body `{"ticketIds":[2,3],"status":"IN_PROGRESS"}` | `204`, `400`, `404`, `409` |

An error from the package answers `{"status":409,"error":"Conflict","message":"...","timestamp":"..."}`.

## Running on Spring Boot 3.5

The Java code and `application.yaml` are the same. Only these lines of `pom.xml` change:

| Item in `pom.xml` | Spring Boot 4.1.1 (this project) | Spring Boot 3.5.x |
|---|---|---|
| Parent version | `4.1.1` | `3.5.16`, or another 3.5.x |
| Web starter | `spring-boot-starter-webmvc` | `spring-boot-starter-web` |
| Test starter | `spring-boot-starter-webmvc-test` | `spring-boot-starter-test` |
| Oracle driver | `ojdbc17` | `ojdbc11` |

Built and tested on Spring Boot 3.5.16 with these changes: all four tests pass.

## What this project does not do

- It does not check the request fields in Java. The package checks them.
- It has no login. Any caller can change any ticket.
- It is not a copy of the Day 5 project. The tables and the package have their own names, so both can live in one schema.
