# Step 6 — Test after the rollback

> [Back to index](README.md) · Previous: [A transaction of its own](05-a-transaction-of-its-own.md) · Next: [Recap and exercises](07-recap-and-exercises.md)

## Goal

`TicketTransactionTests`: two tests that check what the service's transaction left in the database.

## Why this matters

The tests of the first project are `@Transactional`, so each test runs inside one transaction that is rolled back at the end. Inside that test transaction, the service's `@Transactional` **joins** it, and nothing is committed or rolled back until the test ends. You cannot see the service's own rollback from there.

So this class is **not** `@Transactional`. The service starts and ends its own transaction, and the test reads the tables afterwards. The price: the rows that the tests create stay in the tables.

### 6.1 Ticket changes are put back

```java
@Test
void aRefusedBulkChangePutsEveryTicketBack() {
    long first = ticketRepository.createTicket("Transaction test: first", null, "LOW");
    long second = ticketRepository.createTicket("Transaction test: second", null, "LOW");
    ticketRepository.changeStatus(second, "IN_PROGRESS");

    assertThatThrownBy(() -> ticketService.changeStatus(List.of(first, second), "IN_PROGRESS"))
            .isInstanceOf(UncategorizedSQLException.class)
            .hasMessageContaining("ORA-20004");

    assertThat(statusOf(first)).isEqualTo("OPEN");
}
```

Without a test transaction, each repository call in the test commits on its own. So the two tickets, and the status of the second, are saved before the service runs.

### 6.2 The log line stays

```java
Integer lines = jdbcTemplate.queryForObject(
        "SELECT COUNT(*) FROM SUPPORT_ERROR_LOG WHERE MESSAGE = ?", Integer.class,
        "Bulk change to IN_PROGRESS stopped at ticket " + ticket);
assertThat(lines).isEqualTo(1);
```

`JdbcTemplate` reads the table directly. The message holds the new ticket's id, so the count finds only the line of this test run.

## Try it

```bash
mvn clean verify
```

Expected:

```text
[INFO] Tests run: 4, Failures: 0, Errors: 0, Skipped: 0, ... in com.bsp.supportassistant.TicketPackageTests
[INFO] Tests run: 2, Failures: 0, Errors: 0, Skipped: 0, ... in com.bsp.supportassistant.TicketTransactionTests
[INFO] Tests run: 6, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

Prove that the tests catch a mistake. Change `REQUIRES_NEW` in `ErrorLogService` back to a plain `@Transactional`, and run `mvn clean verify` again. Expected: `theErrorLineStaysAfterTheRollback` fails with `expected: 1` and `but was: 0`. Put `REQUIRES_NEW` back.

## Checkpoint

`src/test/java/com/bsp/supportassistant/TicketTransactionTests.java` - Matches [../support-assistant-plsql-transactions/src/test/java/com/bsp/supportassistant/TicketTransactionTests.java](../support-assistant-plsql-transactions/src/test/java/com/bsp/supportassistant/TicketTransactionTests.java) exactly.

<details>
<summary>Full <code>src/test/java/com/bsp/supportassistant/TicketTransactionTests.java</code></summary>

```java
package com.bsp.supportassistant;

import com.bsp.supportassistant.dto.TicketResponse;
import com.bsp.supportassistant.repository.TicketRepository;
import com.bsp.supportassistant.service.TicketService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.UncategorizedSQLException;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// This class is NOT @Transactional on purpose. The service opens its own transaction,
// and each test checks what is left in the database after that transaction ends.
// The tickets that the tests create stay in the table.
@SpringBootTest
class TicketTransactionTests {

    @Autowired
    private TicketService ticketService;

    @Autowired
    private TicketRepository ticketRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void aRefusedBulkChangePutsEveryTicketBack() {
        long first = ticketRepository.createTicket("Transaction test: first", null, "LOW");
        long second = ticketRepository.createTicket("Transaction test: second", null, "LOW");
        ticketRepository.changeStatus(second, "IN_PROGRESS");

        // The first ticket can move. The second is already IN_PROGRESS, so the package refuses it.
        assertThatThrownBy(() -> ticketService.changeStatus(List.of(first, second), "IN_PROGRESS"))
                .isInstanceOf(UncategorizedSQLException.class)
                .hasMessageContaining("ORA-20004");

        assertThat(statusOf(first)).isEqualTo("OPEN");
    }

    @Test
    void theErrorLineStaysAfterTheRollback() {
        long ticket = ticketRepository.createTicket("Transaction test: closed", null, "LOW");
        ticketRepository.changeStatus(ticket, "IN_PROGRESS");

        assertThatThrownBy(() -> ticketService.changeStatus(List.of(ticket), "IN_PROGRESS"))
                .isInstanceOf(UncategorizedSQLException.class);

        Integer lines = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM SUPPORT_ERROR_LOG WHERE MESSAGE = ?", Integer.class,
                "Bulk change to IN_PROGRESS stopped at ticket " + ticket);
        assertThat(lines).isEqualTo(1);
    }

    private String statusOf(long ticketId) {
        return ticketRepository.findTickets(null).stream()
                .filter(t -> t.id() == ticketId)
                .map(TicketResponse::status)
                .findFirst()
                .orElseThrow();
    }
}
```

</details>

## Common mistakes

| Symptom | Cause | Fix |
|---|---|---|
| `aRefusedBulkChangePutsEveryTicketBack` fails with `but was: "IN_PROGRESS"`, and the bulk method has `@Transactional` | The test class has `@Transactional`. The service joined the test's transaction, so nothing was rolled back before the test read the status | Remove `@Transactional` from this test class |
| `aRefusedBulkChangePutsEveryTicketBack` fails with `but was: "IN_PROGRESS"`, and the test class has no `@Transactional` | The bulk method has no `@Transactional` | Put it back |
| The tables fill up with `Transaction test` tickets | Each run of this class commits its rows | Expected. Delete them in SQL Developer when you want: first from `SUPPORT_TICKET_LOG`, then from `SUPPORT_TICKETS` |

Next: **[Recap and exercises](07-recap-and-exercises.md)**.
