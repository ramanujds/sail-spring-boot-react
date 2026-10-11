# Step 4 — Pattern matching

> [Back to index](README.md) · Previous: [Step 3 — Sealed types](03-sealed-types.md) · Next: [Step 5 — Switch expressions](05-switch-expressions.md)

**Package:** `patternmatching` · **TODOs:** 2.1 and 2.2 · **Test:** `EventPatternsTest`

## Goal

Read a value out of an event with `instanceof` patterns, and describe every event with a `switch` that the compiler checks for completeness.

## Why this matters

Branching on the type of a value used to take three steps: test the type, cast, and use the casted variable. The cast is where mistakes creep in, because the compiler trusts you. Pattern matching for `instanceof` makes the test, the cast, and the variable one expression, so the variable exists only where the test succeeded.

Pattern matching for `switch` goes further, and combines with the sealed type from Step 3. The `case` label tests the type *and* takes a record apart. Because `TicketEvent` is sealed, a `switch` that covers every subtype needs no `default`. Add a new event later and every incomplete `switch` stops compiling.

In the service layer you build later, this is how you handle ticket events and statuses: one place per behaviour, checked by the compiler, with no fallthrough `else`.

## 1. Run the demo

```bash
mvn -q compile
java -cp target/classes com.helpdeskpro.lab.patternmatching.PatternMatchingDemo
```

**Expected:**

```text
--- instanceof pattern (provided) ---
opened ticket 101 titled Printer jam
--- assigneeOf (TODO 2.1) ---
TODO 2.1: implement assigneeOf
--- describe (TODO 2.2) ---
TODO 2.2: implement describe
```

The first section is provided and already uses pattern matching for `instanceof`. Open `printIfOpened` in `PatternMatchingDemo.java` and note that `opened` is only usable inside the `if`.

## 2. TODO 2.1: `assigneeOf`

Open `EventPatterns.java`. The contract is:

| Event | Result |
|---|---|
| `Assigned` | `Optional` containing the agent |
| Any other event | An empty `Optional` |

Use `instanceof` with a pattern variable. Do not cast by hand and do not call `getClass()`.

```bash
mvn test -Dtest='EventPatternsTest#assignee*'
```

**Expected:**

```text
[INFO] Tests run: 2, Failures: 0, Errors: 0, Skipped: 0
```

## 3. TODO 2.2: `describe`

In the same file, complete `describe`. The contract is:

| Event | Result |
|---|---|
| `Opened` | `Ticket <id> opened: <title>` |
| `Assigned` | `Ticket <id> assigned to <agent>` |
| `Resolved`, non-blank resolution | `Ticket <id> resolved: <resolution>` |
| `Resolved`, blank resolution | `Ticket <id> resolved (no resolution text)` |
| `Closed` | `Ticket <id> closed` |

Rules that make this exercise worth doing:

- Use a **switch expression** over `TicketEvent`.
- Do **not** add a `default` branch. The compiler must be the one checking completeness.
- Take the record components apart in the `case` label.
- A guard (`when`) handles the blank-resolution case, so the order of the two `Resolved` cases matters.

```bash
mvn test -Dtest='EventPatternsTest#describes*'
```

**Expected:**

```text
[INFO] Tests run: 5, Failures: 0, Errors: 0, Skipped: 0
```

## 4. Play around

After the tests are green:

1. Add a fifth record, `Reopened(long ticketId, String reason)`, inside `TicketEvent`. Compile with `mvn -q compile`. Which file and line does the error point to, and what does it say?
2. Add a `default -> "Unknown event"` branch to `describe`, then compile again. What changes in the build, and what would a user see for a `Reopened` event?
3. Remove the `default` and the `Reopened` record again before you continue.

## Try it

```bash
mvn test -Dtest=EventPatternsTest
```

**Expected:**

```text
[INFO] Tests run: 7, Failures: 0, Errors: 0, Skipped: 0
```

Then the demo:

```bash
mvn -q compile
java -cp target/classes com.helpdeskpro.lab.patternmatching.PatternMatchingDemo
```

**Expected:**

```text
--- instanceof pattern (provided) ---
opened ticket 101 titled Printer jam
--- assigneeOf (TODO 2.1) ---
Opened -> Optional.empty
Assigned -> Optional[agent-1]
Resolved -> Optional.empty
Resolved -> Optional.empty
Closed -> Optional.empty
--- describe (TODO 2.2) ---
Ticket 101 opened: Printer jam
Ticket 101 assigned to agent-1
Ticket 101 resolved (no resolution text)
Ticket 102 resolved: Replaced the toner
Ticket 101 closed
```

## Checkpoint

This is `EventPatterns.java` as provided, with TODOs 2.1 and 2.2 open. Your version fills in the two method bodies.

<details>
<summary>Full <code>EventPatterns.java</code></summary>

```java
package com.helpdeskpro.lab.patternmatching;

import com.helpdeskpro.lab.sealedtypes.TicketEvent;
import java.util.Optional;

/**
 * Pattern matching over the sealed {@link TicketEvent} hierarchy.
 */
public final class EventPatterns {

    private EventPatterns() {
    }

    // ---------------------------------------------------------------------
    // TODO 2.1: read a component with pattern matching for instanceof
    //
    // Return the agent when the event is an Assigned event, and an empty
    // Optional for every other kind of event.
    //
    // Rule: use `instanceof` with a pattern variable. Do not cast by hand
    // and do not call getClass().
    //
    // Hints: instanceof TicketEvent.Assigned assigned, Optional.of,
    //        Optional.empty
    // ---------------------------------------------------------------------
    public static Optional<String> assigneeOf(TicketEvent event) {
        throw new UnsupportedOperationException("TODO 2.1: implement assigneeOf");
    }

    // ---------------------------------------------------------------------
    // TODO 2.2: describe an event with a pattern-matching switch
    //
    // Return a one-line description:
    //
    //   Opened                          -> "Ticket <id> opened: <title>"
    //   Assigned                        -> "Ticket <id> assigned to <agent>"
    //   Resolved, non-blank resolution  -> "Ticket <id> resolved: <resolution>"
    //   Resolved, blank resolution      -> "Ticket <id> resolved (no resolution text)"
    //   Closed                          -> "Ticket <id> closed"
    //
    // Rules:
    //  - use a switch expression over the sealed type
    //  - do NOT add a default branch; the compiler must be the one that
    //    checks every TicketEvent subtype is handled
    //  - take the record components apart in the case label
    //
    // Hints: record patterns, a `when` guard, String.isBlank()
    // ---------------------------------------------------------------------
    public static String describe(TicketEvent event) {
        throw new UnsupportedOperationException("TODO 2.2: implement describe");
    }
}
```

</details>

This matches [EventPatterns.java](../java-features-lab/src/main/java/com/helpdeskpro/lab/patternmatching/EventPatterns.java) exactly.

## Common mistakes

| Symptom | Cause | Fix |
|---|---|---|
| `this case label is dominated by a preceding case label` | The general `Resolved` case is listed before the guarded one | Put the case with the `when` guard first |
| `the switch expression does not cover all possible input values` | A subtype has no case and there is no `default` | Add the missing case. This is the check working |
| `describesResolvedWithBlankResolutionText` returns the `resolved:` form with nothing after the colon | The guard is missing, or tests the wrong variable | Use `isBlank()` on the resolution component in the guard |
| You added `default` and every test is green | `default` makes the switch complete without covering the cases | Remove it; the exercise is that the compiler does the checking |

Next: **[Step 5 — Switch expressions](05-switch-expressions.md)**.
