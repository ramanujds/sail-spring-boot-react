# Optional: the same calls with MyBatis

Parts 2 and 3 call the package with `SimpleJdbcCall`, and each call is written as Java code. With MyBatis you write each call once, in an annotation on an interface, and MyBatis creates the code. This part shows the same calls that way. It is a separate project. You can skip it.

**Time:** about 30 minutes.
**Project:** [support-assistant-plsql-mybatis](support-assistant-plsql-mybatis/) in this folder.
**Starting point:** the package and tables of [part 2](02-calling-plsql-programs.md), already created.

## What you will see

- The same five endpoints, the same service, the same controller, the same error handler, and the same four tests.
- One interface, `TicketMapper`, with an annotation on each method.
- A small `TicketRepository` that gives the service its four methods.
- Two settings that Oracle needs.

---

## 1. The ideas

**MyBatis** is a library that runs SQL that you write, and turns the result into Java objects. It has no entity classes. You write the SQL, here the call to the package, in an annotation.

| Program | The annotation holds | What you get back |
|---|---|---|
| Function | `SELECT PKG.fn(#{priority}) FROM DUAL` | The return value of the method |
| Procedure, no `OUT` value | `{call PKG.proc(#{a}, #{b})}` | Nothing |
| Procedure with an `OUT` value | The same, with `#{id, mode=OUT, jdbcType=NUMERIC}` | The value, put back in a `Map` |
| Procedure with a `SYS_REFCURSOR` | `#{rows, mode=OUT, jdbcType=CURSOR, resultMap=...}` | A list of records, put in a `Map` |

`#{name}` is replaced by the value of the argument with that name.

The two ways side by side, as tried on `SUPPORT_TICKET_PKG`:

| | `SimpleJdbcCall` (parts 2 and 3) | MyBatis (this part) |
|---|---|---|
| Entity class | None | None |
| Where the call is written | Java code | The call, as SQL text, in an annotation |
| Function | `executeFunction` | A `SELECT` |
| `OUT` value | A `Map` from `execute` | A `Map` that you pass in |
| Result set | A row mapper that you write | Records, filled automatically |
| Transaction | Optional for one call | Optional for one call |
| Extra library | None | MyBatis |
| Classes for the four programs | 1 | 2 |

MyBatis is easiest for a result set and a function. An `OUT` value is the awkward part: it travels in a `Map`.

---

## 2. What differs from the main project

| File | Change |
|---|---|
| `pom.xml` | `spring-boot-starter-jdbc` becomes `mybatis-spring-boot-starter` |
| `application.yaml` | Two MyBatis settings added |
| `repository/TicketMapper.java` | New. The calls, as annotations |
| `repository/TicketRepository.java` | Rewritten. Calls the mapper |

The service, the controller, the records, the error handler, the tests, and the SQL scripts are the same. MyBatis throws the same `UncategorizedSQLException` for an error number from the package, so the handler of part 3 needs no change.

---

## Build steps

### Step 1: change the dependency and the settings

In `pom.xml`, replace the JDBC starter with:

```xml
<dependency>
    <groupId>org.mybatis.spring.boot</groupId>
    <artifactId>mybatis-spring-boot-starter</artifactId>
    <version>4.1.0</version>
</dependency>
```

It brings the JDBC starter with it, so the connection settings and `@Transactional` work as before.

In `application.yaml`, add:

```yaml
mybatis:
  configuration:
    jdbc-type-for-null: 'NULL'
    local-cache-scope: statement
```

| Setting | Why |
|---|---|
| `jdbc-type-for-null` | Oracle needs a type for a `null` value. Without it, a ticket with no description fails with `ORA-17004` |
| `local-cache-scope: statement` | MyBatis keeps the result of a `SELECT` for the rest of a transaction. A package call can change the data without MyBatis knowing. This keeps a result for one statement only |

### Step 2: the mapper

Create `TicketMapper` in the package `repository`. MyBatis finds it because of `@Mapper`.

The function is a `SELECT`. The return type is the result:

```java
@Mapper
public interface TicketMapper {

    @Select("SELECT SUPPORT_TICKET_PKG.open_count(#{priority}) FROM DUAL")
    int openCount(String priority);
```

A procedure is a `{call ...}`. `CALLABLE` tells MyBatis that it is a call to a stored program, not a query:

```java
    @Select("{call SUPPORT_TICKET_PKG.change_status(#{ticketId}, #{newStatus})}")
    @Options(statementType = StatementType.CALLABLE)
    void changeStatus(@Param("ticketId") long ticketId, @Param("newStatus") String newStatus);
```

A procedure with an `OUT` value. `mode=OUT` marks it. MyBatis puts the value in the `Map` under the name `ticketId`:

```java
    @Select("{call SUPPORT_TICKET_PKG.create_ticket(#{title}, #{description}, #{priority},"
            + " #{ticketId, mode=OUT, jdbcType=NUMERIC})}")
    @Options(statementType = StatementType.CALLABLE)
    void createTicket(Map<String, Object> params);
```

A procedure with a cursor. `jdbcType=CURSOR` marks it, and `resultMap=ticketMap` says how to turn each row into a `TicketResponse`:

```java
    @Select("{call SUPPORT_TICKET_PKG.find_tickets(#{status},"
            + " #{tickets, mode=OUT, jdbcType=CURSOR, javaType=java.sql.ResultSet, resultMap=ticketMap})}")
    @Options(statementType = StatementType.CALLABLE)
    @Results(id = "ticketMap", value = {})
    List<TicketResponse> findTickets(Map<String, Object> params);
}
```

`@Results(id = "ticketMap", value = {})` names the mapping and lets MyBatis fill the record by itself. It matches the columns to the fields **in order**, so the columns of the `SELECT` in `find_tickets` must be in the same order as the fields of `TicketResponse`. The rows arrive in the `Map`, under the name `tickets`. The list that the method returns is empty and is not used.

### Step 3: the repository

`TicketRepository` keeps the four methods that the service calls. It builds the `Map` for the two procedures with `OUT` values, and reads the answer from it:

```java
public long createTicket(String title, String description, String priority) {
    Map<String, Object> params = new HashMap<>();
    params.put("title", title);
    params.put("description", description);
    params.put("priority", priority);
    ticketMapper.createTicket(params);
    return ((Number) params.get("ticketId")).longValue();
}
```

```java
@SuppressWarnings("unchecked")
public List<TicketResponse> findTickets(String status) {
    Map<String, Object> params = new HashMap<>();
    params.put("status", status);
    ticketMapper.findTickets(params);
    return (List<TicketResponse>) params.get("tickets");
}
```

`openCount` and `changeStatus` pass the call straight to the mapper. The project has the full class.

### Step 4: run it

Set `DB_USERNAME` and `DB_PASSWORD`, then:

```bash
mvn clean verify
java -jar target/support-assistant-plsql-mybatis-0.0.1-SNAPSHOT.jar
```

Repeat the calls of parts 2 and 3: create three tickets, list them, send requests that the package refuses, and run the bulk change that fails.

**Check:** you should see the same answers as before: `{"id":1}`, a list of three `OPEN` tickets, `400`, `404`, and `409` with the messages that the package wrote, and a failed bulk call that leaves tickets 2 and 3 `OPEN`. `mvn clean verify` shows `Tests run: 4, Failures: 0, Errors: 0` and `BUILD SUCCESS`.

---

## Common mistakes

| Symptom | Cause | Fix |
|---|---|---|
| `ORA-17004: Invalid column type: 1111`, with `Error setting null for parameter #2 with JdbcType OTHER` | A `null` value, and no `jdbc-type-for-null` setting | Add `jdbc-type-for-null: 'NULL'` |
| The test `openCountGoesUpByOne` fails: the count does not change after a create | MyBatis kept the result of the first `SELECT` for the rest of the transaction | Add `local-cache-scope: statement` |
| `ORA-17041: Missing IN or OUT parameter at index: 4` | A call with an `OUT` parameter has no `@Options(statementType = StatementType.CALLABLE)` | Add it |
| The title shows the status, and the status shows the title. No error | The fields of `TicketResponse` are in another order than the columns of the cursor. Two `String` fields swap without a message | Keep the fields in the order of the `SELECT` |
| `ORA-17059: Failed to convert to internal representation`, with `Error attempting to get column 'TITLE'` | The same order mistake, with fields of different types | Keep the fields in the order of the `SELECT` |

Each row was produced by making the mistake on purpose.

---

## Practice

1. Remove `local-cache-scope` from `application.yaml` and run `mvn clean verify`. Say which test fails, and why it passes when you put the setting back.
2. Change the order of `title` and `status` in `TicketResponse`, rebuild, and call `GET /api/tickets`. Describe what you see. Put the order back.
3. Add `due_in_hours (p_ticket_id IN NUMBER) RETURN NUMBER` to the package. Call it in both projects. Count the lines that each one needed.

---

## Recap

- MyBatis has no entity. You write the call in an annotation, and MyBatis creates the code of the method.
- A function is a `SELECT`. A procedure is a `{call ...}` with `CALLABLE`. An `OUT` value or a cursor comes back in a `Map`.
- A cursor fills a record automatically, if the fields are in the order of the columns.
- Two settings are needed for Oracle: one for `null`, and one so that a package call is not hidden by the cache.
- The service, the controller, the handler, and the tests do not change.
