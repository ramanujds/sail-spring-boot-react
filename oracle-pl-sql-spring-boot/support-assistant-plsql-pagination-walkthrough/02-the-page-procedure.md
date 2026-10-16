# Step 2 — The page procedure

> [Back to index](README.md) · Previous: [Copy the project](01-copy-the-project.md) · Next: [PageResponse and the repository](03-page-response-and-repository.md)

## Goal

A package `SUPPORT_TICKET_PAGE_PKG` with one procedure, `find_page`, tried in SQL Developer. Also 12 sample tickets, so that there is more than one page.

## Why this matters

`FIND_TICKETS` opens a cursor over every ticket. Java then receives every row, even if the screen shows five. `find_page` asks Oracle for only the rows of one page, and counts all the matching rows in the same call. The caller needs that count to know how many pages there are.

The new procedure lives in its own package. `SUPPORT_TICKET_PKG` does not change, so the other projects in this folder keep working in the same schema.

### 2.1 How Oracle reads one page

Oracle 12c and later can skip rows and take a fixed number, after sorting. The picture shows page `1` with `size=5`.

```mermaid
flowchart LR
    A["SUPPORT_TICKETS"] --> B["WHERE: keep the rows asked for"]
    B --> C["ORDER BY: sort them"]
    C --> D["OFFSET 5 ROWS: skip page 0"]
    D --> E["FETCH NEXT 5 ROWS: keep page 1"]
```

**How to read it:** the rows are filtered and sorted first. Only then does Oracle skip and take. The number to skip is `page × size`.

### 2.2 The specification

Create `db/oracle/03-page-package.sql`. The specification has two `OUT` parameters:

```sql
PROCEDURE find_page (
    p_status     IN  VARCHAR2,
    p_page       IN  NUMBER,
    p_size       IN  NUMBER,
    p_sort       IN  VARCHAR2,
    p_direction  IN  VARCHAR2,
    p_tickets    OUT SYS_REFCURSOR,
    p_total      OUT NUMBER);
```

### 2.3 The body

First the total, with the same `WHERE` as the page:

```sql
SELECT COUNT(*)
  INTO p_total
  FROM SUPPORT_TICKETS
 WHERE p_status IS NULL OR STATUS = p_status;
```

Then the page:

```sql
OPEN p_tickets FOR
    SELECT ID, TITLE, DESCRIPTION, STATUS, PRIORITY, CREATED_AT, DUE_BY
      FROM SUPPORT_TICKETS
     WHERE p_status IS NULL OR STATUS = p_status
     ORDER BY
           CASE WHEN p_sort = 'DUE_BY'     AND p_direction = 'ASC'  THEN DUE_BY     END ASC,
           CASE WHEN p_sort = 'DUE_BY'     AND p_direction = 'DESC' THEN DUE_BY     END DESC,
           CASE WHEN p_sort = 'CREATED_AT' AND p_direction = 'ASC'  THEN CREATED_AT END ASC,
           CASE WHEN p_sort = 'CREATED_AT' AND p_direction = 'DESC' THEN CREATED_AT END DESC,
           ID
    OFFSET p_page * p_size ROWS FETCH NEXT p_size ROWS ONLY;
```

| Part | Why it is there |
|---|---|
| Four `CASE` lines | Only the line that matches `p_sort` and `p_direction` has a value. The others are `NULL` for every row, so they do not change the order |
| No SQL built from text | The sort comes from a fixed list. A value such as `DUE_BY; DROP TABLE ...` matches no line and changes nothing |
| `ID` last | Two tickets can have the same due date. `ID` is different in every row, so two calls always give the same order, and no ticket appears on two pages |

The full script is in the checkpoint.

### 2.4 The sample tickets and the drop script

Create `db/oracle/04-sample-tickets.sql` (in the checkpoint). It calls `SUPPORT_TICKET_PKG.create_ticket` 12 times, with priorities that take turns (`MEDIUM`, `LOW`, `HIGH`, ...), and ends with `COMMIT`.

Add `DROP PACKAGE SUPPORT_TICKET_PAGE_PKG;` to the top of `db/oracle/99-drop.sql`.

## Try it

Run `03-page-package.sql`, then `04-sample-tickets.sql`, in SQL Developer as your application user.

Read page `1` with `size=5`, sorted by due date. Run this as a script (F5):

```sql
VARIABLE tickets REFCURSOR
VARIABLE total NUMBER
EXEC SUPPORT_TICKET_PAGE_PKG.find_page(NULL, 1, 5, 'DUE_BY', 'ASC', :tickets, :total)
PRINT total
PRINT tickets
```

Expected, with only the 12 sample tickets in the table: `total` is `12`, and the page holds five rows:

| ID | TITLE | PRIORITY |
|---|---|---|
| 4 | Sample ticket 4 | MEDIUM |
| 7 | Sample ticket 7 | MEDIUM |
| 10 | Sample ticket 10 | MEDIUM |
| 2 | Sample ticket 2 | LOW |
| 5 | Sample ticket 5 | LOW |

Page `0` held the four `HIGH` tickets (due in 1 day) and `Sample ticket 1`. If your table has other tickets from earlier steps, the ids and the total are different.

## Checkpoint

`db/oracle/03-page-package.sql` - Matches [../support-assistant-plsql-pagination/db/oracle/03-page-package.sql](../support-assistant-plsql-pagination/db/oracle/03-page-package.sql) exactly.

<details>
<summary>Full <code>db/oracle/03-page-package.sql</code></summary>

```sql
-- Oracle only. Run after 02-package.sql. It does not change SUPPORT_TICKET_PKG.
-- OFFSET ... FETCH NEXT needs Oracle 12c or later.

CREATE OR REPLACE PACKAGE SUPPORT_TICKET_PAGE_PKG AS

    -- One page of tickets, and the number of tickets in all pages.
    -- p_status     NULL for every status
    -- p_page       the page number, from 0
    -- p_size       how many tickets in a page
    -- p_sort       'DUE_BY' or 'CREATED_AT'. Any other value orders by ID only
    -- p_direction  'ASC' or 'DESC'
    PROCEDURE find_page (
        p_status     IN  VARCHAR2,
        p_page       IN  NUMBER,
        p_size       IN  NUMBER,
        p_sort       IN  VARCHAR2,
        p_direction  IN  VARCHAR2,
        p_tickets    OUT SYS_REFCURSOR,
        p_total      OUT NUMBER);

END SUPPORT_TICKET_PAGE_PKG;
/

CREATE OR REPLACE PACKAGE BODY SUPPORT_TICKET_PAGE_PKG AS

    PROCEDURE find_page (
        p_status     IN  VARCHAR2,
        p_page       IN  NUMBER,
        p_size       IN  NUMBER,
        p_sort       IN  VARCHAR2,
        p_direction  IN  VARCHAR2,
        p_tickets    OUT SYS_REFCURSOR,
        p_total      OUT NUMBER) IS
    BEGIN
        -- The total, with the same WHERE as the page.
        SELECT COUNT(*)
          INTO p_total
          FROM SUPPORT_TICKETS
         WHERE p_status IS NULL OR STATUS = p_status;

        -- The page. Only one of the four CASE lines has a value. The others are NULL for every row,
        -- so they do not change the order. ID comes last, so two calls always give the same order.
        OPEN p_tickets FOR
            SELECT ID, TITLE, DESCRIPTION, STATUS, PRIORITY, CREATED_AT, DUE_BY
              FROM SUPPORT_TICKETS
             WHERE p_status IS NULL OR STATUS = p_status
             ORDER BY
                   CASE WHEN p_sort = 'DUE_BY'     AND p_direction = 'ASC'  THEN DUE_BY     END ASC,
                   CASE WHEN p_sort = 'DUE_BY'     AND p_direction = 'DESC' THEN DUE_BY     END DESC,
                   CASE WHEN p_sort = 'CREATED_AT' AND p_direction = 'ASC'  THEN CREATED_AT END ASC,
                   CASE WHEN p_sort = 'CREATED_AT' AND p_direction = 'DESC' THEN CREATED_AT END DESC,
                   ID
            OFFSET p_page * p_size ROWS FETCH NEXT p_size ROWS ONLY;
    END find_page;

END SUPPORT_TICKET_PAGE_PKG;
/
```

</details>

`db/oracle/04-sample-tickets.sql` - Matches [../support-assistant-plsql-pagination/db/oracle/04-sample-tickets.sql](../support-assistant-plsql-pagination/db/oracle/04-sample-tickets.sql) exactly.

<details>
<summary>Full <code>db/oracle/04-sample-tickets.sql</code></summary>

```sql
-- Oracle only. Optional. Creates 12 tickets, so that there is more than one page to read.
-- It calls the same procedure as POST /api/tickets, then saves with COMMIT.

DECLARE
    v_id NUMBER;
BEGIN
    FOR i IN 1 .. 12 LOOP
        SUPPORT_TICKET_PKG.create_ticket(
            p_title       => 'Sample ticket ' || i,
            p_description => NULL,
            p_priority    => CASE MOD(i, 3) WHEN 0 THEN 'HIGH' WHEN 1 THEN 'MEDIUM' ELSE 'LOW' END,
            p_ticket_id   => v_id);
    END LOOP;
    COMMIT;
END;
/
```

</details>

`db/oracle/99-drop.sql` - Matches [../support-assistant-plsql-pagination/db/oracle/99-drop.sql](../support-assistant-plsql-pagination/db/oracle/99-drop.sql) exactly.

<details>
<summary>Full <code>db/oracle/99-drop.sql</code></summary>

```sql
-- Oracle only. Removes everything that 01-tables.sql, 02-package.sql and 03-page-package.sql created,
-- so that you can start again.

DROP PACKAGE SUPPORT_TICKET_PAGE_PKG;

DROP PACKAGE SUPPORT_TICKET_PKG;
DROP TABLE SUPPORT_TICKET_LOG;
DROP TABLE SUPPORT_TICKETS;
DROP SEQUENCE SUPPORT_TICKETS_SEQ;
```

</details>

## Common mistakes

| Symptom | Cause | Fix |
|---|---|---|
| `ORA-00933: SQL command not properly ended` when the body is created | The database is older than Oracle 12c | Use Oracle 12c or later. (Not tried: no older database was available) |
| The page comes back in id order: tickets 1, 2, 3, 4, 5 | `p_sort` or `p_direction` was written in lower case, such as `'due_by'`. It matches no `CASE` line, so only `ID` orders the rows | Pass upper case. In step 4, Java does this for you |

Next: **[PageResponse and the repository](03-page-response-and-repository.md)**.
