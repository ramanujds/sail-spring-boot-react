# Step 5 — Switch expressions

> [Back to index](README.md) · Previous: [Step 4 — Pattern matching](04-pattern-matching.md) · Next: [Step 6 — Text blocks](06-text-blocks.md)

**Package:** `switchexpressions` · **TODOs:** 3.1 and 3.2 · **Test:** `StatusLabelsTest`

## Goal

Map a ticket status to a label with a switch expression, and use a block with `yield` where one branch needs more than a single expression.

## Why this matters

Ticket status drives most of the behaviour in the application: what the UI shows, what transitions are legal, whether an SLA clock is running. All of that is a mapping from one enum to something else, and the Java 8 style `switch` statement has three problems that cause real bugs there: execution falls through to the next label unless you remember `break`, a variable must be declared outside and assigned inside, and the compiler cannot tell you that a case is missing. Most of those bugs are silent.

A switch *expression* produces a value, so the variable is assigned exactly once. Arrow labels do not fall through. And when the value being switched on is an `enum`, a switch with no `default` is checked for completeness: add a constant to `TicketStatus` and every incomplete switch becomes a compile error. It is the same safety net you used with the sealed type in Step 4.

## 1. Read the provided version

Open `StatusLabels.java`. `labelJava8Style` is provided with `case`, `break`, and a `default` that throws. Notice that the `default` branch is the only thing guarding against a missing constant, and the compiler does not enforce it.

`TicketStatus.java` is the enum both versions switch on:

<details>
<summary>Full <code>TicketStatus.java</code></summary>

```java
package com.helpdeskpro.lab.domain;

/**
 * The ticket lifecycle states used throughout the programme.
 *
 * Lifecycle: OPEN -> ASSIGNED -> IN_PROGRESS -> RESOLVED -> CLOSED,
 * with REOPENED branching off CLOSED and looping back to ASSIGNED.
 */
public enum TicketStatus {
    OPEN,
    ASSIGNED,
    IN_PROGRESS,
    RESOLVED,
    CLOSED,
    REOPENED
}
```

</details>

## 2. Run the demo

```bash
mvn -q compile
java -cp target/classes com.helpdeskpro.lab.switchexpressions.SwitchExpressionsDemo
```

**Expected:**

```text
--- Java 8 style labels ---
OPEN -> Waiting
ASSIGNED -> In progress
IN_PROGRESS -> In progress
RESOLVED -> Resolved
CLOSED -> Closed
REOPENED -> Waiting
--- switch expression labels (TODO 3.1) ---
TODO 3.1: implement label
--- SLA messages (TODO 3.2) ---
TODO 3.2: implement slaMessage
```

## 3. TODO 3.1: `label`

The contract is the same mapping as `labelJava8Style`:

| Status | Label |
|---|---|
| `OPEN`, `REOPENED` | `Waiting` |
| `ASSIGNED`, `IN_PROGRESS` | `In progress` |
| `RESOLVED` | `Resolved` |
| `CLOSED` | `Closed` |

Use one switch expression with arrow labels, several constants per label, and **no** `default`.

## 4. TODO 3.2: `slaMessage`

Some branches need a decision before they can produce a value. A single expression cannot hold that, so you use a block and `yield` the result.

| Status | `hoursLeft` | Result |
|---|---|---|
| `ASSIGNED` or `IN_PROGRESS` | below 0 | `Overdue by <n> hours`, where `n` is positive |
| `ASSIGNED` or `IN_PROGRESS` | 0 or more | `<n> hours left` |
| any other status | any | `No SLA clock running` |

For example, `(IN_PROGRESS, -3)` returns `Overdue by 3 hours`. Put the overdue-or-left decision in a block that ends with `yield`.

## 5. Play around

After the tests are green:

1. Add a constant `ARCHIVED` to `TicketStatus`. Compile. Which methods fail to compile and which do not? What explains the difference?
2. Remove the constant again.
3. In `labelJava8Style`, delete one `break` and run the demo. Which statuses now print the wrong label, and does the compiler warn you?
4. Restore the `break`.

## Try it

```bash
mvn test -Dtest=StatusLabelsTest
```

**Expected:**

```text
[INFO] Tests run: 4, Failures: 0, Errors: 0, Skipped: 0
```

Then the demo:

```bash
mvn -q compile
java -cp target/classes com.helpdeskpro.lab.switchexpressions.SwitchExpressionsDemo
```

**Expected:** the two sections that were TODO messages now show values.

```text
--- switch expression labels (TODO 3.1) ---
OPEN -> Waiting
ASSIGNED -> In progress
IN_PROGRESS -> In progress
RESOLVED -> Resolved
CLOSED -> Closed
REOPENED -> Waiting
--- SLA messages (TODO 3.2) ---
5 hours left
Overdue by 3 hours
No SLA clock running
```

## Checkpoint

This is `StatusLabels.java` as provided, with TODOs 3.1 and 3.2 open.

<details>
<summary>Full <code>StatusLabels.java</code></summary>

```java
package com.helpdeskpro.lab.switchexpressions;

import com.helpdeskpro.lab.domain.TicketStatus;

/**
 * Switch expressions over the ticket status.
 */
public final class StatusLabels {

    private StatusLabels() {
    }

    // Provided: the Java 8 style version, for comparison with TODO 3.1.
    public static String labelJava8Style(TicketStatus status) {
        String label;
        switch (status) {
            case OPEN:
            case REOPENED:
                label = "Waiting";
                break;
            case ASSIGNED:
            case IN_PROGRESS:
                label = "In progress";
                break;
            case RESOLVED:
                label = "Resolved";
                break;
            case CLOSED:
                label = "Closed";
                break;
            default:
                throw new IllegalArgumentException("Unknown status: " + status);
        }
        return label;
    }

    // ---------------------------------------------------------------------
    // TODO 3.1: the same mapping as a switch expression
    //
    //   OPEN, REOPENED          -> "Waiting"
    //   ASSIGNED, IN_PROGRESS   -> "In progress"
    //   RESOLVED                -> "Resolved"
    //   CLOSED                  -> "Closed"
    //
    // Rules:
    //  - one switch expression, arrow labels, several constants per label
    //  - no default branch. Because TicketStatus is an enum, the compiler
    //    reports an error if a constant is missing.
    // ---------------------------------------------------------------------
    public static String label(TicketStatus status) {
        throw new UnsupportedOperationException("TODO 3.1: implement label");
    }

    // ---------------------------------------------------------------------
    // TODO 3.2: a switch branch that needs a block and `yield`
    //
    // Describe the SLA position of a ticket:
    //
    //   ASSIGNED or IN_PROGRESS, hoursLeft < 0   -> "Overdue by <n> hours"
    //                                               (n is the positive number)
    //   ASSIGNED or IN_PROGRESS, hoursLeft >= 0  -> "<n> hours left"
    //   any other status                         -> "No SLA clock running"
    //
    // Example: (IN_PROGRESS, -3) -> "Overdue by 3 hours"
    //
    // Rule: use a switch expression, and put the overdue/left decision in a
    // block that ends with `yield`.
    // ---------------------------------------------------------------------
    public static String slaMessage(TicketStatus status, int hoursLeft) {
        throw new UnsupportedOperationException("TODO 3.2: implement slaMessage");
    }
}
```

</details>

This matches [StatusLabels.java](../java-features-lab/src/main/java/com/helpdeskpro/lab/switchexpressions/StatusLabels.java) exactly.

## Common mistakes

| Symptom | Cause | Fix |
|---|---|---|
| `switch rule completes without providing a value` | A block ends without `yield` | End every path of the block with `yield` |
| `attempt to return out of a switch expression` | You used `return` inside a branch block | Use `yield` to produce the branch value |
| `the switch expression does not cover all possible input values` | A constant is missing from `label` | Add the constant to a label. This is the check working |
| `labelMatchesTheJava8StyleVersionForEveryStatus` fails for `REOPENED` | `REOPENED` was mapped to a different label than `OPEN` | Group them under the same label |

Next: **[Step 6 — Text blocks](06-text-blocks.md)**.
