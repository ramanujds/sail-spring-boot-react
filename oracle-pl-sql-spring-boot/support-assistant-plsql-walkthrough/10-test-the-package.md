# Step 10 — Test the package

> [Back to index](README.md) · Previous: [Several calls, all or none](09-transactions.md) · Next: [Recap and exercises](11-recap-and-exercises.md)

## Goal

Four tests that call the real package and leave the tables as they were.

## Why this matters

A test against the real package catches a change in a rule, or a broken package body, before a user does. `@Transactional` on a test class rolls each test back, so the tests do not leave tickets behind. A sequence number is not given back by a rollback, so ids still move forward.

### 10.1 The test class

`@SpringBootTest` starts the application. `@Transactional` rolls each test back. The repository is the class under test:

```java
@SpringBootTest
@Transactional
class TicketPackageTests {

    @Autowired
    private TicketRepository ticketRepository;
```

### 10.2 One test

```java
@Test
void aTicketCannotSkipAStep() {
    long id = ticketRepository.createTicket("Reset my password", null, "LOW");

    assertThatThrownBy(() -> ticketRepository.changeStatus(id, "CLOSED"))
            .isInstanceOf(UncategorizedSQLException.class)
            .hasMessageContaining("ORA-20004");
}
```

### 10.3 The other three

| Test | Checks |
|---|---|
| `createTicketGivesBackAnIdAndStartsOpen` | The new id is in the `OPEN` list |
| `openCountGoesUpByOne` | `openCount` is one higher after a create |
| `highPriorityIsDueInOneDay` | `dueBy` is `createdAt` plus one day |

## Try it

Set `DB_USERNAME` and `DB_PASSWORD`, then:

```bash
mvn clean verify
```

Expected:

```text
Tests run: 4, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

The database must be running, with the package from step 2.

## Checkpoint

`src/test/java/com/bsp/supportassistant/TicketPackageTests.java` - Matches [../support-assistant-plsql/src/test/java/com/bsp/supportassistant/TicketPackageTests.java](../support-assistant-plsql/src/test/java/com/bsp/supportassistant/TicketPackageTests.java) exactly.

<details>
<summary>Full <code>src/test/java/com/bsp/supportassistant/TicketPackageTests.java</code></summary>

```java
package com.bsp.supportassistant;

import com.bsp.supportassistant.dto.TicketResponse;
import com.bsp.supportassistant.repository.TicketRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.UncategorizedSQLException;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// These tests call the real package in your Oracle database.
// @Transactional on a test class rolls each test back, so the tables stay as they were.
@SpringBootTest
@Transactional
class TicketPackageTests {

    @Autowired
    private TicketRepository ticketRepository;

    @Test
    void createTicketGivesBackAnIdAndStartsOpen() {
        long id = ticketRepository.createTicket("Printer is jammed", "Second floor", "HIGH");

        List<TicketResponse> open = ticketRepository.findTickets("OPEN");

        assertThat(open).extracting(TicketResponse::id).contains(id);
    }

    @Test
    void openCountGoesUpByOne() {
        int before = ticketRepository.openCount("MEDIUM");

        ticketRepository.createTicket("VPN drops every hour", null, "MEDIUM");

        assertThat(ticketRepository.openCount("MEDIUM")).isEqualTo(before + 1);
    }

    @Test
    void highPriorityIsDueInOneDay() {
        long id = ticketRepository.createTicket("Server room is hot", null, "HIGH");

        TicketResponse ticket = ticketRepository.findTickets(null).stream()
                .filter(t -> t.id() == id)
                .findFirst()
                .orElseThrow();

        assertThat(Duration.between(ticket.createdAt(), ticket.dueBy())).isEqualTo(Duration.ofDays(1));
    }

    @Test
    void aTicketCannotSkipAStep() {
        long id = ticketRepository.createTicket("Reset my password", null, "LOW");

        assertThatThrownBy(() -> ticketRepository.changeStatus(id, "CLOSED"))
                .isInstanceOf(UncategorizedSQLException.class)
                .hasMessageContaining("ORA-20004");
    }
}
```

</details>

## Common mistakes

| Symptom | Cause | Fix |
|---|---|---|
| Tests fail with `ORA-01017` | `DB_USERNAME` or `DB_PASSWORD` is not set in the terminal that runs Maven | Export both variables, then run `mvn clean verify` again |
| Tests fail with `ORA-12541: Cannot connect. No listener` | The database is not running, or `DB_URL` is wrong | Start the database. Check the port |

Next: **[Recap and exercises](11-recap-and-exercises.md)**.
