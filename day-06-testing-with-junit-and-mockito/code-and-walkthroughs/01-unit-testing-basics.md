# Unit testing: the basics

So far you checked your work by starting the application and sending requests. That works, and it is slow, and you repeat it after every change. A **test** is a small program that calls your code and checks the answer, so the computer repeats the check for you in a fraction of a second.

**Starting point:** your project at the end of [Day 5, part 5](../../day-05-transactions-plsql-pagination-and-specifications/code-and-walkthroughs/05-specifications.md).
**Project:** [support-assistant-jpa](support-assistant-jpa/) in this folder. It is the Day 5 project. Day 6 adds only test classes, under `src/test/java`. The application code does not change.

## What you will build

- A test class for the SLA rules (`SlaPolicyTest`).
- A test class for the mapper (`TicketMapperTest`).
- The habit: write a test, run it, see it pass, break the code, see it fail.

---

## 1. The idea

A **unit test** checks one small piece of code, alone. No database, no web server, no other classes that you did not mean to test. It runs in milliseconds.

Every test has three parts, called **arrange, act, assert**:

| Part | What it does | Example |
|---|---|---|
| **Arrange** | Prepare the input | A ticket created at 9:00, with priority `HIGH` |
| **Act** | Call the code | `SlaPolicy.dueBy(Priority.HIGH, created)` |
| **Assert** | Check the result | It is 9:00 the next day |

`SlaPolicy` from [Day 5, part 2](../../day-05-transactions-plsql-pagination-and-specifications/code-and-walkthroughs/02-sla-logic.md) is a good first target: plain Java, no database, and the time is a parameter, not read from the clock.

---

## Build steps

### Step 1: a check by hand

The simplest test is a `main` method that prints what it finds. A learner could write:

```java
public class SlaCheck {

    public static void main(String[] args) {
        LocalDateTime created = LocalDateTime.of(2026, 10, 14, 9, 0);

        LocalDateTime dueBy = SlaPolicy.dueBy(Priority.HIGH, created);

        System.out.println("expected 2026-10-15T09:00, got " + dueBy);
    }
}
```

Running it prints:

```text
expected 2026-10-15T09:00, got 2026-10-15T09:00
```

It works, and it has three problems. A person has to read the line and compare. There is one such class for every check. And nothing says "all checks passed". A **test framework** solves all three.

### Step 2: the same check with JUnit

**JUnit** is the test framework for Java. It is already in your project: the test starter in `pom.xml` brings it, together with Mockito and the other test libraries.

Create the class in `src/test/java`, in the same package as the class you test, with the name of that class and `Test` at the end:

```text
src/test/java/com/bsp/supportassistant/service/SlaPolicyTest.java
```

```java
class SlaPolicyTest {

    private final LocalDateTime created = LocalDateTime.of(2026, 10, 14, 9, 0);

    @Test
    void highPriorityIsDueInOneDay() {
        LocalDateTime dueBy = SlaPolicy.dueBy(Priority.HIGH, created);

        assertEquals(LocalDateTime.of(2026, 10, 15, 9, 0), dueBy);
    }
}
```

| Part | Meaning |
|---|---|
| `@Test` | JUnit runs this method as a test |
| The method name | A sentence that says what must be true. It is what you read when the test fails |
| `assertEquals(expected, actual)` | The test fails if the two values are not equal. The **expected** value comes first |

The imports are `org.junit.jupiter.api.Test` and `static org.junit.jupiter.api.Assertions.assertEquals`. The class and the method need no `public`.

Run it. In IntelliJ, click the green triangle next to the class name. From the project folder:

```bash
mvn -q test -Dtest=SlaPolicyTest
```

**Check:** IntelliJ shows a green bar with `1 test passed`. Maven prints nothing with `-q` when every test passes. Run it without `-q` to see `Tests run: 1, Failures: 0, Errors: 0`.

### Step 3: more cases

A rule has more than one case. Add a test for each:

```java
@Test
void mediumPriorityIsDueInThreeDays() {
    assertEquals(created.plusDays(3), SlaPolicy.dueBy(Priority.MEDIUM, created));
}

@Test
void lowPriorityIsDueInSevenDays() {
    assertEquals(created.plusDays(7), SlaPolicy.dueBy(Priority.LOW, created));
}
```

Then the overdue rule. A helper at the bottom of the class builds a ticket:

```java
private Ticket ticket(TicketStatus status, LocalDateTime dueBy) {
    Ticket ticket = new Ticket();
    ticket.setStatus(status);
    ticket.setDueBy(dueBy);
    return ticket;
}
```

```java
@Test
void openTicketPastItsDueDateIsOverdue() {
    Ticket ticket = ticket(TicketStatus.OPEN, created.plusDays(1));

    assertTrue(SlaPolicy.isOverdue(ticket, created.plusDays(2)));
}

@Test
void openTicketBeforeItsDueDateIsNotOverdue() {
    Ticket ticket = ticket(TicketStatus.OPEN, created.plusDays(1));

    assertFalse(SlaPolicy.isOverdue(ticket, created.plusHours(5)));
}

@Test
void closedTicketIsNeverOverdue() {
    Ticket ticket = ticket(TicketStatus.CLOSED, created.plusDays(1));

    assertFalse(SlaPolicy.isOverdue(ticket, created.plusDays(30)));
}
```

`assertTrue` and `assertFalse` check a condition.

Each test sets "now" by passing a time to `isOverdue`. No test reads the clock, so each gives the same answer every time you run it.

**Check:** run the class. You should see `6 tests passed`.

### Step 4: see a test fail

A test is only useful if it fails when the code is wrong. Break the rule on purpose. In `SlaPolicy.dueBy`, change the `HIGH` line to `plusDays(2)`, and run the class again.

**Check:** one test fails, and the message says which and why:

```text
SlaPolicyTest.highPriorityIsDueInOneDay
org.opentest4j.AssertionFailedError: expected: <2026-10-15T09:00> but was: <2026-10-16T09:00>
```

The method name tells you the rule that broke. The message tells you the two values. Change the line back to `plusDays(1)`, and run again: six green tests.

### Step 5: test an object

`TicketMapper` is also plain Java. Create `TicketMapperTest` in `src/test/java/com/bsp/supportassistant/mapper`:

```java
class TicketMapperTest {

    @Test
    void applyChangesCopiesOnlyTheFieldsThatWereSent() {
        Ticket ticket = new Ticket();
        ticket.setTitle("Printer is offline");
        ticket.setDescription("Floor 2");
        ticket.setPriority(Priority.LOW);

        TicketRequest request = new TicketRequest(null, null, null, Priority.HIGH);
        TicketMapper.applyChanges(ticket, request);

        assertEquals(Priority.HIGH, ticket.getPriority());        // sent, so changed
        assertEquals("Printer is offline", ticket.getTitle());     // not sent, so kept
        assertEquals("Floor 2", ticket.getDescription());
    }

    @Test
    void toResponseShowsAClosedTicketAsNotOverdue() {
        Ticket ticket = new Ticket();
        ticket.setStatus(TicketStatus.CLOSED);
        ticket.setDueBy(LocalDateTime.now().minusDays(5));

        assertFalse(TicketMapper.toResponse(ticket).overdue());
    }
}
```

The first test checks several fields. That is fine when they belong to one idea: "only the fields that were sent change". The complete file also has a test for an overdue ticket.

**Check:** run `TicketMapperTest`. You should see `3 tests passed`.

---

## What `mvn test` runs

`mvn test` and `mvn verify` run every test class, including `SupportAssistantApplicationTests` that the generator created. That test starts the whole application, so it needs the Oracle database. The new tests need nothing.

To run all of your tests without the database test:

```bash
mvn test -Dtest='!SupportAssistantApplicationTests'
```

`!` means "except".

---

## Common mistakes

| Symptom | Cause | Fix |
|---|---|---|
| `expected: <2026-10-16T09:00> but was: <2026-10-15T09:00>`, and the code is right | The arguments are the wrong way round: `assertEquals(actual, expected)` | The expected value comes first |
| The test is not run, and the Maven output has no `Tests run` line for it | There is no `@Test`, or the class name does not end in `Test` | Add `@Test`. Name the class `SlaPolicyTest` |
| `cannot find symbol` on `import org.junit.Test` | That is JUnit 4. This project uses JUnit 5, the package `org.junit.jupiter.api` | Import `org.junit.jupiter.api.Test` |
| A test passes on Monday and fails on Tuesday | The test reads `LocalDateTime.now()` in the code that it tests, or compares with a fixed date that moved into the past | Pass the time in as a parameter, as `isOverdue` does |
| One failed test hides the others in the same method | A method has many unrelated assertions, and the first failure stops it | One idea per test method |

---

## Practice

1. Add a test that a `RESOLVED` ticket past its due date is not overdue. Run it.
2. In `SlaPolicy.isOverdue`, change `isBefore(now)` to `isAfter(now)`. Run the class. Write down which tests fail, and why those and not the others. Restore the line.
3. Write `TicketMapperTest.toEntityCopiesTheFieldsOfTheRequest`. Check the title, description, and priority of the `Ticket` that `TicketMapper.toEntity` returns.

---

## Recap

- A unit test calls one small piece of code and checks the result. It needs no database and no server.
- A test has three parts: arrange, act, assert. Name the method as a sentence.
- JUnit runs methods marked `@Test`. `assertEquals(expected, actual)`, `assertTrue`, and `assertFalse` do the checking.
- Pass the time in as a parameter. Code that reads the clock cannot be tested the same way twice.
- A test that never fails is not a test: break the code once to see it fail.

### State at end of this part

| File | What it holds |
|---|---|
| `src/test/.../service/SlaPolicyTest.java` | 6 tests of the SLA rules |
| `src/test/.../mapper/TicketMapperTest.java` | 3 tests of the mapper |
