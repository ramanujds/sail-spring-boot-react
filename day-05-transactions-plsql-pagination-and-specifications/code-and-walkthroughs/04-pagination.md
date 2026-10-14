# Pagination

`GET /api/v1/tickets` returns every ticket. With 30 tickets that is fine. With 30,000 it is slow, large, and no screen can show it. In this note the list returns one **page** at a time: 5 tickets, then the next 5, and so on.

**Starting point:** your project at the end of [part 3](03-calling-plsql.md).
**Project:** [support-assistant-jpa](support-assistant-jpa/) in this folder.

## What you will build

- A list that takes `page`, `size`, and `sort`.
- A `PageResponse` that tells the caller which page it has and how many there are.
- A comment count that is read with the ticket, so a page never loads comments.
- A clear `400` for a `sort` that does not exist.

---

## 1. The idea

A page is described by three values:

| Parameter | Meaning | Example |
|---|---|---|
| `page` | Which page. The first page is `0` | `page=1` |
| `size` | How many tickets in a page | `size=5` |
| `sort` | The field to order by, and `asc` or `desc` | `sort=dueBy,asc` |

With 30 tickets and `size=5`, there are 6 pages, `0` to `5`. Page `1` holds tickets 6 to 10 of the sorted list.

Spring Data does the work. Your repository method takes a `Pageable` (the three values), and returns a `Page` (the tickets of that page, and the total). Two SQL statements run: one for the page, and one that counts all rows, so that the caller can know the number of pages.

---

## Build steps

### Step 1: the repository returns a page

In `TicketRepository`, change the three filter methods to take a `Pageable` and return a `Page<Ticket>`. Remove their `@EntityGraph`:

```java
Page<Ticket> findAllByStatus(TicketStatus status, Pageable pageable);

Page<Ticket> findAllByPriority(Priority priority, Pageable pageable);

Page<Ticket> findAllByStatusAndPriority(TicketStatus status, Priority priority, Pageable pageable);
```

The imports are `org.springframework.data.domain.Page` and `org.springframework.data.domain.Pageable`.

`findAll(Pageable)` is already in `JpaRepository`, so you do not declare it.

### Step 2: a response for a page

A `Page` holds more than a caller needs. Create `PageResponse` in `dto`, with the five values that matter:

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

In `TicketMapper`, add a method that fills it:

```java
public static PageResponse<TicketResponse> toPageResponse(Page<Ticket> page) {
    return new PageResponse<>(
            toResponseList(page.getContent()),
            page.getNumber(),
            page.getSize(),
            page.getTotalElements(),
            page.getTotalPages());
}
```

| `Page` method | Meaning |
|---|---|
| `getContent()` | The tickets of this page |
| `getNumber()` | The page number, from `0` |
| `getSize()` | The size that was asked for |
| `getTotalElements()` | How many tickets match, in all pages |
| `getTotalPages()` | How many pages there are |

### Step 3: the controller

In `TicketController`, replace `findAll`:

```java
// GET /api/v1/tickets?page=0&size=5&sort=dueBy,asc returns one page of tickets.
// Without page, size, and sort, you get the first 5 tickets, the most urgent first.
@GetMapping
public PageResponse<TicketResponse> findAll(@RequestParam(required = false) TicketStatus status,
                                            @RequestParam(required = false) Priority priority,
                                            @PageableDefault(size = 5, sort = "dueBy") Pageable pageable) {
    if (status != null && priority != null) {
        return TicketMapper.toPageResponse(ticketRepository.findAllByStatusAndPriority(status, priority, pageable));
    }
    if (status != null) {
        return TicketMapper.toPageResponse(ticketRepository.findAllByStatus(status, pageable));
    }
    if (priority != null) {
        return TicketMapper.toPageResponse(ticketRepository.findAllByPriority(priority, pageable));
    }
    return TicketMapper.toPageResponse(ticketRepository.findAll(pageable));
}
```

Spring reads `page`, `size`, and `sort` from the URL and builds the `Pageable` for you. `@PageableDefault` gives the values to use when the caller sends none: 5 tickets, sorted by `dueBy`, so the most urgent come first. Its import is `org.springframework.data.web.PageableDefault`.

**Check:** `mvn -q clean verify` ends with no errors.

### Step 4: count the comments in the database

Restart and call the list:

```bash
curl -i "http://localhost:8080/api/v1/tickets"
```

**Check:** you get `500`, and the log has `LazyInitializationException`. The message is `Cannot lazily initialize collection of role '...Ticket.comments' ... (no session)` on Spring Boot 4, and `failed to lazily initialize a collection of role: ...Ticket.comments: could not initialize proxy - no Session` on Spring Boot 3.5.

This is the error from [Day 4, part 4](../../day-04-exception-handling-relationships-and-composite-keys/code-and-walkthroughs/04-lazy-loading-and-n-plus-1.md). The mapper reads `ticket.getComments().size()`, and the paged query did not load the comments.

Day 4 fixed it with `@EntityGraph`, which joins the comments to the tickets. A join does not fit with a page: a ticket with 3 comments is 3 rows, and "5 rows" is not "5 tickets". Hibernate then reads **every** row and cuts the page in Java. On Spring Boot 3.5 the log says:

```text
HHH90003004: firstResult/maxResults specified with collection fetch; applying in memory
```

With 30,000 tickets, that is the problem pagination was meant to remove.

The list does not need the comments. It needs their **number**. Ask the database to count them, as part of reading the ticket. In `Ticket`, add a field:

```java
// The number of comments. The database counts them when the ticket is read. It is not a column.
@Formula("(select count(*) from COMMENTS c where c.TICKET_ID = ID)")
private int commentCount;

public int getCommentCount() {
    return commentCount;
}
```

The import is `org.hibernate.annotations.Formula`.

| Part | Meaning |
|---|---|
| `@Formula("...")` | Instead of a column, read this SQL expression with the ticket |
| `c.TICKET_ID = ID` | The comments of this ticket. Hibernate puts the ticket's table name in front of `ID` |
| No setter | The value is only read |

Use it in `TicketMapper.toResponse`, in place of `ticket.getComments().size()`:

```java
ticket.getCommentCount(),
```

`TicketRepository` no longer needs `@EntityGraph` anywhere. Delete the `findAll()` and `findById` overrides and the annotation on `search`, together with the imports that become unused. `Ticket` keeps its `comments` list: it is still what deletes the comments with the ticket.

**Check:** restart and call the list again. You get `200`, and each ticket has its `commentCount`. The log now has the count inside the same statement, and the page limit in the SQL:

```text
Hibernate: select t1_0.id,(select count(*) from COMMENTS c where c.TICKET_ID = t1_0.ID),t1_0.created_at,...
from tickets t1_0 order by t1_0.due_by offset ? rows fetch first ? rows only
```

### Step 5: limit the size

A caller can ask for `size=100000`. Cap it in `application.yaml`, under `spring`:

```yaml
  data:
    web:
      pageable:
        max-page-size: 50
```

**Check:** the file still has one `spring:` block, and `data:` is indented two spaces, like `jpa:`.

### Step 6: try it

Restart, and call:

```bash
curl "http://localhost:8080/api/v1/tickets"
curl "http://localhost:8080/api/v1/tickets?page=1&size=3&sort=title,desc"
curl "http://localhost:8080/api/v1/tickets?status=OPEN&size=2"
curl "http://localhost:8080/api/v1/tickets?page=99"
curl "http://localhost:8080/api/v1/tickets?size=100000"
```

**Check:** the answers have this shape. With 30 tickets:

```json
{"content":[ ...5 tickets... ],"page":0,"size":5,"totalElements":30,"totalPages":6}
```

| Call | You should see |
|---|---|
| No parameters | 5 tickets, with the earliest `dueBy` first. `totalPages` is 6 |
| `page=1&size=3&sort=title,desc` | The 4th to 6th ticket, in reverse title order |
| `status=OPEN&size=2` | 2 tickets, all `OPEN`. `totalElements` is the number of open tickets |
| `page=99` | `"content":[]`, an empty list. `totalElements` is still 30 |
| `size=100000` | `"size":50`: the cap from step 5 |

In the log, the first call ran two statements:

```text
Hibernate: select t1_0.id,(select count(*) from COMMENTS c where c.TICKET_ID = t1_0.ID),... from tickets t1_0 order by t1_0.due_by offset ? rows fetch first ? rows only
Hibernate: select count(t1_0.id) from tickets t1_0
```

The first reads one page of tickets, each with its comment count. The second counts all tickets.

### Step 7: a clear error for a wrong `sort`

```bash
curl -i "http://localhost:8080/api/v1/tickets?sort=colour"
```

**Check:** you get `500`, and the log has `PropertyReferenceException: No property 'colour' found for type 'Ticket'`. The caller made the mistake, so it should be a `400`.

Do not catch Spring Data's exception. Decide yourself which fields a list may be sorted by. This also keeps callers from sorting by a field that you do not want to expose.

Create `InvalidSortException` in `exception`:

```java
public class InvalidSortException extends RuntimeException {

    public InvalidSortException(String field) {
        super("Cannot sort by '" + field + "'");
    }
}
```

Add a handler to `GlobalExceptionHandler`:

```java
@ExceptionHandler(InvalidSortException.class)
public ResponseEntity<ApiError> handleInvalidSort(InvalidSortException ex) {
    return build(HttpStatus.BAD_REQUEST, ex.getMessage());
}
```

In `TicketController`, list the allowed fields and check the sort before the query runs:

```java
// The fields a list may be sorted by.
private static final Set<String> SORT_FIELDS = Set.of("title", "status", "priority", "dueBy", "createdAt");

private void checkSort(Pageable pageable) {
    for (Sort.Order order : pageable.getSort()) {
        if (!SORT_FIELDS.contains(order.getProperty())) {
            throw new InvalidSortException(order.getProperty());
        }
    }
}
```

Call `checkSort(pageable);` as the first line of `findAll`. The `Sort` import is `org.springframework.data.domain.Sort`.

**Check:** the same call answers `400` with `Cannot sort by 'colour'`, and `?sort=title,desc` still works.

---

## What the caller does with a page

| The caller wants | It uses |
|---|---|
| Show "page 2 of 6" | `page + 1` and `totalPages` |
| A "next" button | Enabled while `page + 1 < totalPages` |
| "30 tickets found" | `totalElements` |
| To stop | When `content` is empty |

---

## Common mistakes

| Symptom | Cause | Fix |
|---|---|---|
| `500` for `?sort=colour`, and `No property 'colour' found for type 'Ticket'` in the log | The `sort` field is not a field of `Ticket`, and nothing checked it | Check the field before the query, as in step 7 |
| `500` with `LazyInitializationException` on the list | The mapper reads `getComments()`, and a paged query does not load the comments | Use `commentCount`, as in step 4 |
| The first page is `?page=1` and returns the second page | Pages start at `0` | Start at `page=0` |
| `?page=99` answers `200` with an empty `content` | A page after the last has no tickets. It is not an error | Read `totalPages`, and stop before it |
| `?page=abc` returns the first page, with no error | Spring ignores a `page` that is not a number and uses the default | Nothing to fix. Send a number |
| The same ticket appears on two pages | The list has no `sort`, so the database may order the rows differently in each call | Always sort. `@PageableDefault(sort = "dueBy")` sets a default |

---

## Practice

1. Create 12 tickets. Walk through them with `size=5` and `page=0`, `1`, `2`. Write down the `content` size of each page, and `totalPages`.
2. Call `?sort=priority,desc&size=3`. Write down the order of the priorities. Why is it not `HIGH`, `MEDIUM`, `LOW`? Hint: what does the database sort, a number or text?
3. Change `max-page-size` to `10`. Restart and call `?size=25`. Write down `size` in the answer.

---

## Recap

- A page has a `page` number from `0`, a `size`, and a `sort`.
- The repository takes a `Pageable` and returns a `Page`. Spring Data runs one statement for the page and one to count.
- A `PageResponse` gives the caller the tickets and the totals.
- Do not join a collection to a page. Read what the list needs, here a count, with `@Formula`.
- Cap `size`, always sort, and answer a bad `sort` with `400`.

### State at end of this part

| File | What changed |
|---|---|
| `repository/TicketRepository.java` | The filter methods take a `Pageable` and return a `Page`. No `@EntityGraph` |
| `model/Ticket.java` | `commentCount`, read with `@Formula` |
| `dto/PageResponse.java` | `content`, `page`, `size`, `totalElements`, `totalPages` |
| `mapper/TicketMapper.java` | `toPageResponse`. `toResponse` uses `getCommentCount()` |
| `api/TicketController.java` | `findAll` takes a `Pageable` and checks the sort |
| `exception/InvalidSortException.java`, `exception/GlobalExceptionHandler.java` | A sort field that is not allowed answers `400` |
| `application.yaml` | `max-page-size: 50` |

| Request | Result |
|---|---|
| `GET /api/v1/tickets` | `200`, a page of 5, the most urgent first |
| `GET /api/v1/tickets?page=1&size=3&sort=title,desc` | `200`, the page asked for |
| `GET /api/v1/tickets?sort=colour` | `400` with `Cannot sort by 'colour'` |
