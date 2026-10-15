# Optional: the same calls with Spring Data JPA

Parts 2 and 3 call the package with `SimpleJdbcCall`. This part shows the same calls through Spring Data JPA. It is a second, separate project. You can skip it.

**Time:** about 30 minutes.
**Project:** [support-assistant-plsql-jpa](support-assistant-plsql-jpa/) in this folder.
**Starting point:** the package and tables of [part 2](02-calling-plsql-programs.md), already created.

## What you will see

- The same five endpoints, the same service, the same controller, and the same four tests.
- Three classes that replace `TicketRepository`.
- One change in the error handler.

---

## 1. The ideas

Spring Data JPA has one tool for each kind of program:

| Program | JPA tool | What you get |
|---|---|---|
| Procedure with an `OUT` value, or none | `@Procedure` on a Spring Data repository method | The `OUT` value as the return value |
| Procedure that opens a `SYS_REFCURSOR` | `StoredProcedureQuery` from the `EntityManager` | Each row as an `Object[]` |
| Function | A native query: `SELECT pkg.fn(:p) FROM DUAL` | One value |

A function cannot go through the procedure tool. Oracle answers `PLS-00221: 'OPEN_COUNT' is not a procedure or is undefined`.

The two ways side by side, as tried on `SUPPORT_TICKET_PKG`:

| | `SimpleJdbcCall` (parts 2 and 3) | Spring Data JPA (this part) |
|---|---|---|
| Entity class | None | One, only to name the repository type |
| Function | `executeFunction` | A native query |
| Procedure with `OUT` | `execute`, read a `Map` | `@Procedure`, the return value |
| Result set | A row mapper gives records | `Object[]` rows, mapped by hand |
| Transaction | Optional for one call | Needed around every call |
| Classes to write for the four programs | 1 | 3 |

Choose `SimpleJdbcCall` when most of the work is calling PL/SQL. Choose JPA when the application already uses JPA for its tables and you want one style.

---

## 2. What differs between the two projects

| File | Change |
|---|---|
| `pom.xml` | `spring-boot-starter-jdbc` becomes `spring-boot-starter-data-jpa` |
| `application.yaml` | Two lines added under `spring.jpa` |
| `repository/TicketRepository.java` | Rewritten with JPA |
| `repository/TicketEntity.java` | New |
| `repository/TicketProcedures.java` | New |
| `exception/GlobalExceptionHandler.java` | Looks for the `SQLException` inside the exceptions of JPA |
| `TicketPackageTests.java` | One word: the exception class that the test expects |

The service, the controller, the records, and the SQL scripts are the same.

---

## Build steps

### Step 1: change the dependency and the settings

In `pom.xml`, replace the JDBC starter with:

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-jpa</artifactId>
</dependency>
```

In `application.yaml`, under `spring:`, add:

```yaml
  jpa:
    hibernate:
      ddl-auto: none
    open-in-view: false
```

`ddl-auto: none` tells Hibernate to leave the tables alone. The tables belong to your scripts.

> **Note:** with JPA, the application connects to Oracle while it starts. A wrong or unset `DB_PASSWORD` stops the start-up with `Unable to obtain isolated JDBC connection [ORA-01017: invalid username/password; logon denied`.

### Step 2: the entity

A Spring Data repository must name an entity type. Create `TicketEntity` in the package `repository`:

```java
@Entity
@Table(name = "SUPPORT_TICKETS")
public class TicketEntity {

    @Id
    private Long id;

    protected TicketEntity() {
    }
}
```

Nothing reads or saves this class. It exists so that the next step compiles.

### Step 3: the procedures

Create the interface `TicketProcedures` in the same package. You write no body. Spring Data creates the code:

```java
public interface TicketProcedures extends Repository<TicketEntity, Long> {

    @Procedure(procedureName = "SUPPORT_TICKET_PKG.CREATE_TICKET", outputParameterName = "P_TICKET_ID")
    Long createTicket(@Param("p_title") String title,
                      @Param("p_description") String description,
                      @Param("p_priority") String priority);

    @Procedure(procedureName = "SUPPORT_TICKET_PKG.CHANGE_STATUS")
    void changeStatus(@Param("p_ticket_id") Long ticketId, @Param("p_new_status") String newStatus);
}
```

| Part | Meaning |
|---|---|
| `extends Repository` | The smallest Spring Data interface. It has no `save()` or `delete()`, so no code can change the table and skip the package |
| `procedureName` | The package and the program, as in SQL |
| `outputParameterName` | The `OUT` parameter whose value is returned |
| `@Param("p_title")` | The PL/SQL parameter that this argument fills |
| `void` | The procedure has no `OUT` parameter |

### Step 4: rewrite `TicketRepository`

The class keeps its name and its four methods, so the service does not change. Its constructor takes the interface and the `EntityManager`:

```java
@Repository
@Transactional
public class TicketRepository {

    private final TicketProcedures ticketProcedures;
    private final EntityManager entityManager;

    public TicketRepository(TicketProcedures ticketProcedures, EntityManager entityManager) {
        this.ticketProcedures = ticketProcedures;
        this.entityManager = entityManager;
    }
}
```

The two procedures pass the call on:

```java
public long createTicket(String title, String description, String priority) {
    return ticketProcedures.createTicket(title, description, priority);
}

public void changeStatus(long ticketId, String newStatus) {
    ticketProcedures.changeStatus(ticketId, newStatus);
}
```

The function goes through a native query:

```java
public int openCount(String priority) {
    Object count = entityManager
            .createNativeQuery("SELECT SUPPORT_TICKET_PKG.open_count(:priority) FROM DUAL")
            .setParameter("priority", priority)
            .getSingleResult();
    return ((Number) count).intValue();
}
```

The result set goes through a `StoredProcedureQuery`:

```java
public List<TicketResponse> findTickets(String status) {
    StoredProcedureQuery query = entityManager
            .createStoredProcedureQuery("SUPPORT_TICKET_PKG.FIND_TICKETS")
            .registerStoredProcedureParameter("p_status", String.class, ParameterMode.IN)
            .registerStoredProcedureParameter("p_tickets", void.class, ParameterMode.REF_CURSOR)
            .setParameter("p_status", status);
    List<Object[]> rows = query.getResultList();
    return rows.stream().map(this::toTicket).toList();
}
```

Each `Object[]` holds the columns in the order of the `SELECT` in `find_tickets`. `toTicket` casts each one: `((Number) row[0]).longValue()`, `(String) row[1]`, and so on. The project has the full method.

> **Warning:** `@Transactional` on the class is not optional. JPA reads an `OUT` value after the call has run, and it needs the same transaction to do so. Without it, the ticket is created and the answer is `500`.

### Step 5: change the error handler

In part 3, the handler read the `SQLException` from `UncategorizedSQLException`. JPA wraps it in other exceptions, so the handler looks for it inside:

```java
@ExceptionHandler(DataAccessException.class)
public ResponseEntity<ApiError> handleDatabase(DataAccessException ex) {
    SQLException sqlException = findSqlException(ex);
    if (sqlException != null) {
        int code = sqlException.getErrorCode();
        if (code >= 20000 && code <= 20999) {
            return build(statusFor(code), messageOf(sqlException));
        }
    }
    return build(HttpStatus.INTERNAL_SERVER_ERROR, "The database call failed");
}
```

`findSqlException` follows `getCause()` until it finds a `SQLException`. The rest of the class is the same as in part 3.

In `TicketPackageTests`, change the expected exception from `UncategorizedSQLException` to `DataAccessException`, because JPA wraps the error in its own class.

### Step 6: run it

Set `DB_USERNAME` and `DB_PASSWORD`, then:

```bash
mvn clean verify
java -jar target/support-assistant-plsql-jpa-0.0.1-SNAPSHOT.jar
```

Repeat the calls of parts 2 and 3: create three tickets, list them, send requests that the package refuses, and run the bulk change that fails.

**Check:** you should see the same answers as before: `{"id":1}`, a list of three `OPEN` tickets, `400`, `404`, and `409` with the messages that the package wrote, and a failed bulk call that leaves tickets 2 and 3 `OPEN`. `mvn clean verify` shows `Tests run: 4, Failures: 0, Errors: 0` and `BUILD SUCCESS`.

---

## Common mistakes

| Symptom | Cause | Fix |
|---|---|---|
| `500`, and `InvalidDataAccessApiUsageException: OUT/INOUT parameter not available: P_TICKET_ID`. The ticket **is** created | `@Procedure` ran with no transaction. The `OUT` value could not be read afterwards | Put `@Transactional` on `TicketRepository` |
| `SQLGrammarException` and `PLS-00221: 'OPEN_COUNT' is not a procedure or is undefined` | A function was registered as a stored procedure | Call the function in a native query: `SELECT ... FROM DUAL` |
| `ClassCastException: class java.sql.Timestamp cannot be cast to class java.time.LocalDateTime` | Spring Boot 3.5 (Hibernate 6) gives a `Timestamp` for a cursor column. Spring Boot 4 (Hibernate 7) gives a `LocalDateTime` | Convert with `instanceof Timestamp`, as `toLocalDateTime` does in the project |
| `Unable to obtain isolated JDBC connection [ORA-01017 ...` at start-up | `DB_USERNAME` or `DB_PASSWORD` is wrong or not set. Hibernate connects when the application starts | Set both variables |

Each row was produced by making the mistake on purpose. The `Timestamp` row was seen on Spring Boot 3.5.16.

---

## Practice

1. Remove `@Transactional` from `TicketRepository`. Create a ticket. Read the log and say why the ticket exists although the answer was `500`. Put the annotation back.
2. Add `due_in_hours (p_ticket_id IN NUMBER) RETURN NUMBER` to the package. Call it in both projects, and say which one needed more code.
3. Write a procedure `find_overdue (p_tickets OUT SYS_REFCURSOR)`. Say why it cannot be a method with `@Procedure` in `TicketProcedures`.

---

## Recap

- Spring Data JPA has a tool for each kind of program: `@Procedure` for a procedure, `StoredProcedureQuery` for a cursor, and a native query for a function.
- A Spring Data repository names an entity type, so the JPA way needs one entity class, even though nothing reads or saves it.
- Every JPA call needs a transaction, because the `OUT` value is read after the call.
- The service, the controller, and the tests do not change. Only the repository and one handler do.
