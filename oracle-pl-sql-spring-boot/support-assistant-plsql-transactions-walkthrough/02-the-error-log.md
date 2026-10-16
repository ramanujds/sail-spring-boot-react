# Step 2 — The error log

> [Back to index](README.md) · Previous: [Copy the project](01-copy-the-project.md) · Next: [Call the log procedure](03-call-the-log-procedure.md)

## Goal

A table `SUPPORT_ERROR_LOG`, and a package `SUPPORT_ERROR_PKG` with one procedure, `log_error`, that adds a line to it.

## Why this matters

When a bulk change is refused, the tickets go back as they were. Nothing in the database shows that the request was tried. An error log keeps one line for each failure, so that an agent can see what went wrong and when.

The new objects live in their own script and their own package. `SUPPORT_TICKET_PKG` does not change, so the other projects in this folder keep working in the same schema.

### 2.1 The script

Create `db/oracle/03-error-log.sql`. The full file is in the checkpoint.

```sql
CREATE TABLE SUPPORT_ERROR_LOG (
    MESSAGE    VARCHAR2(500)  NOT NULL,
    LOGGED_AT  TIMESTAMP      DEFAULT LOCALTIMESTAMP NOT NULL
);
```

```sql
PROCEDURE log_error (p_message IN VARCHAR2) IS
BEGIN
    INSERT INTO SUPPORT_ERROR_LOG (MESSAGE) VALUES (SUBSTR(p_message, 1, 500));
END log_error;
```

`SUBSTR` cuts a long message to the 500 characters that the column holds. Like the ticket package, `log_error` has **no `COMMIT`**. The caller decides when to save.

### 2.2 The drop script

Add the two new objects to the top of `db/oracle/99-drop.sql`. They go first, before the objects they could depend on.

## Try it

Run `03-error-log.sql` in SQL Developer, as your application user. Expected:

```text
Table SUPPORT_ERROR_LOG created.
Package SUPPORT_ERROR_PKG compiled
Package Body SUPPORT_ERROR_PKG compiled
```

SQL\*Plus prints `Table created.`, `Package created.`, and `Package body created.`

Now call the procedure, undo it, and count:

```sql
EXEC SUPPORT_ERROR_PKG.log_error('sql rollback check')
ROLLBACK;
SELECT COUNT(*) FROM SUPPORT_ERROR_LOG;
```

Expected: `0`. The procedure did not commit, so `ROLLBACK` removed its line. Remember this: in step 4, Spring's rollback removes the line in the same way.

## Checkpoint

`db/oracle/03-error-log.sql` - Matches [../support-assistant-plsql-transactions/db/oracle/03-error-log.sql](../support-assistant-plsql-transactions/db/oracle/03-error-log.sql) exactly.

<details>
<summary>Full <code>db/oracle/03-error-log.sql</code></summary>

```sql
-- Oracle only. Run after 02-package.sql. It does not change SUPPORT_TICKET_PKG.
-- A table for requests that failed, and a small package that writes to it.

CREATE TABLE SUPPORT_ERROR_LOG (
    MESSAGE    VARCHAR2(500)  NOT NULL,
    LOGGED_AT  TIMESTAMP      DEFAULT LOCALTIMESTAMP NOT NULL
);

CREATE OR REPLACE PACKAGE SUPPORT_ERROR_PKG AS

    -- Writes one line. Like SUPPORT_TICKET_PKG, it has no COMMIT. The caller decides when to save.
    PROCEDURE log_error (p_message IN VARCHAR2);

END SUPPORT_ERROR_PKG;
/

CREATE OR REPLACE PACKAGE BODY SUPPORT_ERROR_PKG AS

    PROCEDURE log_error (p_message IN VARCHAR2) IS
    BEGIN
        INSERT INTO SUPPORT_ERROR_LOG (MESSAGE) VALUES (SUBSTR(p_message, 1, 500));
    END log_error;

END SUPPORT_ERROR_PKG;
/
```

</details>

`db/oracle/99-drop.sql` - Matches [../support-assistant-plsql-transactions/db/oracle/99-drop.sql](../support-assistant-plsql-transactions/db/oracle/99-drop.sql) exactly.

<details>
<summary>Full <code>db/oracle/99-drop.sql</code></summary>

```sql
-- Oracle only. Removes everything that 01-tables.sql, 02-package.sql and 03-error-log.sql created,
-- so that you can start again.

DROP PACKAGE SUPPORT_ERROR_PKG;
DROP TABLE SUPPORT_ERROR_LOG;

DROP PACKAGE SUPPORT_TICKET_PKG;
DROP TABLE SUPPORT_TICKET_LOG;
DROP TABLE SUPPORT_TICKETS;
DROP SEQUENCE SUPPORT_TICKETS_SEQ;
```

</details>

## Common mistakes

| Symptom | Cause | Fix |
|---|---|---|
| `ORA-00955: name is already used by an existing object` | The script was run twice. `CREATE TABLE` cannot replace a table | Nothing to fix. The package parts use `CREATE OR REPLACE` and were replaced |
| The count is `1` after `ROLLBACK` | SQL Developer has **Autocommit** turned on | Turn it off in **Tools → Preferences → Database → Advanced** |
| `PLS-00201: identifier 'SUPPORT_ERROR_PKG.LOG_ERROR' must be declared` | The script was run as another user | Run it as the user that your application connects as |

Next: **[Call the log procedure](03-call-the-log-procedure.md)**.
