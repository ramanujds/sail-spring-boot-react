# support-assistant-jpa

The `support-assistant` application with tickets and their comments stored in an **Oracle database**. This is the project of Day 4, parts 1 to 4. It continues the project of [Day 3](../../../day-03-rest-api-dtos-and-validation/code-and-walkthroughs/06-spring-data-jpa-with-oracle.md).

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

1. Have an Oracle database you can log in to, with the tables of [db/oracle/schema.sql](db/oracle/schema.sql): `TICKETS` and `COMMENTS`. See [Day 3, part 5](../../../day-03-rest-api-dtos-and-validation/code-and-walkthroughs/05-oracle-database-setup.md). If you already have `TICKETS`, run only the `CREATE TABLE COMMENTS` statement.
2. Open [src/main/resources/application.yaml](src/main/resources/application.yaml) and set `url`, `username`, and `password`. The file has two blocks: Option B (Oracle XE 21 in Docker) is active, and Option A (Oracle 19c) is commented out. Replace the placeholders and keep one block.

> **Warning:** Do not commit the file with a real password in it.

## Build and run

```bash
mvn clean package
java -jar target/support-assistant-0.0.1-SNAPSHOT.jar
```

Or run `SupportAssistantApplication` from IntelliJ. The application starts on port `8080`.

> **Note:** `mvn clean package` also runs the test, and the test starts the whole application. The database must be running, both tables must exist, and `application.yaml` must hold the right values, or the build fails with `Failed to load ApplicationContext`.

## Layout

```text
db/oracle/schema.sql                         # TICKETS and COMMENTS, run once by hand
src/main/resources/application.yaml          # the settings
src/main/java/com/bsp/supportassistant
├── SupportAssistantApplication.java
├── model/Ticket.java                        # one row of TICKETS, with its list of comments
├── model/Comment.java                       # one row of COMMENTS, with a link to its ticket
├── model/TicketStatus.java
├── model/Priority.java
├── dto/TicketRequest.java
├── dto/TicketResponse.java                  # includes commentCount
├── dto/CommentRequest.java
├── dto/CommentResponse.java
├── mapper/TicketMapper.java
├── mapper/CommentMapper.java
├── repository/TicketRepository.java         # query methods and @EntityGraph
├── repository/CommentRepository.java
├── exception/ApiError.java                  # the body of every error
├── exception/TicketNotFoundException.java
├── exception/GlobalExceptionHandler.java    # turns exceptions into ApiError responses
├── api/TicketController.java
└── api/CommentController.java
```

## Endpoints

| Method | URL | Status codes |
|---|---|---|
| `GET` | `/api/v1/tickets`, with optional `?status=` and `?priority=` | `200`, `400` for an unknown value |
| `GET` | `/api/v1/tickets/{id}` | `200`, `400` for an id that is not a UUID, `404` |
| `GET` | `/api/v1/tickets/count?status=OPEN` | `200`, `400` |
| `GET` | `/api/v1/tickets/search?keyword=printer` | `200`, `400` without `keyword` |
| `POST` | `/api/v1/tickets` | `201`, `400` |
| `PATCH` | `/api/v1/tickets/{id}` | `200`, `400`, `404` |
| `DELETE` | `/api/v1/tickets/{id}`. Its comments are deleted too | `204`, `400`, `404` |
| `GET` | `/api/v1/tickets/{ticketId}/comments` | `200`, `404` |
| `POST` | `/api/v1/tickets/{ticketId}/comments` | `201`, `400`, `404` |

Every error has the body `{"status":404,"error":"Not Found","message":"...","timestamp":"..."}`.

Swagger UI: <http://localhost:8080/swagger-ui.html>

## What changed since Day 3

- Part 1: `ApiError`, `TicketNotFoundException`, and `GlobalExceptionHandler`. The controller throws instead of building `404` responses.
- Part 2: `findAllByPriority`, `findAllByStatusAndPriority`, `countByStatus`, and a `@Query` method `search`, with matching endpoints.
- Part 3: the `COMMENTS` table, the `Comment` entity, and `CommentController`. `Ticket` has a `@OneToMany` list with `cascade = REMOVE`.
- Part 4: `TicketResponse` has `commentCount`. The repository methods that return tickets use `@EntityGraph`.

## What this version does not do yet

- There is no field-by-field validation. A request without a `title` or a `priority` reaches the database, which refuses it, and the API answers `400` with a general message.
- There is no service layer. The controllers call the repositories.
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

Built and run on Spring Boot 3.5.16 (Hibernate 6.6) with these changes.

Some messages differ in wording: Hibernate writes `Schema-validation:` (with a hyphen) in schema errors, and the lazy loading error reads `failed to lazily initialize a collection of role`.

