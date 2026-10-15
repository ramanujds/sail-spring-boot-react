# Step 2 — Tables and the PL/SQL package

> [Back to index](README.md) · Previous: [Concepts and overview](01-concepts-and-overview.md) · Next: [Project and connection](03-project-and-connection.md)

## Goal

Create the tables and the package, and call the package in SQL before you write any Java.

## Why this matters

The rules live here. If you test them in SQL first, a later failure is known to be on the Java side. The names start with `SUPPORT_`, so they do not clash with other tables in your schema.

### 2.1 The tables

`01-tables.sql` creates a sequence and two tables. The main one:

```sql
CREATE TABLE SUPPORT_TICKETS (
    ID           NUMBER         PRIMARY KEY,
    TITLE        VARCHAR2(100)  NOT NULL,
    DESCRIPTION  VARCHAR2(500),
    STATUS       VARCHAR2(20)   NOT NULL,
    PRIORITY     VARCHAR2(10)   NOT NULL,
    CREATED_AT   TIMESTAMP      NOT NULL,
    DUE_BY       TIMESTAMP      NOT NULL
);
```

### 2.2 The package specification

The specification lists the four programs. This is the contract that Java will call:

```sql
FUNCTION open_count (p_priority IN VARCHAR2) RETURN NUMBER;

PROCEDURE create_ticket (
    p_title IN VARCHAR2, p_description IN VARCHAR2,
    p_priority IN VARCHAR2, p_ticket_id OUT NUMBER);

PROCEDURE change_status (p_ticket_id IN NUMBER, p_new_status IN VARCHAR2);

PROCEDURE find_tickets (p_status IN VARCHAR2, p_tickets OUT SYS_REFCURSOR);
```

### 2.3 The package body

| Program | What it does |
|---|---|
| `open_count` | Counts the `OPEN` and `IN_PROGRESS` tickets of a priority |
| `create_ticket` | Checks the values, sets the due date (1, 3 or 7 days), inserts, gives back the id |
| `change_status` | Allows one step only: `OPEN`, `IN_PROGRESS`, `RESOLVED`, `CLOSED` |
| `find_tickets` | Opens a cursor on one status, or on all tickets when the status is `NULL` |

The errors use `RAISE_APPLICATION_ERROR`:

| Number | Meaning |
|---|---|
| `-20001` | The title is missing |
| `-20002` | The priority is not `HIGH`, `MEDIUM` or `LOW` |
| `-20003` | The ticket does not exist |
| `-20004` | The status change is not allowed |

No program has a `COMMIT`. The caller decides.

### 2.4 Run the scripts

Type or copy the files from the checkpoint below. Run `01-tables.sql`, then `02-package.sql`, as the user that your application will connect as. In SQL Developer, use **F5**. With SQL\*Plus:

```bash
sqlplus YOUR_USERNAME/YOUR_PASSWORD@//ORACLE_HOST:1521/ORACLE_SERVICE @db/oracle/01-tables.sql
sqlplus YOUR_USERNAME/YOUR_PASSWORD@//ORACLE_HOST:1521/ORACLE_SERVICE @db/oracle/02-package.sql
```

`99-drop.sql` removes everything, so that you can start again.

## Try it

```sql
SELECT SUPPORT_TICKET_PKG.open_count('HIGH') FROM DUAL;
EXEC SUPPORT_TICKET_PKG.change_status(1, 'IN_PROGRESS')
```

Expected:

```text
SUPPORT_TICKET_PKG.OPEN_COUNT('HIGH')
-------------------------------------
                                    0

ORA-20003: Ticket 1 does not exist
```

The first call returns `0`. The second call is refused by the package, because there is no ticket yet. You will see the same text from Java in step 8.

## Checkpoint

`db/oracle/01-tables.sql` - Matches [../support-assistant-plsql/db/oracle/01-tables.sql](../support-assistant-plsql/db/oracle/01-tables.sql) exactly.

<details>
<summary>Full <code>db/oracle/01-tables.sql</code></summary>

```sql
-- Oracle only. Run once, as the user that your application connects as.
-- The names start with SUPPORT_ so they do not clash with the TICKETS table of Day 5.

CREATE SEQUENCE SUPPORT_TICKETS_SEQ START WITH 1 INCREMENT BY 1;

CREATE TABLE SUPPORT_TICKETS (
    ID           NUMBER         PRIMARY KEY,
    TITLE        VARCHAR2(100)  NOT NULL,
    DESCRIPTION  VARCHAR2(500),
    STATUS       VARCHAR2(20)   NOT NULL,
    PRIORITY     VARCHAR2(10)   NOT NULL,
    CREATED_AT   TIMESTAMP      NOT NULL,
    DUE_BY       TIMESTAMP      NOT NULL
);

CREATE TABLE SUPPORT_TICKET_LOG (
    TICKET_ID  NUMBER         NOT NULL REFERENCES SUPPORT_TICKETS (ID),
    ACTION     VARCHAR2(50)   NOT NULL,
    LOGGED_AT  TIMESTAMP      DEFAULT LOCALTIMESTAMP NOT NULL
);
```

</details>

`db/oracle/02-package.sql` - Matches [../support-assistant-plsql/db/oracle/02-package.sql](../support-assistant-plsql/db/oracle/02-package.sql) exactly.

<details>
<summary>Full <code>db/oracle/02-package.sql</code></summary>

```sql
-- Oracle only. Run after 01-tables.sql. You can run it again: CREATE OR REPLACE replaces the package.
-- No program in this package has a COMMIT. The caller decides when to save.

CREATE OR REPLACE PACKAGE SUPPORT_TICKET_PKG AS

    -- Error numbers that this package raises with RAISE_APPLICATION_ERROR.
    -- -20001  The title is missing
    -- -20002  The priority is not HIGH, MEDIUM or LOW
    -- -20003  The ticket does not exist
    -- -20004  The status change is not allowed

    -- Function: how many tickets of this priority are not finished.
    FUNCTION open_count (p_priority IN VARCHAR2) RETURN NUMBER;

    -- Procedure with an OUT parameter: creates a ticket and gives back its id.
    PROCEDURE create_ticket (
        p_title       IN  VARCHAR2,
        p_description IN  VARCHAR2,
        p_priority    IN  VARCHAR2,
        p_ticket_id   OUT NUMBER);

    -- Procedure: moves a ticket to its next status.
    PROCEDURE change_status (
        p_ticket_id   IN  NUMBER,
        p_new_status  IN  VARCHAR2);

    -- Procedure with a result set: the tickets with this status, or all tickets when p_status is NULL.
    PROCEDURE find_tickets (
        p_status      IN  VARCHAR2,
        p_tickets     OUT SYS_REFCURSOR);

END SUPPORT_TICKET_PKG;
/

CREATE OR REPLACE PACKAGE BODY SUPPORT_TICKET_PKG AS

    FUNCTION open_count (p_priority IN VARCHAR2) RETURN NUMBER IS
        v_count NUMBER;
    BEGIN
        SELECT COUNT(*)
          INTO v_count
          FROM SUPPORT_TICKETS
         WHERE PRIORITY = p_priority
           AND STATUS IN ('OPEN', 'IN_PROGRESS');
        RETURN v_count;
    END open_count;

    PROCEDURE create_ticket (
        p_title       IN  VARCHAR2,
        p_description IN  VARCHAR2,
        p_priority    IN  VARCHAR2,
        p_ticket_id   OUT NUMBER) IS
        v_days NUMBER;
    BEGIN
        IF p_title IS NULL THEN
            RAISE_APPLICATION_ERROR(-20001, 'Title is required');
        END IF;

        -- The service level rule: HIGH is due in 1 day, MEDIUM in 3 days, LOW in 7 days.
        v_days := CASE p_priority WHEN 'HIGH' THEN 1 WHEN 'MEDIUM' THEN 3 WHEN 'LOW' THEN 7 END;
        IF v_days IS NULL THEN
            RAISE_APPLICATION_ERROR(-20002, 'Priority must be HIGH, MEDIUM or LOW');
        END IF;

        p_ticket_id := SUPPORT_TICKETS_SEQ.NEXTVAL;

        INSERT INTO SUPPORT_TICKETS (ID, TITLE, DESCRIPTION, STATUS, PRIORITY, CREATED_AT, DUE_BY)
        VALUES (p_ticket_id, p_title, p_description, 'OPEN', p_priority,
                LOCALTIMESTAMP, LOCALTIMESTAMP + NUMTODSINTERVAL(v_days, 'DAY'));

        INSERT INTO SUPPORT_TICKET_LOG (TICKET_ID, ACTION) VALUES (p_ticket_id, 'CREATED');
    END create_ticket;

    PROCEDURE change_status (
        p_ticket_id   IN  NUMBER,
        p_new_status  IN  VARCHAR2) IS
        v_status SUPPORT_TICKETS.STATUS%TYPE;
        v_next   SUPPORT_TICKETS.STATUS%TYPE;
    BEGIN
        BEGIN
            SELECT STATUS INTO v_status FROM SUPPORT_TICKETS WHERE ID = p_ticket_id FOR UPDATE;
        EXCEPTION
            WHEN NO_DATA_FOUND THEN
                RAISE_APPLICATION_ERROR(-20003, 'Ticket ' || p_ticket_id || ' does not exist');
        END;

        -- The life of a ticket: OPEN, IN_PROGRESS, RESOLVED, CLOSED. One step at a time.
        v_next := CASE v_status
                      WHEN 'OPEN'        THEN 'IN_PROGRESS'
                      WHEN 'IN_PROGRESS' THEN 'RESOLVED'
                      WHEN 'RESOLVED'    THEN 'CLOSED'
                  END;

        IF v_next IS NULL THEN
            RAISE_APPLICATION_ERROR(-20004, 'Ticket ' || p_ticket_id || ' is CLOSED and cannot change');
        END IF;
        IF p_new_status IS NULL OR p_new_status <> v_next THEN
            RAISE_APPLICATION_ERROR(-20004, 'Ticket ' || p_ticket_id || ' is ' || v_status
                                            || '. The next status is ' || v_next);
        END IF;

        UPDATE SUPPORT_TICKETS SET STATUS = p_new_status WHERE ID = p_ticket_id;
        INSERT INTO SUPPORT_TICKET_LOG (TICKET_ID, ACTION) VALUES (p_ticket_id, 'STATUS ' || p_new_status);
    END change_status;

    PROCEDURE find_tickets (
        p_status      IN  VARCHAR2,
        p_tickets     OUT SYS_REFCURSOR) IS
    BEGIN
        OPEN p_tickets FOR
            SELECT ID, TITLE, DESCRIPTION, STATUS, PRIORITY, CREATED_AT, DUE_BY
              FROM SUPPORT_TICKETS
             WHERE p_status IS NULL OR STATUS = p_status
             ORDER BY ID;
    END find_tickets;

END SUPPORT_TICKET_PKG;
/
```

</details>

`db/oracle/99-drop.sql` - Matches [../support-assistant-plsql/db/oracle/99-drop.sql](../support-assistant-plsql/db/oracle/99-drop.sql) exactly.

<details>
<summary>Full <code>db/oracle/99-drop.sql</code></summary>

```sql
-- Oracle only. Removes everything that 01-tables.sql and 02-package.sql created, so that you can start again.

DROP PACKAGE SUPPORT_TICKET_PKG;
DROP TABLE SUPPORT_TICKET_LOG;
DROP TABLE SUPPORT_TICKETS;
DROP SEQUENCE SUPPORT_TICKETS_SEQ;
```

</details>

## Common mistakes

| Symptom | Cause | Fix |
|---|---|---|
| `Warning: Package Body created with compilation errors.` | The body has a mistake. The package is stored but invalid | Type `SHOW ERRORS PACKAGE BODY SUPPORT_TICKET_PKG` and read the line |
| `ORA-00955: name is already used by an existing object` | `01-tables.sql` was run twice | Run `99-drop.sql`, then both scripts again |
| A script creates nothing and shows `/` on its own | A `/` line is missing after `END ...;` | Put `/` alone on a line after each program |

Next: **[Project and connection](03-project-and-connection.md)**.
