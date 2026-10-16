# Pagination

`GET /api/tickets` returns every ticket. With 30 tickets that is fine. With 30,000 it is slow, large, and no screen can show it. In this part the list returns one **page** at a time, and the PL/SQL package reads only the rows of that page.

**Time:** about 60 minutes.
**Starting point:** your project at the end of [part 3](03-errors-and-transactions.md), or the finished [support-assistant-plsql](support-assistant-plsql/). This part does not need part 5.
**Project:** [support-assistant-plsql-pagination](support-assistant-plsql-pagination/) in this folder.
**Step by step:** the same build, with the full files after every step, is in [support-assistant-plsql-pagination-walkthrough](support-assistant-plsql-pagination-walkthrough/README.md).

## What you will build

- A procedure `FIND_PAGE` that opens a cursor for one page, and gives back the total as an `OUT` value.
- A `PageResponse` that tells the caller which page it has and how many there are.
- `GET /api/tickets?page=0&size=5&sort=dueBy&direction=asc`, with a clear `400` for a value that is not allowed.

---

## 1. The ideas

### A page is described by four values

| Parameter | Meaning | Example |
|---|---|---|
| `page` | Which page. The first page is `0` | `page=1` |
| `size` | How many tickets in a page | `size=5` |
| `sort` | The field to order by | `sort=dueBy` |
| `direction` | `asc` (smallest first) or `desc` (largest first) | `direction=desc` |

With 12 tickets and `size=5`, there are 3 pages. Page `0` holds tickets 1 to 5 of the sorted list, page `1` holds 6 to 10, and page `2` holds the last 2.

### How Oracle reads one page

Oracle 12c and later can skip rows and take a fixed number, after sorting:

```sql
ORDER BY DUE_BY, ID
OFFSET 5 ROWS FETCH NEXT 5 ROWS ONLY
```

The order of the work matters. The picture shows page `1` with `size=5`.

```mermaid
flowchart LR
    A["SUPPORT_TICKETS"] --> B["WHERE: keep the rows asked for"]
    B --> C["ORDER BY: sort them"]
    C --> D["OFFSET 5 ROWS: skip page 0"]
    D --> E["FETCH NEXT 5 ROWS: keep page 1"]
```

**How to read it:** the rows are filtered and sorted first. Only then does Oracle skip and take. The number to skip is `page × size`.

`ID` comes last in the `ORDER BY`. Two tickets can have the same due date. Without a column that is different in every row, Oracle may order those two differently on each call, and one ticket can appear on two pages.

### One call, two answers

The caller needs the tickets of the page **and** the total, to show "page 2 of 3". `FIND_PAGE` gives both:

| Parameter | Kind | Holds |
|---|---|---|
| `p_tickets` | `OUT SYS_REFCURSOR` | The rows of this page |
| `p_total` | `OUT NUMBER` | How many rows match, in all pages |

`SimpleJdbcCall` puts both in the same `Map`, as in part 2.

### Sort safely

The caller chooses the sort. Do not glue the caller's text into the SQL statement: a value such as `DUE_BY; DROP TABLE SUPPORT_TICKETS` would become part of the SQL. This is called **SQL injection**.

Two guards keep the sort safe:

1. Java accepts only the names it knows (`dueBy`, `createdAt`), and answers `400` for any other.
2. The package does not build SQL from text. It has one `CASE` line for each allowed sort, and only the matching line has a value.

---

## Build steps

### Step 1: copy the project

Start from a copy, so that the project of part 3 stays as it is. In the folder that holds `support-assistant-plsql`:

```bash
cp -r support-assistant-plsql support-assistant-plsql-pagination
cd support-assistant-plsql-pagination
rm -rf target
```

On Windows, copy the folder in Explorer and delete `target` in the copy.

Give the copy its own name:

| File | Line | New value |
|---|---|---|
| `pom.xml` | `<artifactId>` | `support-assistant-plsql-pagination` |
| `pom.xml` | `<name>` | `support-assistant-plsql-pagination` |
| `src/main/resources/application.yaml` | `spring.application.name` | `support-assistant-plsql-pagination` |

Leave the `<artifactId>` of the parent (`spring-boot-starter-parent`) as it is. Open the folder in IntelliJ as a new project.

**Check:** with `DB_USERNAME` and `DB_PASSWORD` set, `mvn clean verify` ends with `Tests run: 4, Failures: 0, Errors: 0` and `BUILD SUCCESS`.

### Step 2: the page procedure

Create `db/oracle/03-page-package.sql`. The specification:

```sql
CREATE OR REPLACE PACKAGE SUPPORT_TICKET_PAGE_PKG AS
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
```

The body of `find_page` counts, then opens the page:

```sql
SELECT COUNT(*) INTO p_total
  FROM SUPPORT_TICKETS
 WHERE p_status IS NULL OR STATUS = p_status;

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

The project file has the whole package, with the `CREATE OR REPLACE PACKAGE BODY` around this code. Only one of the four `CASE` lines has a value. The other three are `NULL` for every row, so they do not change the order. The count and the page use the same `WHERE`, so the total matches the pages.

The new package is separate from `SUPPORT_TICKET_PKG`, so the other projects in this folder keep working. Add `DROP PACKAGE SUPPORT_TICKET_PAGE_PKG;` to the top of `db/oracle/99-drop.sql`.

Run the script in SQL Developer. To have more than one page, also run [04-sample-tickets.sql](support-assistant-plsql-pagination/db/oracle/04-sample-tickets.sql) from the project: it creates 12 tickets with `create_ticket`, and commits.

Try page `1` with `size=5`. Run this as a script (F5) in SQL Developer:

```sql
VARIABLE tickets REFCURSOR
VARIABLE total NUMBER
EXEC SUPPORT_TICKET_PAGE_PKG.find_page(NULL, 1, 5, 'DUE_BY', 'ASC', :tickets, :total)
PRINT total
PRINT tickets
```

**Check:** with only the 12 sample tickets in the table, `total` is `12`, and the page holds 5 rows: `Sample ticket 4`, `7` and `10` (`MEDIUM`), then `Sample ticket 2` and `5` (`LOW`). With other tickets in the table, the numbers are larger.

### Step 3: a record for a page

Create `PageResponse` in the package `dto`:

```java
public record PageResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages) {
}
```

`T` stands for the type of the items: here `TicketResponse`. The same record can later hold a page of anything.

### Step 4: call the procedure

In `TicketRepository`, add a field and build the call in the constructor. It uses the same row mapper as `findTickets`:

```java
this.findPage = new SimpleJdbcCall(jdbcTemplate)
        .withCatalogName("SUPPORT_TICKET_PAGE_PKG").withProcedureName("FIND_PAGE")
        .returningResultSet("P_TICKETS", this::toTicket);
```

Add the method. It reads both `OUT` values from the `Map`:

```java
@SuppressWarnings("unchecked")
public PageResponse<TicketResponse> findPage(String status, int page, int size,
                                             String sortColumn, String direction) {
    Map<String, Object> out = findPage.execute(status, page, size, sortColumn, direction);
    List<TicketResponse> tickets = (List<TicketResponse>) out.get("P_TICKETS");
    long total = ((Number) out.get("P_TOTAL")).longValue();
    int totalPages = (int) ((total + size - 1) / size);
    return new PageResponse<>(tickets, page, size, total, totalPages);
}
```

`(total + size - 1) / size` divides and rounds up: 12 tickets in pages of 5 are 3 pages.

**Check:** `mvn -q compile` prints no errors.

### Step 5: check the values in the service

The caller sends text. The service checks it before the package is called. In `TicketService`, add the allowed sorts and a limit:

```java
// The fields a list may be sorted by, and the column each one stands for.
private static final Map<String, String> SORT_COLUMNS = Map.of("dueBy", "DUE_BY", "createdAt", "CREATED_AT");
private static final int MAX_SIZE = 50;
```

Replace `find` with `findPage`:

```java
public PageResponse<TicketResponse> findPage(String status, int page, int size, String sort, String direction) {
    if (page < 0) {
        throw new InvalidRequestException("page must be 0 or more");
    }
    if (size < 1 || size > MAX_SIZE) {
        throw new InvalidRequestException("size must be from 1 to " + MAX_SIZE);
    }
    String column = SORT_COLUMNS.get(sort);
    if (column == null) {
        throw new InvalidRequestException("Cannot sort by '" + sort + "'. Use dueBy or createdAt");
    }
    if (!direction.equalsIgnoreCase("asc") && !direction.equalsIgnoreCase("desc")) {
        throw new InvalidRequestException("direction must be asc or desc");
    }
    return ticketRepository.findPage(status, page, size, column, direction.toUpperCase());
}
```

`InvalidRequestException` already answers `400`, through the handler of part 3. The import for `Map` is `java.util.Map`.

### Step 6: the endpoint

In `TicketController`, replace the `find` method:

```java
@GetMapping
public PageResponse<TicketResponse> find(@RequestParam(required = false) String status,
                                         @RequestParam(defaultValue = "0") int page,
                                         @RequestParam(defaultValue = "5") int size,
                                         @RequestParam(defaultValue = "dueBy") String sort,
                                         @RequestParam(defaultValue = "asc") String direction) {
    return ticketService.findPage(status, page, size, sort, direction);
}
```

`defaultValue` is used when the caller does not send the parameter. Without parameters, the caller gets the first 5 tickets, the most urgent first. Remove the import of `java.util.List` if nothing else in the class uses it.

**Check:** `mvn -q clean verify` ends with no errors.

### Step 7: try it

Restart, and call:

```bash
curl "http://localhost:8080/api/tickets?size=2"
```

**Check:** you should see this shape, with the 12 sample tickets only:

```json
{"content":[{"id":3,"title":"Sample ticket 3","description":null,"status":"OPEN","priority":"HIGH",...},
            {"id":6,"title":"Sample ticket 6","description":null,"status":"OPEN","priority":"HIGH",...}],
 "page":0,"size":2,"totalElements":12,"totalPages":6}
```

Then try these:

| Call | You should see (12 sample tickets) |
|---|---|
| `/api/tickets` | 5 tickets, the 4 `HIGH` ones first. `totalPages` is `3` |
| `/api/tickets?page=2` | The last 2 tickets, both `LOW` |
| `/api/tickets?page=3` | `"content":[]`. `totalElements` is still `12` |
| `/api/tickets?size=4&sort=createdAt&direction=desc` | Tickets 12, 11, 10, 9: the newest first |
| `/api/tickets?status=CLOSED` | `"content":[]`, `"totalElements":0`, `"totalPages":0` |
| `/api/tickets?sort=colour` | `400` with `Cannot sort by 'colour'. Use dueBy or createdAt` |
| `/api/tickets?size=100` | `400` with `size must be from 1 to 50` |
| `/api/tickets?page=-1` | `400` with `page must be 0 or more` |

### Step 8: test the pages

Create `TicketPageTests`. The class is `@Transactional`, so each test rolls back. A `@BeforeEach` method creates three tickets, so that there are always at least two pages of 2:

```java
@Test
void thePagesTogetherHoldEveryTicketOnceInOrder() {
    List<TicketResponse> all = new ArrayList<>();
    PageResponse<TicketResponse> page = ticketService.findPage(null, 0, 2, "dueBy", "asc");
    for (int number = 0; number < page.totalPages(); number++) {
        all.addAll(ticketService.findPage(null, number, 2, "dueBy", "asc").content());
    }

    assertThat(all).hasSize((int) page.totalElements());
    assertThat(all).extracting(TicketResponse::id).doesNotHaveDuplicates();
    assertThat(all).extracting(TicketResponse::dueBy).isSortedAccordingTo(Comparator.naturalOrder());
}
```

The project has four tests: the size of a page and its total, every ticket once and in order, an empty page after the last, and a refused sort.

**Check:** `mvn clean verify` ends with `Tests run: 8, Failures: 0, Errors: 0` and `BUILD SUCCESS`.

---

## What the caller does with a page

| The caller wants | It uses |
|---|---|
| Show "page 2 of 3" | `page + 1` and `totalPages` |
| A "next" button | Enabled while `page + 1 < totalPages` |
| "12 tickets found" | `totalElements` |
| To stop | When `content` is empty |

---

## Common mistakes

| Symptom | Cause | Fix |
|---|---|---|
| `?page=1` returns the second page | Pages start at `0` | Ask for `page=0` first |
| `?page=99` answers `200` with an empty `content` | A page after the last has no tickets. It is not an error | Read `totalPages`, and stop before it |
| `ClassCastException: class java.math.BigDecimal cannot be cast to class java.lang.Integer` | `P_TOTAL` is an Oracle `NUMBER`, which arrives as a `BigDecimal` | Read it as a `Number`, then call `longValue()` |
| `NullPointerException` on the total | The key is written `"p_total"`. The keys of the `Map` are in upper case | Use `"P_TOTAL"` |
| `ClassCastException: class org.springframework.util.LinkedCaseInsensitiveMap cannot be cast to class ...TicketResponse` | The call has no `returningResultSet("P_TICKETS", ...)`, so each row arrives as a `Map` | Add `returningResultSet` with the row mapper |
| `?size=abc` answers `400` with no `message` | Spring cannot turn `abc` into an `int`, and answers before your code runs | Send a number |
| The same ticket appears on two pages | The `ORDER BY` has no column that is different in every row | End the `ORDER BY` with `ID` |
| `ORA-00933: SQL command not properly ended` when the package is created | The database is older than Oracle 12c, which has no `OFFSET ... FETCH` | Use Oracle 12c or later. (Not tried: no older database was available) |

---

## Practice

1. With the 12 sample tickets, walk through every page with `size=4`. Write down the number of tickets on each page, and `totalPages`.
2. Add `sort=title`. You need one entry in `SORT_COLUMNS` and two new `CASE` lines in the package. Why can `TITLE` not share a `CASE` line with `DUE_BY`? Hint: what type does each column have?
3. Change `MAX_SIZE` to `10`. Restart, and call `?size=25`. Write down the answer.

---

## Recap

- A page has a `page` number from `0`, a `size`, a `sort`, and a `direction`.
- Oracle reads one page with `ORDER BY ... OFFSET ... ROWS FETCH NEXT ... ROWS ONLY`. The `ORDER BY` always ends with a column that is different in every row.
- One procedure call gives back the page as a cursor and the total as a second `OUT` value. `PageResponse` gives both to the caller.
- Java allows only known sort names. The package never builds SQL from the caller's text.

### State at end of this part

| File | What changed |
|---|---|
| `db/oracle/03-page-package.sql` | New. `SUPPORT_TICKET_PAGE_PKG.find_page` |
| `db/oracle/04-sample-tickets.sql` | New, optional. 12 tickets |
| `dto/PageResponse.java` | New. `content`, `page`, `size`, `totalElements`, `totalPages` |
| `repository/TicketRepository.java` | `findPage` |
| `service/TicketService.java` | `findPage` checks the values. `find` is gone |
| `api/TicketController.java` | `GET /api/tickets` returns a page |
| `test/.../TicketPageTests.java` | New. Four tests that roll back |

| Request | Result |
|---|---|
| `GET /api/tickets` | `200`, the first 5 tickets, the most urgent first |
| `GET /api/tickets?page=1&size=3&sort=createdAt&direction=desc` | `200`, the page asked for |
| `GET /api/tickets?sort=colour` | `400` with `Cannot sort by 'colour'. Use dueBy or createdAt` |
