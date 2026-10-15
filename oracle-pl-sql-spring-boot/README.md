# Calling PL/SQL from Spring Boot

**Builds on:** the Spring Boot flow (controller, service, repository) and the JPA basics of [Day 2](../day-02-spring-boot-basics-and-jpa/README.md). [Day 5, part 3](../day-05-transactions-plsql-pagination-and-specifications/code-and-walkthroughs/03-calling-plsql.md) shows a first call. This module goes further: a whole package, a result set, errors, and transactions.

A Spring Boot application can use the PL/SQL procedures and functions that already live in your Oracle database. You keep the rules in the database. Spring Boot receives the web request, calls the program, and sends back the answer as JSON.

## The short answer

| Question | Answer |
|---|---|
| Can a Spring Boot application call PL/SQL procedures and functions? | Yes. It connects to Oracle and calls the program by name. |
| Do you need entity classes? | No. An entity is a Java class that stands for a table. A call to a program does not need one. |
| Do you need ORM concepts? | No. ORM is object-relational mapping, the part of JPA that turns rows into objects. There is no persistence context, no lazy loading, and no relationship mapping in a PL/SQL call. |
| What do you need? | A database connection, `SimpleJdbcCall` to call the program, a small record for each row that comes back, and `@Transactional` when several calls must succeed or fail together. |

## How a request travels

The picture shows one request, from the client to the database.

```mermaid
flowchart LR
    client["Client: curl or a browser"] --> controller["TicketController"]
    subgraph spring ["Spring Boot application"]
        controller --> service["TicketService"]
        service --> repository["TicketRepository"]
    end
    repository -->|"SimpleJdbcCall"| pkg["SUPPORT_TICKET_PKG"]
    subgraph oracle ["Oracle database"]
        pkg --> tables[("SUPPORT_TICKETS, SUPPORT_TICKET_LOG")]
    end
```

**How to read it:** each box passes the call to the next one. The rules (the order of statuses, the due date) are in the package, in the Oracle box. The Spring Boot box holds no entity class.

## Parts

| Part | What | Notes | Time |
|---|---|---|---|
| 1 | Entities or not: what a call needs, and when to use JPA | [01-entities-or-not.md](01-entities-or-not.md) | 20 min |
| 2 | Call a function, a procedure with an `OUT` parameter, and a result set | [02-calling-plsql-programs.md](02-calling-plsql-programs.md) | 60 min |
| 3 | Errors from `RAISE_APPLICATION_ERROR`, commits, and rollback | [03-errors-and-transactions.md](03-errors-and-transactions.md) | 40 min |
| 4 (optional) | The same calls with Spring Data JPA, in a separate project | [04-the-same-calls-with-spring-data-jpa.md](04-the-same-calls-with-spring-data-jpa.md) | 30 min |

## Project

| Project | Holds | Parts |
|---|---|---|
| [support-assistant-plsql](support-assistant-plsql/) | A ticket API that calls the package `SUPPORT_TICKET_PKG` with `SimpleJdbcCall`. No entity, no JPA | 2 and 3 |
| [support-assistant-plsql-jpa](support-assistant-plsql-jpa/) | The same API, calling the same package through Spring Data JPA | 4 (optional) |

Each project has a README with build and run steps, and the SQL scripts in `db/oracle/`. Both use the same package, so run the scripts once.

## Before you start

- A running Spring Boot application with a controller and a service, as in Day 2 and Day 3.
- Java 21 and Maven.
- An Oracle database, and a user that can create tables, a sequence, and a package.
- A SQL tool: SQL Developer or SQL\*Plus.

## What you will have at the end

- One PL/SQL package with a function and three procedures, including one that opens a `SYS_REFCURSOR`.
- Five endpoints that call it.
- Error numbers from `RAISE_APPLICATION_ERROR` turned into `400`, `404`, and `409` answers.
- A test that calls the package and rolls back, so the tables stay clean.
