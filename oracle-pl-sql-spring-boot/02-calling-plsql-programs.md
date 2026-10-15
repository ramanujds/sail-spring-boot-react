# Calling PL/SQL programs

In this part you create a PL/SQL package, then call its function, its procedures, and its result set from Spring Boot. There is no entity class anywhere.

**Time:** about 60 minutes.
**Project:** [support-assistant-plsql](support-assistant-plsql/) in this folder.

## What you will build

- A package `SUPPORT_TICKET_PKG` with one function and three procedures.
- `GET /api/tickets/open-count?priority=HIGH`, which calls the function.
- `POST /api/tickets`, which calls a procedure and reads its `OUT` parameter.
- `GET /api/tickets?status=OPEN`, which calls a procedure and reads its `SYS_REFCURSOR`.

---

## 1. The ideas

A call to a stored program needs four things.

| Need | In this project |
|---|---|
| A connection to Oracle | The settings in `application.yaml`. Spring Boot creates a connection pool and a `JdbcTemplate` from them |
| The name of the program | `withCatalogName("SUPPORT_TICKET_PKG").withProcedureName("CREATE_TICKET")` |
| The values to pass in | The arguments of `execute(...)` |
| A place to read the answer | A `Number` for a function, a `Map` for `OUT` values, a list for a `SYS_REFCURSOR` |

`SimpleJdbcCall` is the Spring class that does this. It asks the database for the program's parameters, so you give only the name.

A **record** holds the values of one result row. It is a class with fields and no behaviour, as in Day 3:

```java
public record TicketResponse(long id, String title, String status) { }
```

The Oracle types come to Java like this:

| PL/SQL | Java | Note |
|---|---|---|
| `NUMBER` | `BigDecimal` | Read it as a `Number`, then call `intValue()` or `longValue()` |
| `VARCHAR2` | `String` | |
| `DATE`, `TIMESTAMP` | `Timestamp` | Read with `rs.getTimestamp(...)`, then `toLocalDateTime()` |
| `SYS_REFCURSOR` | A list of your records | You give a row mapper |
| `BOOLEAN` | Not supported | Return `NUMBER` (1 or 0) or `VARCHAR2` (`'Y'` or `'N'`) |

---

## Build steps

### Step 1: create the tables and the package

The folder [db/oracle](support-assistant-plsql/db/oracle/) has two scripts:

| File | Creates |
|---|---|
| `01-tables.sql` | The sequence `SUPPORT_TICKETS_SEQ`, and the tables `SUPPORT_TICKETS` and `SUPPORT_TICKET_LOG` |
| `02-package.sql` | The package `SUPPORT_TICKET_PKG`: specification and body |

The names start with `SUPPORT_`, so they do not clash with the `TICKETS` table of Day 5.

Read the package specification in `02-package.sql` first. It lists the four programs:

```sql
FUNCTION open_count (p_priority IN VARCHAR2) RETURN NUMBER;

PROCEDURE create_ticket (
    p_title IN VARCHAR2, p_description IN VARCHAR2,
    p_priority IN VARCHAR2, p_ticket_id OUT NUMBER);

PROCEDURE change_status (p_ticket_id IN NUMBER, p_new_status IN VARCHAR2);

PROCEDURE find_tickets (p_status IN VARCHAR2, p_tickets OUT SYS_REFCURSOR);
```

| Program | Kind | What it does |
|---|---|---|
| `open_count` | Function | Counts the `OPEN` and `IN_PROGRESS` tickets of a priority |
| `create_ticket` | Procedure, `OUT` parameter | Checks the values, sets the due date from the priority, inserts the ticket, and gives back its id |
| `change_status` | Procedure | Allows only the next step: `OPEN`, `IN_PROGRESS`, `RESOLVED`, `CLOSED` |
| `find_tickets` | Procedure, `SYS_REFCURSOR` | Opens a cursor on the tickets with a status, or on all tickets when the status is `NULL` |

Run both scripts, in order, as the user that your application will connect as.

**Option A:** in SQL Developer, open each file and run it as a script with **F5**.

**Option B:** with SQL\*Plus, from the `support-assistant-plsql` folder:

```bash
sqlplus YOUR_USERNAME/YOUR_PASSWORD@//ORACLE_HOST:1521/ORACLE_SERVICE @db/oracle/01-tables.sql
sqlplus YOUR_USERNAME/YOUR_PASSWORD@//ORACLE_HOST:1521/ORACLE_SERVICE @db/oracle/02-package.sql
```

**Check:** you should see:

```text
Sequence created.
Table created.
Table created.
Package created.
Package body created.
```

If you see `Warning: Package Body created with compilation errors.`, type `SHOW ERRORS PACKAGE BODY SUPPORT_TICKET_PKG` and read the line and the message.

### Step 2: try the package in SQL

Call the programs before you write any Java:

```sql
SELECT SUPPORT_TICKET_PKG.open_count('HIGH') FROM DUAL;

VARIABLE id NUMBER
EXEC SUPPORT_TICKET_PKG.create_ticket('Printer is jammed', 'Second floor', 'HIGH', :id)
PRINT id
```

**Check:** the count is `0`, and `PRINT id` shows `1`.

Undo the test ticket:

```sql
ROLLBACK;
```

The sequence does not go back, as with any Oracle sequence. The number `1` is used, so the first ticket that you create from Java gets the id `2`.

### Step 3: create the project and connect

Create a Spring Boot project in Spring Initializr with these dependencies:

| Dependency | Why |
|---|---|
| Spring Web | Controllers and JSON |
| JDBC API | The connection pool, `JdbcTemplate`, `SimpleJdbcCall`, `@Transactional` |
| Oracle Driver | Lets Java talk to Oracle |

Do **not** add Spring Data JPA. Or open the finished project [support-assistant-plsql](support-assistant-plsql/).

Open `src/main/resources/application.yaml`:

```yaml
spring:
  datasource:
    url: ${DB_URL:jdbc:oracle:thin:@//localhost:1521/XEPDB1}
    username: ${DB_USERNAME}
    password: ${DB_PASSWORD}
```

`${DB_USERNAME}` reads an environment variable. The password is not in the file.

Set the variables before you run:

| Where you run | How |
|---|---|
| IntelliJ | **Run | Edit Configurations**, then **Environment variables**: `DB_USERNAME=YOUR_USERNAME;DB_PASSWORD=YOUR_PASSWORD` |
| Terminal | `export DB_USERNAME=YOUR_USERNAME` and `export DB_PASSWORD=YOUR_PASSWORD` |

If your database is not on `localhost:1521/XEPDB1`, set `DB_URL` the same way, for example `jdbc:oracle:thin:@//ORACLE_HOST:1521/ORACLE_SERVICE`.

**Check:** run `SupportAssistantApplication`. You should see `Started SupportAssistantApplication`.

> **Note:** the application starts even when the user name or password is wrong. Nothing connects to Oracle until the first call. A wrong value shows up as `ORA-01017` on the first request.

> **Note:** the connection goes directly to Oracle, so the company proxy plays no part. A connection problem is a host, port, firewall, or login problem. See the table at the end.

### Step 4: call the function

Create `TicketRepository` in the package `repository`. It builds one `SimpleJdbcCall` for each program:

```java
@Repository
public class TicketRepository {

    private final SimpleJdbcCall openCount;

    public TicketRepository(JdbcTemplate jdbcTemplate) {
        this.openCount = new SimpleJdbcCall(jdbcTemplate)
                .withCatalogName("SUPPORT_TICKET_PKG")
                .withFunctionName("OPEN_COUNT");
    }

    // The arguments go in order. The return value is read as a Number.
    public int openCount(String priority) {
        Number count = openCount.executeFunction(Number.class, priority);
        return count.intValue();
    }
}
```

| Part | Meaning |
|---|---|
| `JdbcTemplate` | Spring's plain SQL helper. Spring Boot creates it from `application.yaml`, so you only ask for it in the constructor |
| `withCatalogName("SUPPORT_TICKET_PKG")` | The package name. For a program outside a package, leave it out |
| `withFunctionName("OPEN_COUNT")` | The name of the function. Use `withProcedureName` for a procedure |
| `executeFunction(Number.class, priority)` | Call it, and read the return value as a `Number` |

Add the service and the controller. They only pass the call on, so the web layer and the database layer stay apart:

```java
@Service
public class TicketService {

    private final TicketRepository ticketRepository;

    public TicketService(TicketRepository ticketRepository) {
        this.ticketRepository = ticketRepository;
    }

    public int openCount(String priority) {
        return ticketRepository.openCount(priority);
    }
}
```

```java
@RestController
@RequestMapping("/api/tickets")
public class TicketController {

    private final TicketService ticketService;

    public TicketController(TicketService ticketService) {
        this.ticketService = ticketService;
    }

    @GetMapping("/open-count")
    public int openCount(@RequestParam String priority) {
        return ticketService.openCount(priority);
    }
}
```

Restart, then call it:

```bash
curl "http://localhost:8080/api/tickets/open-count?priority=HIGH"
```

**Check:** you should see `0`.

### Step 5: call a procedure and read its `OUT` parameter

Add a record for the request body:

```java
public record CreateTicketRequest(String title, String description, String priority) { }
```

In `TicketRepository`, add a second field, build it in the constructor, and add the method:

```java
this.createTicket = new SimpleJdbcCall(jdbcTemplate)
        .withCatalogName("SUPPORT_TICKET_PKG")
        .withProcedureName("CREATE_TICKET");
```

```java
// The values go in the order of the IN parameters. The OUT value comes back in a Map.
public long createTicket(String title, String description, String priority) {
    Map<String, Object> out = createTicket.execute(title, description, priority);
    return ((Number) out.get("P_TICKET_ID")).longValue();
}
```

| Part | Meaning |
|---|---|
| `execute(title, description, priority)` | Call the procedure. Pass one value for each `IN` parameter, in the order of the specification. The `OUT` parameter is not passed |
| `out.get("P_TICKET_ID")` | Read the `OUT` value. Use the upper-case name |

In `TicketService`, add `create`, which passes the three values to `createTicket`. In `TicketController`, add the route:

```java
@PostMapping
@ResponseStatus(HttpStatus.CREATED)
public TicketCreated create(@RequestBody CreateTicketRequest request) {
    return new TicketCreated(ticketService.create(request));
}
```

`TicketCreated` is another record, with one field: `long id`.

Restart, then create three tickets:

```bash
curl -X POST http://localhost:8080/api/tickets -H "Content-Type: application/json" \
  -d '{"title":"Printer is jammed","description":"Second floor printer","priority":"HIGH"}'
curl -X POST http://localhost:8080/api/tickets -H "Content-Type: application/json" \
  -d '{"title":"VPN drops every hour","priority":"MEDIUM"}'
curl -X POST http://localhost:8080/api/tickets -H "Content-Type: application/json" \
  -d '{"title":"Reset my password","priority":"LOW"}'
```

**Check:** you should see `{"id":2}`, then `{"id":3}`, then `{"id":4}`.

Call the function again:

```bash
curl "http://localhost:8080/api/tickets/open-count?priority=HIGH"
```

**Check:** you should see `1`.

### Step 6: read a result set

A procedure can open a `SYS_REFCURSOR`. Spring reads each row with a **row mapper**: a method that turns the current row into one record.

Add the record `TicketResponse` with the fields `long id`, `String title`, `String description`, `String status`, `String priority`, `LocalDateTime createdAt`, and `LocalDateTime dueBy`.

In `TicketRepository`, build the call. `returningResultSet` names the cursor parameter and gives the row mapper:

```java
this.findTickets = new SimpleJdbcCall(jdbcTemplate)
        .withCatalogName("SUPPORT_TICKET_PKG")
        .withProcedureName("FIND_TICKETS")
        .returningResultSet("P_TICKETS", this::toTicket);
```

Add the two methods:

```java
@SuppressWarnings("unchecked")
public List<TicketResponse> findTickets(String status) {
    Map<String, Object> out = findTickets.execute(status);
    return (List<TicketResponse>) out.get("P_TICKETS");
}

private TicketResponse toTicket(ResultSet rs, int rowNumber) throws SQLException {
    return new TicketResponse(
            rs.getLong("ID"),
            rs.getString("TITLE"),
            rs.getString("DESCRIPTION"),
            rs.getString("STATUS"),
            rs.getString("PRIORITY"),
            rs.getTimestamp("CREATED_AT").toLocalDateTime(),
            rs.getTimestamp("DUE_BY").toLocalDateTime());
}
```

Spring calls `toTicket` once for each row. The column names are the ones in the `SELECT` of `find_tickets`.

In the service, add `find(String status)`. In the controller:

```java
@GetMapping
public List<TicketResponse> find(@RequestParam(required = false) String status) {
    return ticketService.find(status);
}
```

When `status` is missing, Java passes `null`, and the procedure returns every ticket.

Restart, then call:

```bash
curl "http://localhost:8080/api/tickets?status=OPEN"
```

**Check:** you should see a JSON list with your three tickets. The first is:

```json
{"id":2,"title":"Printer is jammed","description":"Second floor printer","status":"OPEN",
 "priority":"HIGH","createdAt":"2026-10-15T22:18:12.239807","dueBy":"2026-10-16T22:18:12.239807"}
```

`dueBy` is one day after `createdAt`, because the priority is `HIGH`. Java did not work that out. The package did.

---

## Common mistakes

Each message below was produced by making the mistake on purpose.

| Symptom | Cause | Fix |
|---|---|---|
| `500`, and `ORA-01017: invalid username/password; logon denied` in the log | `DB_USERNAME` or `DB_PASSWORD` is wrong, or not set. An unset variable is read as the text `${DB_PASSWORD}` | Set both variables in the run configuration or the terminal |
| `ORA-12541: Cannot connect. No listener at host localhost port 1599` | The host or port in `DB_URL` is wrong, or the listener is down | Check the port with your database team. A firewall can also block it |
| `ORA-12514: ... Service NOSUCHSERVICE is not registered with the listener` | The service name at the end of `DB_URL` is wrong | Use the service name, not the SID |
| `ORA-17868: Unknown host specified` | The host name in `DB_URL` is wrong | Check the spelling, and try `ping` on the host |
| `BadSqlGrammarException` and `PLS-00201: identifier 'NO_SUCH_PKG.CREATE_TICKET' must be declared` | The package name in `withCatalogName` is wrong, or the package belongs to another user | `SELECT OBJECT_NAME, OBJECT_TYPE, STATUS FROM USER_OBJECTS WHERE OBJECT_NAME LIKE 'SUPPORT%'` |
| `PLS-00302: component 'CREATE_TICKET_X' must be declared` | The package exists, but the program name is wrong, or it is only in the body and not in the specification | Check the specification |
| `ORA-04063: package body "SUPPORT.SUPPORT_TICKET_PKG" has errors` | The package body did not compile | `SHOW ERRORS PACKAGE BODY SUPPORT_TICKET_PKG`, fix, and run the file again |
| `ORA-17004: Invalid column type: 1111` | The function returns `BOOLEAN`. JDBC cannot read it | Return `NUMBER` (1 or 0) or `VARCHAR2` (`'Y'` or `'N'`) |
| `PLS-00306: wrong number or types of arguments in call to 'OPEN_COUNT'` | A function was called with `withProcedureName` | Use `withFunctionName` and `executeFunction` |
| `ClassCastException: class java.math.BigDecimal cannot be cast to class java.lang.Integer` | The call asked for `Integer.class`. Oracle's `NUMBER` is a `BigDecimal` | Ask for `Number.class`, then `intValue()` |
| `NullPointerException` on the line that reads an `OUT` value | The key was written in lower case: `out.get("p_ticket_id")` returns `null` | Use the upper-case name: `P_TICKET_ID` |
| `ArrayIndexOutOfBoundsException: Index 1 out of bounds for length 1` | Fewer values than `IN` parameters were passed to `execute(...)` | Pass one value for each `IN` parameter, in the order of the specification |

**Check yourself**

- You can run a function and a procedure in SQL before you call them from Java.
- You can say why this project has no `@Entity` class.
- You can say why `executeFunction` is used for `OPEN_COUNT` and `execute` for `CREATE_TICKET`.
- You can say where the due date is worked out.

---

## Practice

1. Add a function `due_in_hours (p_ticket_id IN NUMBER) RETURN NUMBER` to the package that returns the hours left until `DUE_BY`. Call it from `GET /api/tickets/{id}/due-in-hours`.
2. Call `GET /api/tickets/open-count?priority=high` in lower case. Change the package so that it also accepts lower case. Run `02-package.sql` again. Call the endpoint without restarting the application, and say why Java needed no change.
3. Add a procedure `find_overdue (p_tickets OUT SYS_REFCURSOR)` that returns the tickets that are not finished and are past `DUE_BY`. Reuse `toTicket` for the rows. Make a ticket overdue with an `UPDATE` on `DUE_BY`.

---

## Recap

- A call to PL/SQL needs a connection, the program name, the values, and a place for the answer. It needs no entity.
- `SimpleJdbcCall` calls a function with `executeFunction` and a procedure with `execute`. A package name goes in `withCatalogName`.
- An `OUT` value comes back in a `Map` with an upper-case key. A `SYS_REFCURSOR` is read with `returningResultSet` and a row mapper.
- Read `NUMBER` as a `Number`. Do not return `BOOLEAN` to Java.

### State at end of this part

| File | What it holds |
|---|---|
| `db/oracle/01-tables.sql` | Sequence and two tables |
| `db/oracle/02-package.sql` | `SUPPORT_TICKET_PKG` |
| `repository/TicketRepository.java` | Three `SimpleJdbcCall` objects, and `toTicket` |
| `service/TicketService.java` | `openCount`, `create`, `find` |
| `api/TicketController.java` | Three endpoints |

| Request | Result |
|---|---|
| `GET /api/tickets/open-count?priority=HIGH` | A number |
| `POST /api/tickets` | `201` and `{"id":2}` |
| `GET /api/tickets?status=OPEN` | A JSON list |

Next: [part 3](03-errors-and-transactions.md) handles the errors that the package raises, and the commit.
