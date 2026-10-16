# Step 3 — The package

> [Back to index](README.md) · Previous: [Tables and the relationship](02-tables-and-relationship.md) · Next: [Project and connection](04-project-and-connection.md)

## Goal

Create `DESK_TICKET_PKG`, and try the programs in SQL before you write any Java.

## Why this matters

The rules between a parent and its children live here: a comment needs a real ticket, and a closed ticket takes no comments. If you test them in SQL first, a later failure is known to be on the Java side. The package also decides what a "read" returns, so the shape of every Java record in the next steps comes from the cursors you write now.

### 3.1 The programs

| Program | Kind | What it does |
|---|---|---|
| `create_ticket` | Procedure, `OUT` id | Checks the values, sets the due date (1, 3 or 7 days), inserts, gives back the id |
| `change_status` | Procedure | Allows one step only: `OPEN`, `IN_PROGRESS`, `RESOLVED`, `CLOSED` |
| `find_tickets` | Procedure, one cursor | The tickets with a status, or all tickets when the status is `NULL`. No comments |
| `add_comment` | Procedure, `OUT` id | **New.** Adds a comment to a ticket and gives back the comment id |
| `get_ticket` | Procedure, **two cursors** | **New.** One ticket, and its comments |
| `find_tickets_with_comments` | Procedure, **one joined cursor** | **New.** Many tickets, each row carrying one comment |

Three programs are the same as in the first project. They are in the checkpoint, and you can type them from there. The next parts explain the three new ones, and the one private function that they share.

The error numbers:

| Number | Meaning | New? |
|---|---|---|
| `-20001` | The title is missing | |
| `-20002` | The priority is not `HIGH`, `MEDIUM` or `LOW` | |
| `-20003` | The ticket does not exist | |
| `-20004` | The status change is not allowed | |
| `-20005` | The author or the text of a comment is missing | yes |
| `-20006` | The ticket is `CLOSED`, so it takes no more comments | yes |

### 3.2 A private function: look at the parent, and lock it

Both `change_status` and `add_comment` must find the ticket first. Write that once, in a function that is in the package body but not in the specification. Java cannot call it:

```sql
FUNCTION lock_ticket (p_ticket_id IN NUMBER) RETURN VARCHAR2 IS
    v_status DESK_TICKETS.STATUS%TYPE;
BEGIN
    SELECT STATUS INTO v_status FROM DESK_TICKETS WHERE ID = p_ticket_id FOR UPDATE;
    RETURN v_status;
EXCEPTION
    WHEN NO_DATA_FOUND THEN
        RAISE_APPLICATION_ERROR(-20003, 'Ticket ' || p_ticket_id || ' does not exist');
END lock_ticket;
```

`FOR UPDATE` locks the ticket row until the caller saves or undoes its work. Nobody can close the ticket while a comment is being added. A private function must come **before** the programs that use it.

### 3.3 `add_comment`: the parent decides what the child may do

```sql
IF p_author IS NULL OR p_body IS NULL THEN
    RAISE_APPLICATION_ERROR(-20005, 'Author and text are required');
END IF;

v_status := lock_ticket(p_ticket_id);
IF v_status = 'CLOSED' THEN
    RAISE_APPLICATION_ERROR(-20006, 'Ticket ' || p_ticket_id || ' is CLOSED and takes no comments');
END IF;

p_comment_id := DESK_COMMENTS_SEQ.NEXTVAL;

INSERT INTO DESK_COMMENTS (ID, TICKET_ID, AUTHOR, BODY, CREATED_AT)
VALUES (p_comment_id, p_ticket_id, p_author, p_body, LOCALTIMESTAMP);
```

The order matters: check the cheap things first, then look at the parent, then insert. The foreign key would also refuse a missing ticket, but with `ORA-02291`. The check above gives a message that you chose.

### 3.4 `get_ticket`: two cursors in one call

A procedure can have more than one `SYS_REFCURSOR` parameter. One cursor holds the parent row, and the other holds the child rows:

```sql
OPEN p_ticket FOR
    SELECT ID, TITLE, DESCRIPTION, STATUS, PRIORITY, CREATED_AT, DUE_BY
      FROM DESK_TICKETS WHERE ID = p_ticket_id;

OPEN p_comments FOR
    SELECT ID, AUTHOR, BODY, CREATED_AT
      FROM DESK_COMMENTS WHERE TICKET_ID = p_ticket_id ORDER BY ID;
```

A cursor that finds no row is not an error, so the program counts the ticket first and raises `-20003` when it is missing. Without this check, a missing ticket would look like a ticket with no data.

### 3.5 `find_tickets_with_comments`: one joined cursor

For many tickets, a cursor for each ticket would mean many calls (step 8 shows why that is slow). Join the two tables in one query instead:

```sql
SELECT t.ID, t.TITLE, t.DESCRIPTION, t.STATUS, t.PRIORITY, t.CREATED_AT, t.DUE_BY,
       c.ID AS COMMENT_ID, c.AUTHOR, c.BODY, c.CREATED_AT AS COMMENT_AT
  FROM DESK_TICKETS t
  LEFT JOIN DESK_COMMENTS c ON c.TICKET_ID = t.ID
 WHERE p_status IS NULL OR t.STATUS = p_status
 ORDER BY t.ID, c.ID;
```

Two things to notice:

- A ticket with 3 comments gives 3 rows. The ticket columns repeat in each row.
- `LEFT JOIN` keeps a ticket that has **no** comment. Its comment columns are `NULL`. A plain `JOIN` would drop that ticket from the result.

The columns `COMMENT_ID` and `COMMENT_AT` have new names because both tables have columns called `ID` and `CREATED_AT`. A cursor needs a different name for each column.

### 3.6 Run the scripts

Type or copy the file from the checkpoint below. Run `02-package.sql` as the user that your application will connect as. With SQL\*Plus:

```bash
sqlplus YOUR_USERNAME/YOUR_PASSWORD@//ORACLE_HOST:1521/ORACLE_SERVICE @db/oracle/02-package.sql
```

**Check:** you should see `Package created.` and `Package body created.` If you see `Warning: Package Body created with compilation errors.`, type `SHOW ERRORS PACKAGE BODY DESK_TICKET_PKG` and read the line.

## Try it

Run these statements in SQL Developer or SQL\*Plus. The cursors print more clearly after these settings:

```sql
SET LINESIZE 120
COLUMN TITLE FORMAT A20
COLUMN BODY FORMAT A25
COLUMN AUTHOR FORMAT A8
COLUMN STATUS FORMAT A8
COLUMN DESCRIPTION NOPRINT
COLUMN PRIORITY NOPRINT
COLUMN CREATED_AT NOPRINT
COLUMN DUE_BY NOPRINT
COLUMN COMMENT_AT NOPRINT
```

Create two tickets and two comments, then read ticket 1 with its two cursors:

```sql
VARIABLE id NUMBER
VARIABLE cid NUMBER
EXEC DESK_TICKET_PKG.create_ticket('Printer is jammed', 'Second floor', 'HIGH', :id)
EXEC DESK_TICKET_PKG.add_comment(:id, 'Meera', 'The paper tray is stuck', :cid)
EXEC DESK_TICKET_PKG.add_comment(:id, 'Karan', 'On my way', :cid)
EXEC DESK_TICKET_PKG.create_ticket('VPN drops every hour', NULL, 'MEDIUM', :id)

VARIABLE t REFCURSOR
VARIABLE c REFCURSOR
EXEC DESK_TICKET_PKG.get_ticket(1, :t, :c)
PRINT t
PRINT c
```

Expected:

```text
        ID TITLE                STATUS
---------- -------------------- --------
         1 Printer is jammed    OPEN

        ID AUTHOR   BODY
---------- -------- -------------------------
         1 Meera    The paper tray is stuck
         2 Karan    On my way
```

Now read all tickets with one joined cursor:

```sql
VARIABLE r REFCURSOR
EXEC DESK_TICKET_PKG.find_tickets_with_comments(NULL, :r)
PRINT r
```

Expected. Ticket 1 appears twice, and ticket 2 appears once, with empty comment columns:

```text
        ID TITLE                STATUS   COMMENT_ID AUTHOR   BODY
---------- -------------------- -------- ---------- -------- -------------------------
         1 Printer is jammed    OPEN              1 Meera    The paper tray is stuck
         1 Printer is jammed    OPEN              2 Karan    On my way
         2 VPN drops every hour OPEN
```

Break the rules. Each statement is refused:

```sql
EXEC DESK_TICKET_PKG.add_comment(999, 'Meera', 'Hello', :cid)
EXEC DESK_TICKET_PKG.add_comment(1, 'Meera', NULL, :cid)
DELETE FROM DESK_TICKETS WHERE ID = 1;
```

Expected:

```text
ORA-20003: Ticket 999 does not exist
ORA-20005: Author and text are required
ORA-02292: integrity constraint (YOUR_USERNAME.SYS_C008348) violated - child record found
```

The foreign key protects the parent as well: a ticket that has comments cannot be deleted. Each message is followed by `ORA-06512` lines that say where it happened.

### Start again from clean tables

The sessions above used the id `1` for a ticket and ids `1` and `2` for comments. An Oracle sequence does not go back. To make the ids in the next steps match this walkthrough, remove everything and create it again. This also tries `99-drop.sql`:

```bash
sqlplus YOUR_USERNAME/YOUR_PASSWORD@//ORACLE_HOST:1521/ORACLE_SERVICE @db/oracle/99-drop.sql
sqlplus YOUR_USERNAME/YOUR_PASSWORD@//ORACLE_HOST:1521/ORACLE_SERVICE @db/oracle/01-tables.sql
sqlplus YOUR_USERNAME/YOUR_PASSWORD@//ORACLE_HOST:1521/ORACLE_SERVICE @db/oracle/02-package.sql
```

`99-drop.sql` removes the child table before the parent table. Oracle refuses to drop a parent while a child points to it.

## Checkpoint

`db/oracle/02-package.sql` - Matches [../support-assistant-plsql-relations/db/oracle/02-package.sql](../support-assistant-plsql-relations/db/oracle/02-package.sql) exactly.

<details>
<summary>Full <code>db/oracle/02-package.sql</code></summary>

```sql
-- Oracle only. Run after 01-tables.sql. You can run it again: CREATE OR REPLACE replaces the package.
-- No program in this package has a COMMIT. The caller decides when to save.

CREATE OR REPLACE PACKAGE DESK_TICKET_PKG AS

    -- Error numbers that this package raises with RAISE_APPLICATION_ERROR.
    -- -20001  The title is missing
    -- -20002  The priority is not HIGH, MEDIUM or LOW
    -- -20003  The ticket does not exist
    -- -20004  The status change is not allowed
    -- -20005  The author or the text of a comment is missing
    -- -20006  The ticket is CLOSED, so it takes no more comments

    -- Creates a ticket and gives back its id.
    PROCEDURE create_ticket (
        p_title       IN  VARCHAR2,
        p_description IN  VARCHAR2,
        p_priority    IN  VARCHAR2,
        p_ticket_id   OUT NUMBER);

    -- Moves a ticket to its next status.
    PROCEDURE change_status (
        p_ticket_id   IN  NUMBER,
        p_new_status  IN  VARCHAR2);

    -- The tickets with this status, or all tickets when p_status is NULL. No comments.
    PROCEDURE find_tickets (
        p_status      IN  VARCHAR2,
        p_tickets     OUT SYS_REFCURSOR);

    -- Adds a comment to a ticket and gives back the id of the comment.
    PROCEDURE add_comment (
        p_ticket_id   IN  NUMBER,
        p_author      IN  VARCHAR2,
        p_body        IN  VARCHAR2,
        p_comment_id  OUT NUMBER);

    -- One ticket and its comments. Two cursors: one row of the parent, and the rows of the child.
    PROCEDURE get_ticket (
        p_ticket_id   IN  NUMBER,
        p_ticket      OUT SYS_REFCURSOR,
        p_comments    OUT SYS_REFCURSOR);

    -- Many tickets and their comments in one cursor. A ticket with 3 comments has 3 rows.
    -- A ticket with no comments has 1 row, and the comment columns are NULL.
    PROCEDURE find_tickets_with_comments (
        p_status      IN  VARCHAR2,
        p_rows        OUT SYS_REFCURSOR);

END DESK_TICKET_PKG;
/

CREATE OR REPLACE PACKAGE BODY DESK_TICKET_PKG AS

    -- Private: it is not in the specification, so Java cannot call it. It must come before the
    -- programs that use it. It reads the status of a ticket and locks the row, so that nobody
    -- else changes the ticket until the caller saves or undoes its work.
    FUNCTION lock_ticket (p_ticket_id IN NUMBER) RETURN VARCHAR2 IS
        v_status DESK_TICKETS.STATUS%TYPE;
    BEGIN
        SELECT STATUS INTO v_status FROM DESK_TICKETS WHERE ID = p_ticket_id FOR UPDATE;
        RETURN v_status;
    EXCEPTION
        WHEN NO_DATA_FOUND THEN
            RAISE_APPLICATION_ERROR(-20003, 'Ticket ' || p_ticket_id || ' does not exist');
    END lock_ticket;

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

        p_ticket_id := DESK_TICKETS_SEQ.NEXTVAL;

        INSERT INTO DESK_TICKETS (ID, TITLE, DESCRIPTION, STATUS, PRIORITY, CREATED_AT, DUE_BY)
        VALUES (p_ticket_id, p_title, p_description, 'OPEN', p_priority,
                LOCALTIMESTAMP, LOCALTIMESTAMP + NUMTODSINTERVAL(v_days, 'DAY'));
    END create_ticket;

    PROCEDURE change_status (
        p_ticket_id   IN  NUMBER,
        p_new_status  IN  VARCHAR2) IS
        v_status DESK_TICKETS.STATUS%TYPE;
        v_next   DESK_TICKETS.STATUS%TYPE;
    BEGIN
        v_status := lock_ticket(p_ticket_id);

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

        UPDATE DESK_TICKETS SET STATUS = p_new_status WHERE ID = p_ticket_id;
    END change_status;

    PROCEDURE find_tickets (
        p_status      IN  VARCHAR2,
        p_tickets     OUT SYS_REFCURSOR) IS
    BEGIN
        OPEN p_tickets FOR
            SELECT ID, TITLE, DESCRIPTION, STATUS, PRIORITY, CREATED_AT, DUE_BY
              FROM DESK_TICKETS
             WHERE p_status IS NULL OR STATUS = p_status
             ORDER BY ID;
    END find_tickets;

    PROCEDURE add_comment (
        p_ticket_id   IN  NUMBER,
        p_author      IN  VARCHAR2,
        p_body        IN  VARCHAR2,
        p_comment_id  OUT NUMBER) IS
        v_status DESK_TICKETS.STATUS%TYPE;
    BEGIN
        IF p_author IS NULL OR p_body IS NULL THEN
            RAISE_APPLICATION_ERROR(-20005, 'Author and text are required');
        END IF;

        -- The parent decides what the child may do. Locking the ticket also means that
        -- nobody can close it between this check and the INSERT below.
        v_status := lock_ticket(p_ticket_id);
        IF v_status = 'CLOSED' THEN
            RAISE_APPLICATION_ERROR(-20006, 'Ticket ' || p_ticket_id || ' is CLOSED and takes no comments');
        END IF;

        p_comment_id := DESK_COMMENTS_SEQ.NEXTVAL;

        INSERT INTO DESK_COMMENTS (ID, TICKET_ID, AUTHOR, BODY, CREATED_AT)
        VALUES (p_comment_id, p_ticket_id, p_author, p_body, LOCALTIMESTAMP);
    END add_comment;

    PROCEDURE get_ticket (
        p_ticket_id   IN  NUMBER,
        p_ticket      OUT SYS_REFCURSOR,
        p_comments    OUT SYS_REFCURSOR) IS
        v_found NUMBER;
    BEGIN
        -- A cursor that finds no row is not an error. Check first, so that a missing ticket is a 404.
        SELECT COUNT(*) INTO v_found FROM DESK_TICKETS WHERE ID = p_ticket_id;
        IF v_found = 0 THEN
            RAISE_APPLICATION_ERROR(-20003, 'Ticket ' || p_ticket_id || ' does not exist');
        END IF;

        OPEN p_ticket FOR
            SELECT ID, TITLE, DESCRIPTION, STATUS, PRIORITY, CREATED_AT, DUE_BY
              FROM DESK_TICKETS
             WHERE ID = p_ticket_id;

        OPEN p_comments FOR
            SELECT ID, AUTHOR, BODY, CREATED_AT
              FROM DESK_COMMENTS
             WHERE TICKET_ID = p_ticket_id
             ORDER BY ID;
    END get_ticket;

    PROCEDURE find_tickets_with_comments (
        p_status      IN  VARCHAR2,
        p_rows        OUT SYS_REFCURSOR) IS
    BEGIN
        -- LEFT JOIN keeps a ticket that has no comment. A plain JOIN would drop it from the result.
        OPEN p_rows FOR
            SELECT t.ID, t.TITLE, t.DESCRIPTION, t.STATUS, t.PRIORITY, t.CREATED_AT, t.DUE_BY,
                   c.ID AS COMMENT_ID, c.AUTHOR, c.BODY, c.CREATED_AT AS COMMENT_AT
              FROM DESK_TICKETS t
              LEFT JOIN DESK_COMMENTS c ON c.TICKET_ID = t.ID
             WHERE p_status IS NULL OR t.STATUS = p_status
             ORDER BY t.ID, c.ID;
    END find_tickets_with_comments;

END DESK_TICKET_PKG;
/
```

</details>

`db/oracle/99-drop.sql` - Matches [../support-assistant-plsql-relations/db/oracle/99-drop.sql](../support-assistant-plsql-relations/db/oracle/99-drop.sql) exactly.

<details>
<summary>Full <code>db/oracle/99-drop.sql</code></summary>

```sql
-- Oracle only. Removes everything that 01-tables.sql and 02-package.sql created, so that you can start again.
-- The child table goes before the parent table, because DESK_COMMENTS refers to DESK_TICKETS.

DROP PACKAGE DESK_TICKET_PKG;
DROP TABLE DESK_COMMENTS;
DROP TABLE DESK_TICKETS;
DROP SEQUENCE DESK_COMMENTS_SEQ;
DROP SEQUENCE DESK_TICKETS_SEQ;
```

</details>

## Common mistakes

| Symptom | Cause | Fix |
|---|---|---|
| `PLS-00313: 'LOCK_TICKET' not declared in this scope` | The private function is written after the program that calls it | Move `lock_ticket` to the top of the package body |
| `ORA-00918: column ambiguously defined` in a joined cursor | Both tables have a column with the same name (`ID`), and one is not marked with its table | Write `t.ID` and `c.ID`, and give the second a new name with `AS` |
| A ticket without comments is missing from the joined cursor | `JOIN` was written instead of `LEFT JOIN` | Use `LEFT JOIN` |
| `ORA-02449: unique/primary keys in table referenced by foreign keys` when you drop | `DESK_TICKETS` was dropped before `DESK_COMMENTS` | Drop the child first, as `99-drop.sql` does |

Next: **[Project and connection](04-project-and-connection.md)**.
