# Step 6 — Text blocks

> [Back to index](README.md) · Previous: [Step 5 — Switch expressions](05-switch-expressions.md) · Next: [Step 7 — Virtual threads](07-virtual-threads.md)

**Package:** `textblocks` · **TODO:** 4.1 · **Test:** `TicketQueriesTest`

## Goal

Write a multi-line JPQL query as a text block that reads like the query it produces.

## Why this matters

A Spring Data repository method often carries its query in an annotation, and an application accumulates dozens of them. A query built by concatenation hides the query. Every line needs a closing quote, a `+`, and a trailing space so words do not run together. The missing space is a classic bug: `"... status = :status" + "AND ..."` produces `:statusAND`, and the error only shows up when the query runs.

A text block is the string, written as it is read. Quotes need no escaping, line breaks are real line breaks, and the compiler removes the indentation that only exists because of where the code sits. You can read the query in the source and see exactly what the database receives.

A text block is a compile-time constant, so it works inside an annotation. This is how you will write it on Day 2 and Day 5:

```java
@Query("""
        SELECT t FROM Ticket t
        WHERE t.status = :status
        ORDER BY t.dueBy
        """)
List<Ticket> findByStatus(@Param("status") TicketStatus status);
```

## 1. Read the provided version

Open `TicketQueries.java`. `byStatusAndPriorityConcatenated` builds the query with `+`. Count the places a space could be missing.

## 2. Run the demo

```bash
mvn -q compile
java -cp target/classes com.helpdeskpro.lab.textblocks.TextBlocksDemo
```

**Expected:**

```text
--- concatenated (provided) ---
SELECT t FROM Ticket t WHERE t.status = :status AND t.priority = :priority ORDER BY t.dueBy
--- text block (TODO 4.1) ---
TODO 4.1: implement byStatusAndPriority
```

## 3. TODO 4.1: `byStatusAndPriority`

The query must read as these four lines, with the `AND` line indented by two spaces and no indentation on the others:

```text
SELECT t FROM Ticket t
WHERE t.status = :status
  AND t.priority = :priority
ORDER BY t.dueBy
```

Rules:

- Use a text block (`"""`), not concatenation.
- The position of the **closing** `"""` decides how much indentation is stripped. Check your result against the expected lines above.
- A closing delimiter on its own line adds a trailing newline. The tests read the result line by line, so either form passes.

## 4. Play around

After the tests are green:

1. Move the closing `"""` one indent level to the left, then to the right. Run the test after each move. What happens to the indentation of every line?
2. Add a line `-- note` as a SQL comment inside the block. Does the query still compare equal to the concatenated one in the first test? Why or why not?
3. Print the result of `byStatusAndPriority()` with `System.out.print(...)`. Where does the text start and end on screen?
4. Restore the block before you continue.

## Try it

```bash
mvn test -Dtest=TicketQueriesTest
```

**Expected:**

```text
[INFO] Tests run: 2, Failures: 0, Errors: 0, Skipped: 0
```

Then the demo:

```bash
mvn -q compile
java -cp target/classes com.helpdeskpro.lab.textblocks.TextBlocksDemo
```

**Expected:** the second section now prints the query on four lines.

```text
--- text block (TODO 4.1) ---
SELECT t FROM Ticket t
WHERE t.status = :status
  AND t.priority = :priority
ORDER BY t.dueBy
```

## Checkpoint

This is `TicketQueries.java` as provided, with TODO 4.1 open.

<details>
<summary>Full <code>TicketQueries.java</code></summary>

```java
package com.helpdeskpro.lab.textblocks;

/**
 * Text blocks for multi-line JPQL and SQL.
 */
public final class TicketQueries {

    private TicketQueries() {
    }

    // Provided: the Java 8 style version, for comparison with TODO 4.1.
    public static String byStatusAndPriorityConcatenated() {
        return "SELECT t FROM Ticket t " +
               "WHERE t.status = :status " +
               "AND t.priority = :priority " +
               "ORDER BY t.dueBy";
    }

    // ---------------------------------------------------------------------
    // TODO 4.1: write the same JPQL query as a text block
    //
    // The query must read as these four lines, with the AND line indented by
    // two spaces and no indentation on the others:
    //
    //   SELECT t FROM Ticket t
    //   WHERE t.status = :status
    //     AND t.priority = :priority
    //   ORDER BY t.dueBy
    //
    // Rule: use a text block (""" ... """), not string concatenation. The
    // position of the closing delimiter decides how much indentation is
    // stripped, so check it against your source indentation.
    // ---------------------------------------------------------------------
    public static String byStatusAndPriority() {
        throw new UnsupportedOperationException("TODO 4.1: implement byStatusAndPriority");
    }
}
```

</details>

This matches [TicketQueries.java](../java-features-lab/src/main/java/com/helpdeskpro/lab/textblocks/TicketQueries.java) exactly.

## Common mistakes

| Symptom | Cause | Fix |
|---|---|---|
| `textBlockKeepsTheIntendedLineStructureAndIndentation` fails on the third line | The `AND` line has no extra indentation relative to the others | Indent it two spaces more than the lines around it |
| Every line starts with spaces in the output | The closing `"""` is further to the left than the text, or text is indented past it | Align the closing delimiter with the left edge of the text |
| The first test fails with the words joined together | A line break was escaped with `\` | Remove the backslash; a normal line break is already a separator |
| `illegal text block open delimiter sequence` | Text starts on the same line as the opening `"""` | Start the text on the next line |

Next: **[Step 7 — Virtual threads](07-virtual-threads.md)**.
