# Custom repository methods

`JpaRepository` gives you `save`, `findAll`, `findById`, and a few more. Real questions are more specific: "all high-priority tickets that are still open", "how many tickets are open", "tickets that mention a printer". In this note you add repository methods that answer them, in two ways: by **naming** the method, and by **writing the query**.

**Starting point:** your project at the end of [part 1](01-exception-handling.md).
**Project:** [support-assistant-jpa](support-assistant-jpa/) in this folder.

## What you will build

- `GET /api/v1/tickets?status=OPEN&priority=HIGH`: filter by status, priority, or both.
- `GET /api/v1/tickets/count?status=OPEN`: how many tickets have a status.
- `GET /api/v1/tickets/search?keyword=printer`: tickets whose title or description contains a word.

---

## 1. A method name is a query

You already wrote one: `findAllByStatus(TicketStatus status)`. Spring Data reads the name from left to right and builds the SQL.

| Part of the name | Meaning |
|---|---|
| `find`, `count`, `exists` | What the method returns: rows, a number, or `true` / `false` |
| `All` | Optional. The result is a list |
| `By` | The conditions start here |
| `Status`, `Priority`, `Title` | A field of the entity. The spelling must match |
| `And`, `Or` | Join two conditions |
| `Containing` | The field contains the text (`like '%text%'`) |
| `IgnoreCase` | Compare without upper and lower case |
| `OrderByTitleAsc` | Sort the result |

| Method | Condition that Spring builds |
|---|---|
| `findAllByPriority(Priority priority)` | `where priority = ?` |
| `findAllByStatusAndPriority(TicketStatus status, Priority priority)` | `where status = ? and priority = ?` |
| `countByStatus(TicketStatus status)` | `select count(...) ... where status = ?` |
| `findAllByTitleContainingIgnoreCase(String keyword)` | `where upper(title) like upper(?)` |

The parameters go in the same order as the conditions in the name.

---

## Build steps

### Step 1: filter by priority and status

Add two methods to `TicketRepository`:

```java
List<Ticket> findAllByPriority(Priority priority);

List<Ticket> findAllByStatusAndPriority(TicketStatus status, Priority priority);
```

> **Note:** If you did practice task 2 of [part 6 of Day 3](../../day-03-rest-api-dtos-and-validation/code-and-walkthroughs/06-spring-data-jpa-with-oracle.md), you already have `findAllByPriority`.

In `TicketController`, give `findAll` a second optional parameter and choose the method:

```java
@GetMapping
public List<TicketResponse> findAll(@RequestParam(required = false) TicketStatus status,
                                    @RequestParam(required = false) Priority priority) {
    if (status != null && priority != null) {
        return TicketMapper.toResponseList(ticketRepository.findAllByStatusAndPriority(status, priority));
    }
    if (status != null) {
        return TicketMapper.toResponseList(ticketRepository.findAllByStatus(status));
    }
    if (priority != null) {
        return TicketMapper.toResponseList(ticketRepository.findAllByPriority(priority));
    }
    return TicketMapper.toResponseList(ticketRepository.findAll());
}
```

**Check:** restart. Create four tickets:

```bash
curl -X POST http://localhost:8080/api/v1/tickets -H "Content-Type: application/json" \
  -d '{"title":"Cannot print invoices","description":"Printer 3 on floor 2 is offline","priority":"HIGH"}'

curl -X POST http://localhost:8080/api/v1/tickets -H "Content-Type: application/json" \
  -d '{"title":"VPN drops every hour","description":"Disconnects from home","priority":"MEDIUM"}'

curl -X POST http://localhost:8080/api/v1/tickets -H "Content-Type: application/json" \
  -d '{"title":"New laptop request","description":"The new joiner needs a printer driver","priority":"LOW"}'

curl -X POST http://localhost:8080/api/v1/tickets -H "Content-Type: application/json" \
  -d '{"title":"Password reset","priority":"HIGH"}'
```

Take the `id` of the VPN ticket and set it `IN_PROGRESS`:

```bash
curl -X PATCH http://localhost:8080/api/v1/tickets/TICKET_ID -H "Content-Type: application/json" \
  -d '{"status":"IN_PROGRESS"}'
```

Now call:

```bash
curl "http://localhost:8080/api/v1/tickets?priority=HIGH"
curl "http://localhost:8080/api/v1/tickets?status=OPEN&priority=HIGH"
curl "http://localhost:8080/api/v1/tickets?status=IN_PROGRESS&priority=HIGH"
```

| Call | You should see |
|---|---|
| `?priority=HIGH` | `Cannot print invoices` and `Password reset` |
| `?status=OPEN&priority=HIGH` | The same two |
| `?status=IN_PROGRESS&priority=HIGH` | `[]`, an empty list. No ticket matches both |

In the log, the last call ran `... where t1_0.status=? and t1_0.priority=?`.

### Step 2: count by status

Add to `TicketRepository`:

```java
long countByStatus(TicketStatus status);
```

Add to `TicketController`:

```java
// GET /api/v1/tickets/count?status=OPEN
@GetMapping("/count")
public long count(@RequestParam TicketStatus status) {
    return ticketRepository.countByStatus(status);
}
```

`@RequestParam` without `required = false` means the parameter must be present. `/count` is fixed text, so Spring does not mistake it for a ticket id.

**Check:**

```bash
curl "http://localhost:8080/api/v1/tickets/count?status=OPEN"
curl "http://localhost:8080/api/v1/tickets/count?status=CLOSED"
```

You should see `3` and `0`. The log shows `select count(t1_0.id) from tickets t1_0 where t1_0.status=?`: the database counts, and no ticket is loaded into Java.

### Step 3: search by title

Add to `TicketRepository`:

```java
List<Ticket> findAllByTitleContainingIgnoreCase(String keyword);
```

Add to `TicketController`:

```java
// GET /api/v1/tickets/search?keyword=printer
@GetMapping("/search")
public List<TicketResponse> search(@RequestParam String keyword) {
    return TicketMapper.toResponseList(ticketRepository.findAllByTitleContainingIgnoreCase(keyword));
}
```

**Check:**

```bash
curl "http://localhost:8080/api/v1/tickets/search?keyword=PRINT"
```

You should see `Cannot print invoices`. The upper-case `PRINT` still matches, and `New laptop request` is not in the list, because its title has no `print`. The log shows:

```text
where upper(t1_0.title) like upper(?) escape '\'
```

### Step 4: search the description too

A ticket about a printer may have `printer` only in its description, like `New laptop request`. A name that checks both fields is:

```java
List<Ticket> findAllByTitleContainingIgnoreCaseOrDescriptionContainingIgnoreCase(String title, String description);
```

It works, but it is hard to read, and the caller has to pass the keyword twice. When a name gets long, write the query yourself with `@Query`. Replace the method from step 3 with:

```java
@Query("select t from Ticket t "
        + "where lower(t.title) like lower(concat('%', :keyword, '%')) "
        + "or lower(t.description) like lower(concat('%', :keyword, '%')) "
        + "order by t.title")
List<Ticket> search(@Param("keyword") String keyword);
```

In the controller, call `ticketRepository.search(keyword)`.

| Part | Meaning |
|---|---|
| `@Query("...")` | Use this query. The method name is free |
| `select t from Ticket t` | The query is written in **JPQL**. It uses the class `Ticket` and its fields, like `t.title`, not the table and column names |
| `:keyword` | A named parameter. It is filled in by the method parameter with `@Param("keyword")` |
| `lower(...)`, `like`, `concat` | Match without case, with the keyword anywhere in the text |
| `order by t.title` | Sort by title |

> **Note:** The imports are `org.springframework.data.jpa.repository.Query` and `org.springframework.data.repository.query.Param`.

**Check:**

```bash
curl "http://localhost:8080/api/v1/tickets/search?keyword=printer"
```

You should see two tickets, in title order: `Cannot print invoices` (the word is in the title) and `New laptop request` (the word is in the description).

### Step 5: say which parameter is missing

Call:

```bash
curl -i http://localhost:8080/api/v1/tickets/search
```

You see `400` with Spring's body, which has no message. Add one more handler to `GlobalExceptionHandler`:

```java
@ExceptionHandler(MissingServletRequestParameterException.class)
public ResponseEntity<ApiError> handleMissingParameter(MissingServletRequestParameterException ex) {
    return build(HttpStatus.BAD_REQUEST, "The parameter '" + ex.getParameterName() + "' is required");
}
```

**Check:** restart and call both endpoints without a parameter:

```bash
curl http://localhost:8080/api/v1/tickets/search
curl http://localhost:8080/api/v1/tickets/count
```

You should see `The parameter 'keyword' is required` and `The parameter 'status' is required`, each inside an `ApiError`.

---

## Which way to write a query

| | Method name | `@Query` |
|---|---|---|
| Good for | One or two simple conditions | Several conditions, `or` with brackets, joins, sorting you choose |
| Written as | `findAllByStatusAndPriority` | A JPQL string |
| Gets hard to read | When the name has more than three parts | Rarely |

Method names and the fields in a JPQL string are checked when the application starts, so a misspelled field never waits for a request. A wrong `@Param` name is not caught at start: it fails when the method is called.

---

## Common mistakes

| Symptom | Cause | Fix |
|---|---|---|
| `No property 'priorty' found for type 'Ticket'; Did you mean 'priority'` when the application starts | The name of a field is misspelled in the method name | Spell it like the field in `Ticket` |
| `Could not resolve attribute 'titel' of 'com.bsp.supportassistant.model.Ticket'` after `Query validation failed` | A field in the JPQL string is misspelled | Use the field names of the class |
| `Could not resolve root entity 'TICKETS'` | The JPQL uses the table name | Use the class name: `from Ticket t` |
| A `500` for the search, with `No argument for named parameter ':keyword'` in the log | The name in `@Param("...")` is not the name after the colon in the query | Make them the same |
| A search that returns nothing for `Printer` | The query compares with case | Use `lower(...)` on both sides, or `IgnoreCase` in the name |

---

## Practice

1. Add `findAllByStatusOrderByTitleAsc(TicketStatus status)` and use it for `?status=...` in place of `findAllByStatus`. Check that the titles come back in order.
2. Add `boolean existsByTitle(String title)`. Use it in `create` to throw an exception of your own when a ticket with that title exists, and handle the exception with `409` in `GlobalExceptionHandler`.
3. Add `List<Ticket> findAllByDescriptionIsNull()` and an endpoint `GET /api/v1/tickets/without-description` that uses it. Check that `Password reset`, which has no description, is listed.

---

## Recap

- A repository method name is a query: `find`, `count`, or `exists`, then `By`, then fields joined with `And` and `Or`.
- When the name gets long, write the query with `@Query`. JPQL uses class and field names, not table and column names.
- `count` and `exists` ask the database. They do not load the rows.
- Method names and JPQL strings are checked at start, so a misspelled field stops the application with a clear message.

### State at end of this part

| Class | What changed |
|---|---|
| `TicketRepository` | Added `findAllByPriority`, `findAllByStatusAndPriority`, `countByStatus`, and `search` |
| `TicketController` | `findAll` takes `status` and `priority`. New `GET /count` and `GET /search` |
| `GlobalExceptionHandler` | Handles a missing request parameter |

| Request | Result |
|---|---|
| `GET /api/v1/tickets?status=&priority=` | Tickets that match the parameters you send |
| `GET /api/v1/tickets/count?status=OPEN` | A number |
| `GET /api/v1/tickets/search?keyword=printer` | Tickets with the word in the title or description |
