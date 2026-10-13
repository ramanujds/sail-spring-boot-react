# UUIDs for ids

Until now a ticket's id is a number: `1`, `2`, `3`. The repository counts upwards and hands out the next number. This note replaces the number with a **UUID**, a long random id that the application creates without counting.

**Starting point:** your project at the end of [part 3](03-status-codes.md).
**Project:** [support-assistant](support-assistant/) in this folder. It holds the in-memory version of the application, with parts 1 to 4 applied. From part 6 on, the work continues in [support-assistant-jpa](support-assistant-jpa/).

> **Note:** The examples in parts 1 to 3 use ids such as `1` and `2`. From this part on, use the ids that the API returns.

## What you will build

- A ticket whose `id` is a `UUID`.
- A repository that creates the id with `UUID.randomUUID()`, and no counter.
- A controller that reads the id from the URL as a `UUID`.

---

## 1. What a UUID is

A **UUID** (Universally Unique Identifier) is a 128-bit value, written as 36 characters in five groups:

```text
92818c23-79b6-4d51-b5c5-617cd067fa71
```

Java has a class for it, `java.util.UUID`. Here are the three things you need:

```java
UUID id = UUID.randomUUID();                  // make a new one
String text = id.toString();                  // 36 characters
UUID same = UUID.fromString(text);            // read one from text
```

Two UUIDs are compared by value with `equals`:

```java
System.out.println(id.equals(same));   // true
System.out.println(id == same);        // false
```

`==` asks "are these the same object in memory". Two `UUID` objects with the same value are different objects, so `==` gives `false`. Always use `equals`.

> **Note:** `UUID.fromString("abc")` throws `IllegalArgumentException: Invalid UUID string: abc`.

---

## 2. Why use a UUID instead of a number

| | Number: `1`, `2`, `3` | UUID |
|---|---|---|
| **Guessing** | Anyone can try `/tickets/2`, then `/tickets/3` | No pattern to follow |
| **Who creates it** | A counter that the whole application shares | Any code, with no counter |
| **After a restart** | The counter goes back to `1`, so ids repeat | A new id is still different |
| **Two systems merged** | Both have a ticket `1` | No clash |
| **Typing it** | `1` | 36 characters |
| **Order** | `2` comes after `1` | No order |

The last two rows are the cost: a UUID is harder to read and to type, and it does not tell you which ticket came first.

> **Warning:** A UUID makes an id hard to guess. It does not decide who may read a ticket. Login and permissions come on a later day.

---

## Build steps

### Step 1: `Ticket` has a `UUID` id

In `Ticket`, import `java.util.UUID` and change the field, the getter, and the setter from `Long` to `UUID`:

```java
private UUID id;

public UUID getId() {
    return id;
}

public void setId(UUID id) {
    this.id = id;
}
```

**Check:** `mvn -q compile` shows errors in `TicketMapper` and `TicketRepository`, because they still treat the id as a `Long`. The next steps fix them.

### Step 2: `TicketResponse` has a `UUID` id

```java
public record TicketResponse(
        UUID id,
        String title,
        String description,
        TicketStatus status,
        Priority priority) {
}
```

`TicketRequest` does not change. It has no id.

### Step 3: the repository creates the id

In `TicketRepository`, remove the `nextId` field and create the id in `save`:

```java
public Ticket save(Ticket ticket) {
    ticket.setId(UUID.randomUUID());
    tickets.add(ticket);
    return ticket;
}
```

Change the parameter type from `Long` to `UUID` in `findById`, `update`, and `deleteById`:

```java
public Optional<Ticket> findById(UUID id) {
    return tickets.stream()
            .filter(ticket -> ticket.getId().equals(id))
            .findFirst();
}
```

### Step 4: the controller reads a `UUID`

In `TicketController`, change `@PathVariable Long id` to `@PathVariable UUID id` in `findById`, `update`, and `delete`:

```java
@GetMapping("/{id}")
public ResponseEntity<TicketResponse> findById(@PathVariable UUID id) {
    // unchanged
}
```

`TicketMapper` needs no change, because it copies the id without looking at its type.

**Check:** `mvn -q clean verify` ends with no errors.

### Step 5: Try it

Start the application and create a ticket:

```bash
curl -i -X POST http://localhost:8080/api/v1/tickets \
  -H "Content-Type: application/json" \
  -d '{"title":"Cannot print invoices","priority":"HIGH"}'
```

**Check:** you should see `HTTP/1.1 201` and an `id` of 36 characters, for example:

```json
{"id":"92818c23-79b6-4d51-b5c5-617cd067fa71","title":"Cannot print invoices","description":null,"status":"OPEN","priority":"HIGH"}
```

Copy your `id` and use it in place of `TICKET_ID`:

```bash
curl -i http://localhost:8080/api/v1/tickets/TICKET_ID
curl -i http://localhost:8080/api/v1/tickets/1
curl -i http://localhost:8080/api/v1/tickets/00000000-0000-0000-0000-000000000000
```

**Check:** the three calls give `200`, `400`, and `404` in that order.

| Call | Status | Why |
|---|---|---|
| `.../TICKET_ID` | `200` | The ticket exists |
| `.../1` | `400` | `1` is not a UUID, so Spring cannot read it |
| `.../00000000-0000-0000-0000-000000000000` | `404` | A valid UUID, but no ticket has it |

Open Swagger UI and look at the `id` of `TicketResponse`.

**Check:** it shows `string` with the format `uuid`.

---

## Common mistakes

| Symptom | Cause | Fix |
|---|---|---|
| `400 Bad Request` for `GET /api/v1/tickets/1` | The path value is not a UUID | Use the `id` from the response |
| `404` for an id that the API returned | The repository compares with `==` instead of `equals`, so two equal UUIDs are not seen as equal | Use `ticket.getId().equals(id)` |
| `incompatible types: java.lang.Long cannot be converted to java.util.UUID` | A method or variable still uses `Long` | Change it to `UUID`. Use **Alt + Enter** in IntelliJ to find each one |

---

## Practice

1. Create three tickets. Write down their ids. Restart the application and create one more. Does any id repeat? Compare with what happened in part 3.
2. Send a `POST` with `"id":"00000000-0000-0000-0000-000000000001"` in the body. Write down the `id` in the response and say why it is not the one you sent.
3. In a small `main` method, create two random UUIDs and compare them with `equals`. Then call `UUID.fromString("abc")` and write down the exception.

---

## Recap

- A **UUID** is a 36-character random id. Create one with `UUID.randomUUID()` and read one from text with `UUID.fromString(...)`.
- It needs no counter, does not repeat after a restart, and cannot be guessed by trying the next number. It is longer and has no order.
- Compare UUIDs with `equals`, never `==`.
- A UUID is not a replacement for login and permissions.

### State at end of this part

| Class | What changed |
|---|---|
| `Ticket` | `id` is a `UUID` |
| `TicketResponse` | `id` is a `UUID` |
| `TicketRepository` | No `nextId`. `save` uses `UUID.randomUUID()`. `findById`, `update`, and `deleteById` take a `UUID` |
| `TicketController` | The path variable is a `UUID` |

| Request | Result |
|---|---|
| `POST /api/v1/tickets` | `201`, with a UUID `id` |
| `GET`, `PATCH`, `DELETE /api/v1/tickets/{id}` with a UUID | `200` or `204`, or `404` when no ticket has it |
| The same with a value that is not a UUID | `400` |
