# support-assistant-jpa

The `support-assistant` application with tickets and comments in an **Oracle database**, a service layer, due dates, calls to PL/SQL, and paged, filterable lists. This is the project of Day 5. It continues [Day 4](../../../day-04-exception-handling-relationships-and-composite-keys/code-and-walkthroughs/support-assistant-jpa/).

## Versions

| Item | Version |
|---|---|
| Java | 21 |
| Spring Boot | 4.1.1 |
| Hibernate | 7.4.5 (comes with Spring Boot) |
| Oracle JDBC driver | `ojdbc17` (comes with Spring Boot) |
| Oracle Database | 19c, or 21c Express Edition |
| springdoc-openapi (Swagger UI) | 3.1.0 |

## Before you build or run

1. Have an Oracle database with the tables of [db/oracle/schema.sql](db/oracle/schema.sql): `TICKETS` (with `CREATED_AT` and `DUE_BY`) and `COMMENTS`. If you have the Day 4 `TICKETS` table, run the `ALTER TABLE` statement of [part 2](../02-sla-logic.md).
2. Run [db/oracle/plsql.sql](db/oracle/plsql.sql) once. It creates the function and the procedure of [part 3](../03-calling-plsql.md). The user needs permission to create them.
3. Open [src/main/resources/application.yaml](src/main/resources/application.yaml) and set `url`, `username`, and `password`. Option B (Oracle XE 21 in Docker) is active, and Option A (Oracle 19c) is commented out.

> **Warning:** Do not commit the file with a real password in it.

## Build and run

```bash
mvn clean package
java -jar target/support-assistant-0.0.1-SNAPSHOT.jar
```

The application starts on port `8080`. The test starts the whole application, so the database must be running with the tables and `application.yaml` must be right.

## Layout

```text
db/oracle/schema.sql                         # TICKETS and COMMENTS
db/oracle/plsql.sql                          # OVERDUE_COUNT and ESCALATE_OVERDUE
src/main/resources/application.yaml          # the settings
src/main/java/com/bsp/supportassistant
├── SupportAssistantApplication.java
├── model/                                   # Ticket, Comment, TicketStatus, Priority
├── dto/                                     # requests, responses, CloseRequest, PageResponse
├── mapper/                                  # TicketMapper, CommentMapper
├── repository/TicketRepository.java
├── repository/TicketSpecifications.java     # one method per filter
├── repository/CommentRepository.java
├── repository/OverdueRepository.java        # calls the PL/SQL programs
├── service/TicketService.java               # transactions, create, update, filters
├── service/SlaPolicy.java                   # due date and overdue rules
├── exception/                               # ApiError, TicketNotFoundException, GlobalExceptionHandler
└── api/                                     # TicketController, CommentController
```

## Endpoints

| Method | URL | Status codes |
|---|---|---|
| `GET` | `/api/v1/tickets?status=&priority=&keyword=&overdue=&page=&size=&sort=` | `200`, `400` |
| `GET` | `/api/v1/tickets/{id}` | `200`, `400`, `404` |
| `GET` | `/api/v1/tickets/count?status=OPEN` | `200`, `400` |
| `GET` | `/api/v1/tickets/search?keyword=printer` | `200`, `400` |
| `GET` | `/api/v1/tickets/overdue/count?priority=LOW` | `200`, `400`. Calls PL/SQL |
| `POST` | `/api/v1/tickets` | `201`, `400` |
| `PATCH` | `/api/v1/tickets/{id}` | `200`, `400`, `404` |
| `POST` | `/api/v1/tickets/{id}/close`, body `{"agent":"...","note":"..."}` | `200`, `400`, `404` |
| `POST` | `/api/v1/tickets/escalate-overdue` | `200`. Calls PL/SQL |
| `DELETE` | `/api/v1/tickets/{id}` | `204`, `400`, `404` |
| `GET` | `/api/v1/tickets/{ticketId}/comments` | `200`, `404` |
| `POST` | `/api/v1/tickets/{ticketId}/comments` | `201`, `400`, `404` |

The list answers `{"content":[...],"page":0,"size":5,"totalElements":30,"totalPages":6}`.

Swagger UI: <http://localhost:8080/swagger-ui.html>

## What changed since Day 4

- Part 1: `TicketService` and `POST /{id}/close`, with `@Transactional`.
- Part 2: `CREATED_AT` and `DUE_BY` on `TICKETS`, `SlaPolicy`, and `dueBy` and `overdue` in the response. `create` and `update` moved into the service.
- Part 3: `OverdueRepository` calls the PL/SQL function and procedure.
- Part 4: the list returns a `PageResponse`, and `sort` errors answer `400`. `commentCount` is read with `@Formula`, so a page does not load comments.
- Part 5: `TicketSpecifications`. The list filters by `status`, `priority`, `keyword`, and `overdue`.

## What this version does not do yet

- There is no field-by-field validation. A request without a `title` or a `priority` answers `400` with a general message.
- `create`, `update`, `close`, and the list use the service. The comment endpoints still call the repositories.
- The test only starts the application. It does not call the API.

## Running on Spring Boot 3.5

The Java code and `application.yaml` are the same. Only these lines of `pom.xml` change:

| Item in `pom.xml` | Spring Boot 4.1.1 (this project) | Spring Boot 3.5.x |
|---|---|---|
| Parent version | `4.1.1` | `3.5.16`, or another 3.5.x |
| Web starter | `spring-boot-starter-webmvc` | `spring-boot-starter-web` |
| Test starter | `spring-boot-starter-webmvc-test` | `spring-boot-starter-test` |
| Oracle driver | `ojdbc17` | `ojdbc11` |
| springdoc-openapi | `3.1.0` | `2.8.17`, or another 2.8.x |

Built and run on Spring Boot 3.5.16 (Hibernate 6.6) with these changes. The code uses only features that exist in both versions.

Some messages differ in wording: Hibernate writes `Schema-validation:` (with a hyphen) in schema errors, and the lazy loading error reads `failed to lazily initialize a collection of role`.

