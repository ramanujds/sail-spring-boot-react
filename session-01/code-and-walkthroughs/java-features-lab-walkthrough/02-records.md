# Step 2 — Records

> [Back to index](README.md) · Previous: [Step 1 — How the lab works](01-how-the-lab-works.md) · Next: [Step 3 — Sealed types](03-sealed-types.md)

**Package:** `records` · **TODOs:** 1.1 and 1.2 · **Tests:** `TicketSummaryTest`, `TicketSummaryMapperTest`

## Goal

Replace an entity-style class with a record for the data you hand to a caller, validate it in a compact constructor, and map a list of entities to a list of records.

## Why this matters

In a Spring Boot application, an entity is managed by the persistence provider. It is mutable, it needs a no-argument constructor, and it can change after you hand it out. Returning it from a controller couples your API to your table design and lets callers see state that changes underneath them. This is why the programme returns DTOs and never entities.

A DTO has the opposite shape: it carries a snapshot, it is equal when its data is equal, and it cannot change. In Java 8 style code that means a constructor, getters, `equals`, `hashCode`, and `toString`, all written by hand and all needing to stay in sync. A record states the components once and the compiler generates the rest, so they cannot drift apart. Spring MVC and Jackson read and write records directly.

What a record does not generate is *rules*. Nothing stops `new TicketSummary(0, "  ", null)`. The compact constructor is where those rules live, and it runs on every construction, so an invalid summary can never exist.

> **Note:** A record cannot be a JPA entity. Entities need a no-argument constructor and mutable state that the persistence provider manages. Use records for the data you send and receive.

## 1. Read the provided classes

`TicketRow` is a Java 8 style, entity-like class. It has getters and setters, and no `equals`:

<details>
<summary>Full <code>TicketRow.java</code></summary>

```java
package com.helpdeskpro.lab.records;

import com.helpdeskpro.lab.domain.TicketStatus;

/**
 * A mutable, entity-style class with getters and setters.
 *
 * It stands in for a persistence entity. Notice what it does not give you:
 * two rows with the same data are not equal, and anyone holding a reference
 * can change it after you have handed it out.
 */
public class TicketRow {

    private final long id;
    private String title;
    private TicketStatus status;
    private String assignedTo;

    public TicketRow(long id, String title, TicketStatus status, String assignedTo) {
        this.id = id;
        this.title = title;
        this.status = status;
        this.assignedTo = assignedTo;
    }

    public long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public TicketStatus getStatus() {
        return status;
    }

    public void setStatus(TicketStatus status) {
        this.status = status;
    }

    public String getAssignedTo() {
        return assignedTo;
    }

    public void setAssignedTo(String assignedTo) {
        this.assignedTo = assignedTo;
    }
}
```

</details>

`TicketSummary` is the record you work on, and `TicketSummaryMapper` is where entities become records. You will edit both in a moment.

## 2. Run the demo

```bash
mvn -q compile
java -cp target/classes com.helpdeskpro.lab.records.RecordsDemo
```

**Expected:**

```text
row equals sameRow:         false
summary equals sameSummary: true
summary:                    TicketSummary[id=101, title=Printer jam, status=OPEN]
row title after change:     Printer on fire
summary title after change: Printer jam
blank title: accepted (complete TODO 1.1 to reject it)
zero id: accepted (complete TODO 1.1 to reject it)
TODO 1.2: implement toSummaries
```

Read the first five lines against `TicketRow` and `TicketSummary`. Two rows with the same data are not equal, two summaries are, and a summary does not follow later changes to the row. The last three lines are the gaps you close next.

## 3. Play around

1. Add a component `String assignedTo` to `TicketSummary` and fix the compile errors. What changes in `toString` without any other edit?
2. Call `summary.getTitle()` in `RecordsDemo`. What does the compiler say, and what is the accessor called instead?
3. Add `equals` to `TicketRow` for `id` only, then re-run the first demo line. Why is a record's generated `equals` safer than this one?
4. Undo your changes before you continue.

## 4. TODO 1.1: validate in a compact constructor

Open `TicketSummary.java`. The contract is:

| Input | Result |
|---|---|
| `title` is `null` | `NullPointerException` |
| `status` is `null` | `NullPointerException` |
| `id` is zero or negative | `IllegalArgumentException` |
| `title` is blank | `IllegalArgumentException` |

A compact constructor has no parameter list. You check the incoming values by name, and the compiler assigns the fields for you afterwards. You never write `this.title = title` in it.

```bash
mvn test -Dtest=TicketSummaryTest
```

**Expected:**

```text
[INFO] Tests run: 5, Failures: 0, Errors: 0, Skipped: 0
```

Four of the five were red before you started.

## 5. TODO 1.2: map entities to summaries

Open `TicketSummaryMapper.java`. This is the shape of nearly every service method that returns a list: take entities, return DTOs. The contract is:

| Requirement | Detail |
|---|---|
| Conversion | Each `TicketRow` through `TicketSummary.from(...)` |
| Order | The original order is kept |
| Mutability | The returned list is unmodifiable |
| Empty input | An empty result |

```bash
mvn test -Dtest=TicketSummaryMapperTest
```

**Expected:**

```text
[INFO] Tests run: 3, Failures: 0, Errors: 0, Skipped: 0
```

## Try it

Run the demo again:

```bash
mvn -q compile
java -cp target/classes com.helpdeskpro.lab.records.RecordsDemo
```

**Expected:** the last three lines change to:

```text
blank title: rejected with IllegalArgumentException
zero id: rejected with IllegalArgumentException
toSummaries: [TicketSummary[id=101, title=Printer on fire, status=OPEN], TicketSummary[id=101, title=Printer jam, status=OPEN]]
```

The mapper ran after the row's title was changed, so the first summary shows the new title and the second shows the original. A summary is a snapshot of the moment it was built.

## Checkpoint

These are `TicketSummary.java` and `TicketSummaryMapper.java` as provided, with TODOs 1.1 and 1.2 open. Your versions add a compact constructor and a method body, and nothing else.

<details>
<summary>Full <code>TicketSummary.java</code></summary>

```java
package com.helpdeskpro.lab.records;

import com.helpdeskpro.lab.domain.TicketStatus;

/**
 * A read-only view of a ticket that is safe to hand to a caller.
 *
 * The components are declared for you. The validation rules are TODO 1.1.
 */
public record TicketSummary(long id, String title, TicketStatus status) {

    // ---------------------------------------------------------------------
    // TODO 1.1: validate in a compact constructor
    //
    // Add a compact constructor (a constructor with no parameter list) that
    // rejects invalid values:
    //
    //   title is null           -> NullPointerException
    //   status is null          -> NullPointerException
    //   id is zero or negative  -> IllegalArgumentException
    //   title is blank          -> IllegalArgumentException
    //
    // Do not assign the fields yourself; a compact constructor assigns them
    // for you after your checks.
    //
    // Hints: Objects.requireNonNull, String.isBlank()
    // ---------------------------------------------------------------------

    // Provided: build a summary from an entity-style row.
    public static TicketSummary from(TicketRow row) {
        return new TicketSummary(row.getId(), row.getTitle(), row.getStatus());
    }
}
```

</details>

This matches [TicketSummary.java](../java-features-lab/src/main/java/com/helpdeskpro/lab/records/TicketSummary.java) exactly.

<details>
<summary>Full <code>TicketSummaryMapper.java</code></summary>

```java
package com.helpdeskpro.lab.records;

import java.util.List;

/**
 * Maps entity-style rows to summaries, the way a service maps entities to
 * response DTOs before returning them from a controller.
 */
public final class TicketSummaryMapper {

    private TicketSummaryMapper() {
    }

    // ---------------------------------------------------------------------
    // TODO 1.2: map a list of rows to a list of summaries
    //
    // Convert every TicketRow with TicketSummary.from(...).
    //
    //  - keep the original order
    //  - the returned list must be unmodifiable
    //  - an empty input gives an empty result
    //
    // Hints: List.stream(), Stream.map(...), Stream.toList()
    // ---------------------------------------------------------------------
    public static List<TicketSummary> toSummaries(List<TicketRow> rows) {
        throw new UnsupportedOperationException("TODO 1.2: implement toSummaries");
    }
}
```

</details>

This matches [TicketSummaryMapper.java](../java-features-lab/src/main/java/com/helpdeskpro/lab/records/TicketSummaryMapper.java) exactly.

## Common mistakes

| Symptom | Cause | Fix |
|---|---|---|
| `cannot assign a value to final variable title` | You assigned the field inside the compact constructor | Remove the assignment; the compiler assigns the components after your checks |
| `nullTitleIsRejected` fails with `IllegalArgumentException` | A `null` title was folded into the blank check and threw the wrong type | Check for `null` first and throw `NullPointerException` |
| `returnsAnUnmodifiableList` fails | The result came from `collect(Collectors.toList())`, which is mutable | Use the stream method that returns an unmodifiable list |
| The tests pass but the demo still prints the TODO message | The demo has not been recompiled | Run `mvn -q compile` again |

Next: **[Step 3 — Sealed types](03-sealed-types.md)**.
