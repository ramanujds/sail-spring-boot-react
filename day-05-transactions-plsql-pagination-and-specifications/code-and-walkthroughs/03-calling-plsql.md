# Calling PL/SQL from Spring Boot

Some logic already lives inside the Oracle database, written by a database team in **PL/SQL**. A Spring Boot application often has to call it. In this note you create two small PL/SQL programs, try them in SQL, and call them from Java.

**Starting point:** your project at the end of [part 2](02-sla-logic.md), with the `DUE_BY` column.
**Project:** [support-assistant-jpa](support-assistant-jpa/) in this folder.

## What you will build

- A PL/SQL **function** that counts the overdue tickets of a priority.
- A PL/SQL **procedure** that raises every overdue ticket to `HIGH`, and says how many it changed.
- `GET /api/v1/tickets/overdue/count` and `POST /api/v1/tickets/escalate-overdue`, which call them.

---

## 1. The idea

**PL/SQL** is Oracle's programming language. It runs inside the database, next to the data. A PL/SQL program stored in the database has a name, can take parameters, and can be called from any program that can connect, including Java.

| | Function | Procedure |
|---|---|---|
| Gives back | One value, with `RETURN` | Nothing, or values through `OUT` parameters |
| Use it for | Asking a question: "how many?" | Doing work: "change these rows" |
| Called in SQL | `SELECT OVERDUE_COUNT('HIGH') FROM DUAL` | `EXEC ESCALATE_OVERDUE(:n)` |

The parts of a PL/SQL program:

| Part | Meaning |
|---|---|
| `CREATE OR REPLACE FUNCTION name (...) RETURN type IS` | The name, the parameters, and the type that it gives back |
| `p_priority IN VARCHAR2` | A parameter that comes **in** |
| `p_escalated OUT NUMBER` | A parameter that the procedure fills, and the caller reads |
| `BEGIN ... END;` | The code |
| `/` alone on a line | Tells the tool that the program ends here |

Why call PL/SQL at all? The rule may already exist and be shared by other applications, and one call replaces many statements.

---

## Build steps

### Step 1: create the two programs

The file [db/oracle/plsql.sql](support-assistant-jpa/db/oracle/plsql.sql) has both. Read them first.

The function:

```sql
CREATE OR REPLACE FUNCTION OVERDUE_COUNT (p_priority IN VARCHAR2) RETURN NUMBER IS
    v_count NUMBER;
BEGIN
    SELECT COUNT(*)
      INTO v_count
      FROM TICKETS
     WHERE STATUS IN ('OPEN', 'IN_PROGRESS')
       AND PRIORITY = p_priority
       AND DUE_BY < SYSTIMESTAMP;
    RETURN v_count;
END OVERDUE_COUNT;
/
```

The procedure:

```sql
CREATE OR REPLACE PROCEDURE ESCALATE_OVERDUE (p_escalated OUT NUMBER) IS
BEGIN
    UPDATE TICKETS
       SET PRIORITY = 'HIGH'
     WHERE STATUS IN ('OPEN', 'IN_PROGRESS')
       AND PRIORITY <> 'HIGH'
       AND DUE_BY < SYSTIMESTAMP;
    p_escalated := SQL%ROWCOUNT;
END ESCALATE_OVERDUE;
/
```

| Part | Meaning |
|---|---|
| `INTO v_count` | Put the result of the `SELECT` into the variable |
| `:=` | Assign a value. `SQL%ROWCOUNT` is the number of rows the last `UPDATE` changed |
| No `COMMIT` | The caller decides when to save. In step 5, Spring does |

The overdue rule is the same as `SlaPolicy.isOverdue` from part 2, written in SQL.

Run the file, as the same user that owns `TICKETS`:

**Option A:** in SQL Developer, open the file and run it as a script with **F5**.

**Option B:** from the `support-assistant-jpa` folder:

```bash
docker exec -i support-oracle sqlplus -s support/YOUR_PASSWORD@//localhost:1521/XEPDB1 < db/oracle/plsql.sql
```

**Check:** you should see:

```text
Function created.

Procedure created.
```

You can run the file again. `CREATE OR REPLACE` replaces the program.

### Step 2: try them in SQL

```sql
SELECT OVERDUE_COUNT('HIGH') FROM DUAL;
```

**Check:** you get one number: how many `HIGH` tickets are overdue.

Call the procedure:

```sql
VARIABLE n NUMBER
EXEC ESCALATE_OVERDUE(:n)
PRINT n
```

**Check:** `PL/SQL procedure successfully completed.`, and `PRINT n` shows how many tickets were raised to `HIGH`.

`VARIABLE n` makes a place for the `OUT` value, and `PRINT n` shows it.

### Step 3: call them from Java

Spring has a class for this, `SimpleJdbcCall`. It reads the program's parameters from the database, so you give only the name. Create `OverdueRepository` in `repository`:

```java
@Repository
public class OverdueRepository {

    private final SimpleJdbcCall overdueCount;
    private final SimpleJdbcCall escalateOverdue;

    public OverdueRepository(JdbcTemplate jdbcTemplate) {
        this.overdueCount = new SimpleJdbcCall(jdbcTemplate).withFunctionName("OVERDUE_COUNT");
        this.escalateOverdue = new SimpleJdbcCall(jdbcTemplate).withProcedureName("ESCALATE_OVERDUE");
    }

    // Calls the function OVERDUE_COUNT(p_priority). Oracle returns a NUMBER, read here as a Number.
    public int countOverdue(Priority priority) {
        Number count = overdueCount.executeFunction(Number.class, priority.name());
        return count.intValue();
    }

    // Calls the procedure ESCALATE_OVERDUE and reads its OUT parameter.
    public int escalateOverdue() {
        Map<String, Object> result = escalateOverdue.execute();
        return ((Number) result.get("P_ESCALATED")).intValue();
    }
}
```

| Part | Meaning |
|---|---|
| `JdbcTemplate` | Spring's plain SQL helper. Spring Boot creates it from the same database settings as JPA, so nothing is added to `pom.xml` or `application.yaml` |
| `withFunctionName("OVERDUE_COUNT")` | The name of the function in the database |
| `executeFunction(Number.class, "HIGH")` | Call it with the arguments in order, and read the return value as a `Number` |
| `withProcedureName("ESCALATE_OVERDUE")` | The name of the procedure |
| `execute()` | Call it. The `OUT` values come back in a `Map`, with the parameter name in upper case |

JPA and `JdbcTemplate` use the same database connection settings, and a transaction covers both.

**Check:** `mvn -q compile` prints no errors.

### Step 4: the service and the controller

In `TicketService`, receive the repository in the constructor, and add two methods:

```java
public int countOverdue(Priority priority) {
    return overdueRepository.countOverdue(priority);
}

// The procedure updates many rows. If anything fails, none of them are changed.
@Transactional
public int escalateOverdue() {
    return overdueRepository.escalateOverdue();
}
```

The procedure changes many rows, so the call is in a transaction, as in [part 1](01-transactions.md). The procedure has no `COMMIT`: when the method returns, Spring commits.

In `TicketController`:

```java
// GET /api/v1/tickets/overdue/count?priority=LOW calls the PL/SQL function.
@GetMapping("/overdue/count")
public int countOverdue(@RequestParam Priority priority) {
    return ticketService.countOverdue(priority);
}

// POST /api/v1/tickets/escalate-overdue calls the PL/SQL procedure.
@PostMapping("/escalate-overdue")
public int escalateOverdue() {
    return ticketService.escalateOverdue();
}
```

**Check:** `mvn -q clean verify` ends with no errors.

### Step 5: try it

Restart. Create two `LOW` tickets, and make them overdue, as in part 2:

```bash
curl -X POST http://localhost:8080/api/v1/tickets -H "Content-Type: application/json" \
  -d '{"title":"Esc demo 1","priority":"LOW"}'
curl -X POST http://localhost:8080/api/v1/tickets -H "Content-Type: application/json" \
  -d '{"title":"Esc demo 2","priority":"LOW"}'
```

```sql
UPDATE TICKETS SET DUE_BY = SYSTIMESTAMP - INTERVAL '1' HOUR WHERE TITLE LIKE 'Esc demo%';
COMMIT;
```

Count, escalate, and count again:

```bash
curl "http://localhost:8080/api/v1/tickets/overdue/count?priority=LOW"
curl -X POST http://localhost:8080/api/v1/tickets/escalate-overdue
curl "http://localhost:8080/api/v1/tickets/overdue/count?priority=LOW"
curl -X POST http://localhost:8080/api/v1/tickets/escalate-overdue
```

**Check:** you should see:

| Call | Answer | Meaning |
|---|---|---|
| Count `LOW` | `2` | The two tickets, from the function |
| Escalate | `2` | The procedure raised two tickets |
| Count `LOW` | `0` | They are `HIGH` now |
| Escalate again | `0` | Nothing left to raise |

Other overdue `LOW` or `MEDIUM` tickets, if you have them, add to the numbers.

---

## Common mistakes

| Symptom | Cause | Fix |
|---|---|---|
| `500` and `ClassCastException: class java.math.BigDecimal cannot be cast to class java.lang.Integer` | The call asked for `Integer.class`. Oracle returns a `NUMBER`, which Java reads as a `BigDecimal` | Ask for `Number.class` and use `intValue()` |
| `500` and `ORA-17041: Missing IN or OUT parameter at index: 1` | The name in `withFunctionName` or `withProcedureName` does not match a program in the database | Check the spelling. `SELECT OBJECT_NAME, STATUS FROM USER_OBJECTS WHERE OBJECT_TYPE IN ('FUNCTION','PROCEDURE');` lists them |
| `Warning: Function created with compilation errors.` when you run the script | The PL/SQL has a mistake. The program is stored but invalid | Type `SHOW ERRORS`. A line such as `PLS-00201: identifier 'V_COUNT' must be declared` names the problem |
| `ORA-01031: insufficient privileges` when you run the script | The user may not create programs | Option A: ask your database team to grant `CREATE PROCEDURE` |
| `Function created with compilation errors.` for a program that is correct, and the program shows `INVALID` in `USER_OBJECTS` | A `/` line is missing after it, so the next `CREATE` is read as part of the same program | Put `/` alone on a line after each `END ...;` |

---

## Practice

1. Call `GET /api/v1/tickets/overdue/count?priority=MEDIUM`. Create a `MEDIUM` ticket, move its `DUE_BY` back, and call again.
2. Change `ESCALATE_OVERDUE` so that it raises `LOW` to `MEDIUM`, and `MEDIUM` to `HIGH`, in one `UPDATE` with a `CASE`. Run the file again and call the endpoint twice.
3. Make the procedure fail on purpose: in `ESCALATE_OVERDUE`, add `RAISE_APPLICATION_ERROR(-20001, 'test');` after the `UPDATE`. Call the endpoint, then count the overdue tickets. Say why the tickets were not changed.

---

## Recap

- A PL/SQL **function** returns a value, and a **procedure** does work and can fill `OUT` parameters.
- `SimpleJdbcCall` calls both by name. A function is read with `executeFunction`, and a procedure with `execute`.
- Oracle's `NUMBER` comes to Java as a `BigDecimal`. Read it as a `Number`.
- Put a call that changes data in a `@Transactional` service method. The PL/SQL does not commit, and Spring does.

### State at end of this part

| File | What it holds |
|---|---|
| `db/oracle/plsql.sql` | `OVERDUE_COUNT` and `ESCALATE_OVERDUE`, run by hand |
| `repository/OverdueRepository.java` | Two `SimpleJdbcCall` objects |
| `service/TicketService.java` | `countOverdue` and `escalateOverdue` |
| `api/TicketController.java` | The two endpoints |

| Request | Result |
|---|---|
| `GET /api/v1/tickets/overdue/count?priority=LOW` | A number |
| `POST /api/v1/tickets/escalate-overdue` | The number of tickets that were raised to `HIGH` |
