# Step 8 — Recap and exercises

> [Back to index](README.md) · Previous: [Step 7 — Virtual threads](07-virtual-threads.md)

## Check your result

Run the full suite and every demo:

```bash
mvn test
```

**Expected:**

```text
[INFO] Tests run: 22, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

None of the demos prints a line starting with `TODO`.

## Quick reference

| Concept | Where it lives | Where you use it next |
|---|---|---|
| Record, compact constructor | `records.TicketSummary` (TODO 1.1) | Request and response DTOs |
| `Stream.toList()` | `records.TicketSummaryMapper` (TODO 1.2) | Mapping entities to DTO lists in services |
| Sealed interface | `sealedtypes.TicketEvent` | The closed set of ticket events |
| Pattern matching for `instanceof` | `patternmatching.EventPatterns.assigneeOf` (TODO 2.1) | Reading a value out of a typed event |
| Record patterns, `when` guard, no-`default` switch | `patternmatching.EventPatterns.describe` (TODO 2.2) | Handling events in the service layer |
| Switch expression, enum completeness | `switchexpressions.StatusLabels.label` (TODO 3.1) | Mapping status to labels and rules |
| `yield` | `switchexpressions.StatusLabels.slaMessage` (TODO 3.2) | Rules that need a decision per case |
| Text block | `textblocks.TicketQueries` (TODO 4.1) | JPQL and SQL in `@Query` |
| Virtual-thread executor | `virtualthreads.VirtualThreadsLab` (TODO 5.1) | `spring.threads.virtual.enabled=true` |

## Gotchas

| Gotcha | What happens | What to do |
|---|---|---|
| A record is not a JPA entity | Entities need a no-argument constructor and mutable state | Use records for DTOs and value objects |
| Records holding mutable objects | `equals` and `hashCode` use whatever the component does, and a mutable component can still change | Keep record components immutable, or copy them in the constructor |
| `Stream.toList()` returns an unmodifiable list | `add` throws `UnsupportedOperationException` | Copy into an `ArrayList` when callers must modify it |
| `default` in a sealed or enum switch | It hides new cases from the compiler | Leave it out when you can cover every case |
| Case order in pattern switches | A general case before a specific one does not compile | List the most specific cases first |
| Text-block indentation | The closing delimiter decides what is stripped | Check the output with `System.out.print` |
| Virtual threads and CPU work | No speed-up for compute-bound tasks | Use them for tasks that wait on I/O |
| Virtual threads and pools | Pooling virtual threads defeats the purpose | Create one per task and let the JVM schedule them |

## Discussion questions

1. Why should a controller return a `TicketSummary`-style record and not the entity it was loaded from?
2. Why does removing `default` from a switch over a sealed type make the code safer rather than less complete?
3. A record gives you `equals`, `hashCode`, and `toString` for free. What risk does that introduce if one component is a mutable object?
4. In what situation would 500 tasks on 20 platform threads and 500 tasks on virtual threads take about the same time?
5. What does the compiler tell you when you add `Reopened` to the sealed interface, and why is that better than a failing test?

## Exercises

Ordered from easiest to hardest. Write a test for each one.

1. **A new status rule.** Add a method `isTerminal(TicketStatus)` to `StatusLabels` that returns `true` for `CLOSED` only, using a switch expression with no `default`. Write the test first.
2. **A new event.** Add `Reopened(long ticketId, String reason)` to `TicketEvent`. Let the compiler list every `switch` you must update, fix them all, and add `EventPatternsTest` cases.
3. **Another DTO.** Add a record `AgentWorkload(String agent, int openTickets)` with a compact constructor that rejects a blank agent and a negative count. Write the tests first, then implement.
4. **Oracle query text.** Add a `topOpenTickets` method to `TicketQueries` that returns a native Oracle SQL text block ending with `FETCH FIRST 10 ROWS ONLY`. Write a test that checks its lines.
5. **Measure it.** Extend `VirtualThreadsLab.main` to print a table of elapsed time for pool sizes 20, 200, and 2000, and for virtual threads, with 5,000 tasks. Record the numbers and explain the shape of them.
6. **From memory.** Delete your implementation of `EventPatterns.describe` and write it again without looking, then compare the two versions and the tests. Which parts did you get wrong the second time, and what does that show you about which parts you understood?

## What's next

You have used each feature in isolation. The next stages of the [Session 1 README](../../README.md) put them to work in the `helpdesk-pro` Spring Boot project: records become DTOs, the sealed interface models ticket events, the pattern-matching `switch` handles ticket status, text blocks hold the JPQL, and virtual threads are switched on for blocking JDBC calls.
