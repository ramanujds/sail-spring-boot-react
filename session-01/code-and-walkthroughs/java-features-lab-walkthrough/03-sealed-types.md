# Step 3 — Sealed types

> [Back to index](README.md) · Previous: [Step 2 — Records](02-records.md) · Next: [Step 4 — Pattern matching](04-pattern-matching.md)

**Package:** `sealedtypes` · **TODOs:** none · **Tests:** none

## Goal

Understand a sealed interface well enough to model the closed set of things that can happen to a ticket, and see what the compiler does when that set changes.

## Why this matters

Some sets are closed by nature. A ticket is opened, assigned, resolved, or closed, and nothing else. An ordinary interface cannot say that: anyone can add an implementation, so every `if` chain or `switch` over it needs a `default`, and nothing tells you when a new kind arrives. That `default` is where bugs hide, because it silently treats a new case like an unknown one.

A sealed interface closes the set. The compiler knows every permitted subtype, so code that branches on it can be checked for completeness. In an event-driven service layer, adding a new kind of ticket event then points you at every place that must handle it, at build time.

This step has no TODO. You read the type and experiment with it, because the next step builds on it.

## 1. Read the type

`TicketEvent` is a sealed interface with four record subtypes, all in one file. Because they share the file, no `permits` clause is needed.

<details>
<summary>Full <code>TicketEvent.java</code></summary>

```java
package com.helpdeskpro.lab.sealedtypes;

/**
 * The closed set of things that can happen to a ticket.
 *
 * The interface is sealed, and every implementation lives in this file, so the
 * compiler knows the complete list of subtypes without a {@code permits} clause.
 */
public sealed interface TicketEvent {

    record Opened(long ticketId, String title) implements TicketEvent {}

    record Assigned(long ticketId, String agent) implements TicketEvent {}

    record Resolved(long ticketId, String resolution) implements TicketEvent {}

    record Closed(long ticketId) implements TicketEvent {}
}
```

</details>

Each event is a record, so you get an immutable, self-describing value for free. That is the combination you see throughout the programme: sealed interface at the top, records below it.

## 2. Run the demo

```bash
mvn -q compile
java -cp target/classes com.helpdeskpro.lab.sealedtypes.SealedTypesDemo
```

**Expected:**

```text
Opened[ticketId=101, title=Printer jam]
Assigned[ticketId=101, agent=agent-1]
Resolved[ticketId=101, resolution=Replaced the toner]
Closed[ticketId=101]
sealed:    true
permitted: [Assigned, Closed, Opened, Resolved]
```

The last two lines come from reflection: the JVM itself knows the type is sealed and which classes it permits.

## 3. Play around

Use a scratch class or the demo's `main`:

1. Create a class `Reassigned implements TicketEvent` in another file. What does the compiler say, and why?
2. Declare `Reassigned` as a fifth record inside `TicketEvent`, then re-run the demo. What does the `permitted` line show now?
3. Try to add a `permits` clause to `TicketEvent` that lists only three of the four records. What happens?
4. Remove the fifth record and any `permits` clause again before you continue. The `switch` you write in the next step must cover exactly the four original events.

## Try it

There is nothing to run beyond the demo. You are done with this step when you can answer these without looking:

- What makes the set of `TicketEvent` subtypes closed?
- Why is a record a good implementation of a sealed interface?
- Where would a `default` branch hide a mistake if `TicketEvent` were a plain interface?

## Checkpoint

Nothing for you to edit in this step. The `TicketEvent.java` listing above is the file exactly as provided, and it matches [TicketEvent.java](../java-features-lab/src/main/java/com/helpdeskpro/lab/sealedtypes/TicketEvent.java).

## Common mistakes

| Symptom | Cause | Fix |
|---|---|---|
| `class is not allowed to extend sealed class` | The implementation is outside the permitted set | Declare it in the same file, or add it to a `permits` clause |
| `sealed class must have subclasses` | A sealed interface with no implementations | Add at least one permitted subtype |
| A record you added does not show up as permitted | It was declared in a different file with no `permits` clause | Declare it in `TicketEvent.java` |

Next: **[Step 4 — Pattern matching](04-pattern-matching.md)**.
