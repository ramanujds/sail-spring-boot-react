# Step 11 — Test the relationship

> [Back to index](README.md) · Previous: [Transactions](10-transactions.md) · Next: [Recap and exercises](12-recap-and-exercises.md)

## Goal

Six tests that call the real package: five that roll back, and one that proves a failed transaction leaves nothing behind.

## Why this matters

Each relationship rule can break on its own: a comment that does not come back with its ticket, a ticket that vanishes from the joined list, a closed ticket that still takes comments. A test for each keeps them working when you change the package or the mapping. The tests call the real Oracle package, because the rules are in the package.

### 11.1 Tests that roll back

`@Transactional` on a test class undoes each test when it ends, so the tables stay as they were. `TicketRelationTests` has five:

| Test | What it proves |
|---|---|
| `commentsComeBackWithTheirTicketInTheOrderTheyWereAdded` | Two cursors in one call: the ticket and its comments, oldest first |
| `aTicketWithoutCommentsHasAnEmptyList` | An empty list, not `null` |
| `theJoinedListGroupsCommentsAndKeepsTicketsWithoutComments` | The join and the grouping: each ticket once, and the quiet ticket still there |
| `aClosedTicketTakesNoComments` | The parent's state forbids a child: `ORA-20006` |
| `aCommentNeedsAnExistingTicket` | The parent must exist: `ORA-20003` |

One of them, in full:

```java
@Test
void aClosedTicketTakesNoComments() {
    long ticketId = ticketRepository.createTicket("Reset my password", null, "LOW");
    ticketRepository.changeStatus(ticketId, "IN_PROGRESS");
    ticketRepository.changeStatus(ticketId, "RESOLVED");
    ticketRepository.changeStatus(ticketId, "CLOSED");

    assertThatThrownBy(() -> ticketRepository.addComment(ticketId, "Meera", "Thanks"))
            .isInstanceOf(UncategorizedSQLException.class)
            .hasMessageContaining("ORA-20006");
}
```

### 11.2 A test that must not roll back by itself

The service method of step 10 opens its own transaction. If the test class were `@Transactional` too, the service would join the test's transaction, and the "undone" ticket would still be visible to the test until the very end. So `TicketTransactionTests` is **not** `@Transactional`:

```java
@Test
void aRefusedFirstCommentLeavesNoTicketBehind() {
    String title = "Atomic test " + UUID.randomUUID();
    CreateTicketWithCommentRequest request =
            new CreateTicketWithCommentRequest(title, null, "LOW", "Meera", null);

    assertThatThrownBy(() -> ticketService.createWithFirstComment(request))
            .isInstanceOf(UncategorizedSQLException.class)
            .hasMessageContaining("ORA-20005");

    assertThat(ticketRepository.findTickets(null))
            .extracting(TicketResponse::title)
            .doesNotContain(title);
}
```

The title has a random part, so no other ticket can have it. The test creates nothing that stays: the failed transaction is undone by the service itself.

## Try it

Set `DB_USERNAME` and `DB_PASSWORD`, and make sure the scripts have run. Then:

```bash
mvn clean verify
```

Expected:

```text
[INFO] Tests run: 5, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: ... -- in com.bsp.supportassistant.TicketRelationTests
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: ... -- in com.bsp.supportassistant.TicketTransactionTests
[INFO] Tests run: 6, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

Prove that the second test guards something. In `TicketService`, put a `//` in front of `@Transactional` on `createWithFirstComment`, and run `mvn clean test -Dtest=TicketTransactionTests`. The test fails, because the ticket stays. Remove the `//` again.

## Checkpoint

`src/test/java/com/bsp/supportassistant/TicketRelationTests.java` - Matches [../support-assistant-plsql-relations/src/test/java/com/bsp/supportassistant/TicketRelationTests.java](../support-assistant-plsql-relations/src/test/java/com/bsp/supportassistant/TicketRelationTests.java) exactly.

<details>
<summary>Full <code>src/test/java/com/bsp/supportassistant/TicketRelationTests.java</code></summary>

```java
package com.bsp.supportassistant;

import com.bsp.supportassistant.dto.CommentResponse;
import com.bsp.supportassistant.dto.TicketWithComments;
import com.bsp.supportassistant.repository.TicketRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.UncategorizedSQLException;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// These tests call the real package in your Oracle database.
// @Transactional on a test class rolls each test back, so the tables stay as they were.
@SpringBootTest
@Transactional
class TicketRelationTests {

    @Autowired
    private TicketRepository ticketRepository;

    @Test
    void commentsComeBackWithTheirTicketInTheOrderTheyWereAdded() {
        long ticketId = ticketRepository.createTicket("Printer is jammed", null, "HIGH");
        ticketRepository.addComment(ticketId, "Meera", "Paper tray is stuck");
        ticketRepository.addComment(ticketId, "Karan", "On my way");

        TicketWithComments result = ticketRepository.getTicket(ticketId);

        assertThat(result.ticket().id()).isEqualTo(ticketId);
        assertThat(result.comments()).extracting(CommentResponse::author).containsExactly("Meera", "Karan");
    }

    @Test
    void aTicketWithoutCommentsHasAnEmptyList() {
        long ticketId = ticketRepository.createTicket("VPN drops every hour", null, "MEDIUM");

        assertThat(ticketRepository.getTicket(ticketId).comments()).isEmpty();
    }

    @Test
    void theJoinedListGroupsCommentsAndKeepsTicketsWithoutComments() {
        long quiet = ticketRepository.createTicket("No comments here", null, "LOW");
        long busy = ticketRepository.createTicket("Two comments here", null, "LOW");
        ticketRepository.addComment(busy, "Meera", "First");
        ticketRepository.addComment(busy, "Karan", "Second");

        List<TicketWithComments> all = ticketRepository.findTicketsWithComments(null);

        assertThat(all).extracting(t -> t.ticket().id()).containsOnlyOnce(quiet, busy);
        assertThat(find(all, quiet).comments()).isEmpty();
        assertThat(find(all, busy).comments()).extracting(CommentResponse::body).containsExactly("First", "Second");
    }

    @Test
    void aClosedTicketTakesNoComments() {
        long ticketId = ticketRepository.createTicket("Reset my password", null, "LOW");
        ticketRepository.changeStatus(ticketId, "IN_PROGRESS");
        ticketRepository.changeStatus(ticketId, "RESOLVED");
        ticketRepository.changeStatus(ticketId, "CLOSED");

        assertThatThrownBy(() -> ticketRepository.addComment(ticketId, "Meera", "Thanks"))
                .isInstanceOf(UncategorizedSQLException.class)
                .hasMessageContaining("ORA-20006");
    }

    @Test
    void aCommentNeedsAnExistingTicket() {
        assertThatThrownBy(() -> ticketRepository.addComment(999_999, "Meera", "Hello"))
                .isInstanceOf(UncategorizedSQLException.class)
                .hasMessageContaining("ORA-20003");
    }

    private TicketWithComments find(List<TicketWithComments> tickets, long ticketId) {
        return tickets.stream().filter(t -> t.ticket().id() == ticketId).findFirst().orElseThrow();
    }
}
```

</details>

`src/test/java/com/bsp/supportassistant/TicketTransactionTests.java` - Matches [../support-assistant-plsql-relations/src/test/java/com/bsp/supportassistant/TicketTransactionTests.java](../support-assistant-plsql-relations/src/test/java/com/bsp/supportassistant/TicketTransactionTests.java) exactly.

<details>
<summary>Full <code>src/test/java/com/bsp/supportassistant/TicketTransactionTests.java</code></summary>

```java
package com.bsp.supportassistant;

import com.bsp.supportassistant.dto.CreateTicketWithCommentRequest;
import com.bsp.supportassistant.dto.TicketResponse;
import com.bsp.supportassistant.repository.TicketRepository;
import com.bsp.supportassistant.service.TicketService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.UncategorizedSQLException;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// This class is NOT @Transactional on purpose. The service opens its own transaction,
// and the test checks what is left in the database after that transaction is undone.
@SpringBootTest
class TicketTransactionTests {

    @Autowired
    private TicketService ticketService;

    @Autowired
    private TicketRepository ticketRepository;

    @Test
    void aRefusedFirstCommentLeavesNoTicketBehind() {
        String title = "Atomic test " + UUID.randomUUID();
        CreateTicketWithCommentRequest request =
                new CreateTicketWithCommentRequest(title, null, "LOW", "Meera", null);

        assertThatThrownBy(() -> ticketService.createWithFirstComment(request))
                .isInstanceOf(UncategorizedSQLException.class)
                .hasMessageContaining("ORA-20005");

        assertThat(ticketRepository.findTickets(null))
                .extracting(TicketResponse::title)
                .doesNotContain(title);
    }
}
```

</details>

## Common mistakes

| Symptom | Cause | Fix |
|---|---|---|
| `ORA-01017` in every test | `DB_USERNAME` or `DB_PASSWORD` is not set in the terminal that runs Maven | `export` both, then run `mvn` again |
| `ORA-04063: package body "...DESK_TICKET_PKG" has errors` | The package body did not compile | `SHOW ERRORS PACKAGE BODY DESK_TICKET_PKG` |
| `aRefusedFirstCommentLeavesNoTicketBehind` fails although `@Transactional` is on the service | `@Transactional` was added to the test class too | Take it off the test class. The service must own the transaction |
| A test passes alone and fails with the others | A test relies on ids or rows that another test made | Create the rows inside the test, and look them up by the id that the call gave back |

Next: **[Recap and exercises](12-recap-and-exercises.md)**.
