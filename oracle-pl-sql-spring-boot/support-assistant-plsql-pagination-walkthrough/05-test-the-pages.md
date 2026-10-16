# Step 5 — Test the pages

> [Back to index](README.md) · Previous: [The service and the controller](04-service-and-controller.md) · Next: [Recap and exercises](06-recap-and-exercises.md)

## Goal

`TicketPageTests`: four tests that call `findPage` against your database and roll back.

## Why this matters

The `curl` calls of step 4 depend on the 12 sample tickets. The tests must pass with whatever is in the table. So they check rules that are always true: a page is never larger than `size`, the pages together hold every ticket once, and they are in order.

### 5.1 Three tickets before each test

The class is `@Transactional`, so each test rolls back, as in the first project. A `@BeforeEach` method runs before every test and creates three tickets. There are then always at least two pages of 2:

```java
@BeforeEach
void createTickets() {
    ticketRepository.createTicket("Page test: high", null, "HIGH");
    ticketRepository.createTicket("Page test: low", null, "LOW");
    ticketRepository.createTicket("Page test: medium", null, "MEDIUM");
}
```

### 5.2 Walk through every page

```java
@Test
void thePagesTogetherHoldEveryTicketOnceInOrder() {
    List<TicketResponse> all = new ArrayList<>();
    PageResponse<TicketResponse> page = ticketService.findPage(null, 0, 2, "dueBy", "asc");
    for (int number = 0; number < page.totalPages(); number++) {
        all.addAll(ticketService.findPage(null, number, 2, "dueBy", "asc").content());
    }

    assertThat(all).hasSize((int) page.totalElements());
    assertThat(all).extracting(TicketResponse::id).doesNotHaveDuplicates();
    assertThat(all).extracting(TicketResponse::dueBy).isSortedAccordingTo(Comparator.naturalOrder());
}
```

| Check | Fails when |
|---|---|
| `hasSize(totalElements)` | The count and the page use a different `WHERE`, or a page skips rows |
| `doesNotHaveDuplicates()` | A ticket appears on two pages |
| `isSortedAccordingTo(...)` | The `ORDER BY` does not follow `p_sort` |

The other three tests check the size of one page and its total, an empty page after the last, and a refused sort. They are in the checkpoint.

## Try it

```bash
mvn clean verify
```

Expected:

```text
[INFO] Tests run: 4, Failures: 0, Errors: 0, Skipped: 0, ... in com.bsp.supportassistant.TicketPackageTests
[INFO] Tests run: 4, Failures: 0, Errors: 0, Skipped: 0, ... in com.bsp.supportassistant.TicketPageTests
[INFO] Tests run: 8, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

Run `SELECT COUNT(*) FROM SUPPORT_TICKETS;` before and after. The count is the same: every test rolled back.

## Checkpoint

`src/test/java/com/bsp/supportassistant/TicketPageTests.java` - Matches [../support-assistant-plsql-pagination/src/test/java/com/bsp/supportassistant/TicketPageTests.java](../support-assistant-plsql-pagination/src/test/java/com/bsp/supportassistant/TicketPageTests.java) exactly.

<details>
<summary>Full <code>src/test/java/com/bsp/supportassistant/TicketPageTests.java</code></summary>

```java
package com.bsp.supportassistant;

import com.bsp.supportassistant.dto.PageResponse;
import com.bsp.supportassistant.dto.TicketResponse;
import com.bsp.supportassistant.exception.InvalidRequestException;
import com.bsp.supportassistant.repository.TicketRepository;
import com.bsp.supportassistant.service.TicketService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// These tests call the page procedure in your Oracle database, and roll back.
@SpringBootTest
@Transactional
class TicketPageTests {

    @Autowired
    private TicketService ticketService;

    @Autowired
    private TicketRepository ticketRepository;

    // Three tickets, so that there are at least two pages of 2.
    @BeforeEach
    void createTickets() {
        ticketRepository.createTicket("Page test: high", null, "HIGH");
        ticketRepository.createTicket("Page test: low", null, "LOW");
        ticketRepository.createTicket("Page test: medium", null, "MEDIUM");
    }

    @Test
    void aPageHoldsAtMostSizeTicketsAndCountsThemAll() {
        PageResponse<TicketResponse> first = ticketService.findPage(null, 0, 2, "dueBy", "asc");

        assertThat(first.content()).hasSize(2);
        assertThat(first.totalElements()).isEqualTo(ticketRepository.findTickets(null).size());
        assertThat(first.totalPages()).isGreaterThanOrEqualTo(2);
    }

    @Test
    void thePagesTogetherHoldEveryTicketOnceInOrder() {
        List<TicketResponse> all = new ArrayList<>();
        PageResponse<TicketResponse> page = ticketService.findPage(null, 0, 2, "dueBy", "asc");
        for (int number = 0; number < page.totalPages(); number++) {
            all.addAll(ticketService.findPage(null, number, 2, "dueBy", "asc").content());
        }

        assertThat(all).hasSize((int) page.totalElements());
        assertThat(all).extracting(TicketResponse::id).doesNotHaveDuplicates();
        assertThat(all).extracting(TicketResponse::dueBy).isSortedAccordingTo(Comparator.naturalOrder());
    }

    @Test
    void aPageAfterTheLastIsEmpty() {
        PageResponse<TicketResponse> page = ticketService.findPage(null, 9999, 5, "dueBy", "asc");

        assertThat(page.content()).isEmpty();
        assertThat(page.totalElements()).isPositive();
    }

    @Test
    void aSortThatIsNotAllowedIsRefused() {
        assertThatThrownBy(() -> ticketService.findPage(null, 0, 5, "colour", "asc"))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessage("Cannot sort by 'colour'. Use dueBy or createdAt");
    }
}
```

</details>

## Common mistakes

| Symptom | Cause | Fix |
|---|---|---|
| The `Page test` tickets stay in the table | The class has no `@Transactional` | Add `@Transactional` to the class |
| `PLS-00201: identifier 'SUPPORT_TICKET_PAGE_PKG.FIND_PAGE' must be declared` | `03-page-package.sql` was not run as the application user | Run it, as in step 2 |

Next: **[Recap and exercises](06-recap-and-exercises.md)**.
