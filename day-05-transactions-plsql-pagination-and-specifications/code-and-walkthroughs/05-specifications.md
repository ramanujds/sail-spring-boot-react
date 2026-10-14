# Specifications: filters that combine

The list can filter by `status` and by `priority`. The controller has one `if` for each combination: both, only status, only priority, none. Add a `keyword` filter and an `overdue` filter, and four filters give 16 combinations. In this note you replace the `if` chain with **specifications**: small conditions that you add to the query only when the caller sends them.

**Starting point:** your project at the end of [part 4](04-pagination.md).
**Project:** [support-assistant-jpa](support-assistant-jpa/) in this folder.

## What you will build

- A `TicketSpecifications` class with one method per condition.
- One list endpoint that takes `status`, `priority`, `keyword`, `overdue`, and a page, in any combination.

---

## 1. The idea

A **specification** is one condition on a ticket, as an object: "the status is `OPEN`", "the title or description contains `printer`". Specifications join with `and`:

```text
hasStatus(OPEN)  and  hasPriority(HIGH)  and  matchesKeyword("printer")
```

| | `if` chain | Specifications |
|---|---|---|
| A new filter | Doubles the number of branches | One new method |
| Code per combination | One branch each | None |
| Each condition | Repeated in each branch | Written once |

---

## Build steps

### Step 1: the conditions

Create `TicketSpecifications` in `repository`:

```java
public class TicketSpecifications {

    public static Specification<Ticket> hasStatus(TicketStatus status) {
        return (root, query, cb) -> cb.equal(root.get("status"), status);
    }

    public static Specification<Ticket> hasPriority(Priority priority) {
        return (root, query, cb) -> cb.equal(root.get("priority"), priority);
    }

    // The keyword is in the title or in the description, in any case.
    public static Specification<Ticket> matchesKeyword(String keyword) {
        return (root, query, cb) -> {
            String pattern = "%" + keyword.toLowerCase() + "%";
            return cb.or(
                    cb.like(cb.lower(root.get("title")), pattern),
                    cb.like(cb.lower(root.get("description")), pattern));
        };
    }

    // Not finished, and the due date has passed. The same rule as SlaPolicy.isOverdue.
    public static Specification<Ticket> isOverdue(LocalDateTime now) {
        return (root, query, cb) -> cb.and(
                root.get("status").in(TicketStatus.OPEN, TicketStatus.IN_PROGRESS),
                cb.lessThan(root.get("dueBy"), now));
    }
}
```

Each method returns a lambda with three parameters:

| Parameter | Meaning |
|---|---|
| `root` | The ticket. `root.get("status")` is the field `status` of `Ticket` |
| `query` | The whole query. You do not need it here |
| `cb` | The builder of conditions: `equal`, `like`, `lessThan`, `and`, `or`, `lower` |

The names in `root.get("...")` are field names of `Ticket`, not column names.

`Specification` is `org.springframework.data.jpa.domain.Specification`.

**Check:** `mvn -q compile` prints no errors.

### Step 2: let the repository use them

In `TicketRepository`:

1. Add `JpaSpecificationExecutor<Ticket>` after `JpaRepository`:

```java
public interface TicketRepository extends JpaRepository<Ticket, UUID>, JpaSpecificationExecutor<Ticket> {
```

2. Delete the three list methods of part 4: `findAllByStatus`, `findAllByPriority`, and `findAllByStatusAndPriority`. Specifications replace them.

`JpaSpecificationExecutor` gives the interface the method `findAll(Specification, Pageable)`. You do not declare it.

**Check:** `mvn -q compile` shows errors in `TicketController`, which still calls the methods you deleted. Step 4 fixes them.

### Step 3: join the conditions that the caller sent

In `TicketService`, add:

```java
// Every filter is optional. Only the filters that were sent are added to the query.
public Page<Ticket> findAll(TicketStatus status, Priority priority, String keyword,
                            boolean overdue, Pageable pageable) {
    Specification<Ticket> spec = (root, query, cb) -> cb.conjunction(); // always true
    if (status != null) {
        spec = spec.and(TicketSpecifications.hasStatus(status));
    }
    if (priority != null) {
        spec = spec.and(TicketSpecifications.hasPriority(priority));
    }
    if (keyword != null && !keyword.isBlank()) {
        spec = spec.and(TicketSpecifications.matchesKeyword(keyword));
    }
    if (overdue) {
        spec = spec.and(TicketSpecifications.isOverdue(LocalDateTime.now()));
    }
    return ticketRepository.findAll(spec, pageable);
}
```

The first line is the condition "anything": `cb.conjunction()` is always true. Each `and` returns a new, longer specification, so the result is assigned back to `spec`.

Four `if` statements handle all 16 combinations.

### Step 4: one endpoint

In `TicketController`, replace `findAll`:

```java
// GET /api/v1/tickets?status=OPEN&priority=HIGH&keyword=printer&overdue=true&page=0&size=5&sort=title
// Every parameter is optional. Without any, you get the first 5 tickets, the most urgent first.
@GetMapping
public PageResponse<TicketResponse> findAll(@RequestParam(required = false) TicketStatus status,
                                            @RequestParam(required = false) Priority priority,
                                            @RequestParam(required = false) String keyword,
                                            @RequestParam(defaultValue = "false") boolean overdue,
                                            @PageableDefault(size = 5, sort = "dueBy") Pageable pageable) {
    checkSort(pageable);
    return TicketMapper.toPageResponse(
            ticketService.findAll(status, priority, keyword, overdue, pageable));
}
```

The controller has no filter logic. It checks the sort, as in part 4, and passes the values on.

**Check:** `mvn -q clean verify` ends with no errors.

### Step 5: try it

Restart. Use the tickets you made in the earlier parts, or create some. Call:

```bash
curl "http://localhost:8080/api/v1/tickets?status=OPEN&priority=HIGH"
curl "http://localhost:8080/api/v1/tickets?keyword=PRINT"
curl "http://localhost:8080/api/v1/tickets?overdue=true&size=3"
curl "http://localhost:8080/api/v1/tickets?overdue=true&priority=LOW&sort=title"
curl "http://localhost:8080/api/v1/tickets?status=CLOSED&keyword=close&size=2"
curl "http://localhost:8080/api/v1/tickets?overdue=maybe"
```

**Check:**

| Call | You should see |
|---|---|
| `status=OPEN&priority=HIGH` | Only tickets that are both `OPEN` and `HIGH` |
| `keyword=PRINT` | Tickets with `print` in the title or description, in any case |
| `overdue=true` | Only tickets with `"overdue":true` |
| `overdue=true&priority=LOW&sort=title` | Overdue `LOW` tickets, in title order |
| `status=CLOSED&keyword=close` | Closed tickets with `close` in the title or description |
| `overdue=maybe` | `400` with `'maybe' is not a valid value for 'overdue'` |

In the log, the SQL has exactly the conditions you sent. For `status=CLOSED&keyword=close`:

```text
select count(t1_0.id) from tickets t1_0 where t1_0.status=? and (lower(t1_0.title) like ? or lower(t1_0.description) like ?)
```

Call with no filters, and the `where` is gone.

---

## When to use which

| Need | Use |
|---|---|
| A fixed question with one or two parameters | A method name, such as `countByStatus` |
| A fixed, longer query | `@Query` |
| Filters that the caller may send in any combination | Specifications |

---

## Common mistakes

| Symptom | Cause | Fix |
|---|---|---|
| `Could not resolve attribute 'statuss' of 'com.bsp.supportassistant.model.Ticket'` with a `500`, when a request uses the filter | The field name in `root.get("...")` is misspelled. It is checked when the query runs, not when the application starts | Use the field name from `Ticket` |
| A filter is ignored and the list is not filtered | `spec.and(...)` was called without `spec = ` in front. `and` returns a new specification | Assign the result: `spec = spec.and(...)` |
| `method does not override or implement a method from a supertype` | The repository does not extend `JpaSpecificationExecutor<Ticket>` | Add it to the `extends` list |
| A keyword filter finds nothing for `Printer` | The condition compares with case | Use `cb.lower(...)` and `toLowerCase()`, as in `matchesKeyword` |

---

## Practice

1. Add a filter `hasNoComments()` that matches tickets with no comments. Hint: `cb.isEmpty(root.get("comments"))`. Add a `noComments` parameter to the endpoint and check it.
2. Add `createdAfter(LocalDateTime time)` with `cb.greaterThan(root.get("createdAt"), time)`, and a `createdAfter` parameter. A time is written as `2026-10-14T23:00:00`. Check it with a time in the past and a time in the future.
3. Call `?status=OPEN&priority=HIGH&overdue=true&keyword=printer&sort=title&size=2`. Read the `select` in the log, and write down each part of the `where` and the filter that produced it.

---

## Recap

- A **specification** is one condition. `and` joins them.
- Build the query from the parameters that were sent: start with a condition that is always true, then `spec = spec.and(...)`.
- The repository extends `JpaSpecificationExecutor`, and `findAll(spec, pageable)` filters and pages in one call.
- A new filter is one new method and one `if`. It does not change the existing filters.

### State at end of this part

| File | What changed |
|---|---|
| `repository/TicketSpecifications.java` | `hasStatus`, `hasPriority`, `matchesKeyword`, `isOverdue` |
| `repository/TicketRepository.java` | Extends `JpaSpecificationExecutor`. The three filter methods of part 4 are gone |
| `service/TicketService.java` | `findAll` joins the conditions |
| `api/TicketController.java` | One `findAll` with the four filters and a page |

| Request | Result |
|---|---|
| `GET /api/v1/tickets?status=&priority=&keyword=&overdue=&page=&size=&sort=` | `200`, one page of the tickets that match every parameter sent |
