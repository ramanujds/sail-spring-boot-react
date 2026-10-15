# Entities or not?

A Spring Boot application can call PL/SQL programs. This note answers the first question: does the call need entity classes and ORM? Nothing to type in this part.

**Time:** about 20 minutes.

## What you will learn

- What an entity gives you, and what a PL/SQL call needs instead.
- How the parts of an Oracle Forms or JSP application map to the parts of a Spring Boot application.
- Which way to use for which job.

---

## 1. What an entity is

An **entity** is a Java class that stands for one table. With JPA you write the class, and JPA writes the SQL:

```java
@Entity
public class Ticket {
    @Id
    private Long id;
    private String title;
    private String status;
}
```

```java
ticketRepository.save(ticket);   // JPA writes the INSERT
```

This is **ORM** (object-relational mapping): rows become objects, and objects become rows. It saves you from writing SQL for plain create, read, update and delete.

---

## 2. What a call to PL/SQL needs

When the rule is in a PL/SQL program, the SQL is already written. Spring Boot has four things to do: connect, call the program by name, pass the values in, and read what comes back.

| | JPA with entities | Calling PL/SQL |
|---|---|---|
| Who writes the SQL | JPA | You, in PL/SQL |
| Where the rules live | In a Java service | In the package, in the database |
| What Java holds | An entity class for each table | A small record for each kind of result row |
| How data is saved | `save()` | A procedure call. Spring decides when to commit |
| What you learn | Entity states, lazy loading, relationships | Program names, parameters, result sets |

So a PL/SQL call needs **no entity**, no persistence context, no lazy loading, and no relationship mapping.

---

## 3. The parts side by side

The idea of each part is the same in both worlds. Only the place changes.

| Oracle Forms or JSP | Spring Boot | In this module |
|---|---|---|
| A button or trigger that starts the work | A controller method | `TicketController` |
| The code that decides which programs to call, and in what order | A service | `TicketService` |
| `EXEC` in a trigger, or a `CallableStatement` in a JSP | A repository with `SimpleJdbcCall` | `TicketRepository` |
| Business rules and `RAISE_APPLICATION_ERROR` | The package, unchanged | `SUPPORT_TICKET_PKG` |
| A message or an alert shown to the user | An `ApiError` in JSON, with a status code | `GlobalExceptionHandler` |
| `COMMIT_FORM`, `ROLLBACK_FORM` | `@Transactional` | `TicketService` |
| A list of values filled from a query | A JSON list, read from a `SYS_REFCURSOR` | `FIND_TICKETS` |

---

## 4. Which way for which job

| Job | Use | Why |
|---|---|---|
| A rule already exists in PL/SQL, or other programs (Forms, reports, batch jobs) share it | Call the program | One copy of the rule |
| Plain create, read, update and delete, with no rule | A JPA entity (Day 2) | Less code |
| One screen needs both | Both, in one application | They use the same connection settings and can share one transaction |

> **Warning:** Do not write a rule twice. If `CHANGE_STATUS` checks the order of statuses, do not check it again in Java. The two copies drift apart, and then the API and the Forms screens disagree.

---

## 5. The JPA way, in short

JPA can call a program too, and it works against the same package. Parts 2 and 3 use `SimpleJdbcCall`, because it has the fewest parts to learn. The optional [part 4](04-the-same-calls-with-spring-data-jpa.md) builds the JPA way in a separate project. Each JPA tool was tried on `SUPPORT_TICKET_PKG`:

| JPA way | What happened |
|---|---|
| `@Procedure` on a Spring Data repository method | Works for a procedure with an `OUT` value. The repository interface must name an entity type, so one entity class is needed even if it is never used |
| `EntityManager.createStoredProcedureQuery` | Works for a procedure, with no entity. Each parameter is registered by hand |
| The same, for a `SYS_REFCURSOR` | Works with no entity, but each row is an `Object[]`. A named record needs an entity or a result mapping |
| The same, for a function | Fails: `PLS-00221: 'OPEN_COUNT' is not a procedure or is undefined`. A function needs a native query: `SELECT pkg.fn(:p) FROM DUAL` |

`SimpleJdbcCall` handles a function, a procedure, and a cursor in the same way, and maps each row to a record without an entity. If your application already uses JPA, the JPA way keeps one style.

---|---|
| JPA `@Procedure` or `StoredProcedureQuery` | JPA is built around entities. A result set is read into an entity class |
| `SELECT my_function(1) FROM DUAL` with `JdbcTemplate` | Works for functions only, not for procedures |

---

## Common mistake

> **Warning:** Writing an entity and a JPA repository for a table that only PL/SQL should change.
>
> **How to spot it:** a `save()` call writes to a table that a procedure also guards. The rule in the procedure is skipped, because `save()` does not call it.
>
> **Fix:** leave that table without an entity. Change it only through the package.

---

## Recap

- A call to PL/SQL needs a connection, the program name, the values, and a place to read the result. It does not need an entity.
- Keep each rule in one place. If the rule is in the package, call the package.
- Use JPA for plain data access and PL/SQL for rules. One application can use both.
- Next: [part 2](02-calling-plsql-programs.md) builds the calls with `SimpleJdbcCall`. The optional [part 4](04-the-same-calls-with-spring-data-jpa.md) shows them with Spring Data JPA.
