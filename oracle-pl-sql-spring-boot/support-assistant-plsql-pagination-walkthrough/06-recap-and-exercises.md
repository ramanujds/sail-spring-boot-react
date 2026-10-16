# Step 6 — Recap and exercises

> [Back to index](README.md) · Previous: [Test the pages](05-test-the-pages.md) · Next: none

## Quick reference

| Concept | Where it lives |
|---|---|
| Read one page in Oracle | `OFFSET p_page * p_size ROWS FETCH NEXT p_size ROWS ONLY` in `03-page-package.sql` |
| A safe sort chosen by the caller | Four `CASE` lines in the `ORDER BY`, and `SORT_COLUMNS` in `TicketService` |
| The same order on every call | `ID` at the end of the `ORDER BY` |
| A cursor and a number from one call | `returningResultSet("P_TICKETS", ...)` and `out.get("P_TOTAL")` in `TicketRepository.findPage` |
| Total pages | `(total + size - 1) / size` |
| Defaults for the URL | `@RequestParam(defaultValue = "...")` in `TicketController.find` |
| `400` for a bad value | `InvalidRequestException` in `TicketService.findPage` |

## What the caller does with a page

| The caller wants | It uses |
|---|---|
| Show "page 2 of 3" | `page + 1` and `totalPages` |
| A "next" button | Enabled while `page + 1 < totalPages` |
| "12 tickets found" | `totalElements` |
| To stop | When `content` is empty |

## Gotchas

| Gotcha | What happens | Fix |
|---|---|---|
| Pages start at `0` | `?page=1` is the second page | Start at `page=0` |
| A page after the last | `200` with `"content":[]`, not an error | Stop before `totalPages` |
| `P_TOTAL` read as `Integer` | `ClassCastException`, `BigDecimal` to `Integer` | Read it as `Number` |
| No unique column at the end of `ORDER BY` | A ticket can appear on two pages, or on none | End with `ID` |
| The sort glued into the SQL text | SQL injection | Allow a fixed list of names, and use `CASE` lines |
| A ticket that sorts into an earlier page is added between two calls | The rows after it move down by one, so the next page repeats the last ticket of the page before | Expected with `OFFSET` paging. The caller can read again from page `0` |

## Discussion questions

1. Why does the count use the same `WHERE` as the page? What would the caller see if it did not?
2. The service turns `dueBy` into `DUE_BY`. Why not let the caller send `DUE_BY`?
3. Why can `TITLE` not share a `CASE` line with `DUE_BY`?
4. Why does the test walk through pages of 2, and not of 5?

## Exercises

1. With the 12 sample tickets, walk through every page with `size=4`. Write down the number of tickets on each page, and `totalPages`.
2. Add `sort=title`. You need one entry in `SORT_COLUMNS` and two new `CASE` lines in the package. Run `03-page-package.sql` again, and try `?sort=title&direction=desc`.
3. Change `MAX_SIZE` to `10`. Restart, and call `?size=25`. Write down the answer.
4. Add `priority` as a filter, next to `status`. It needs a new `IN` parameter in the package, in both `WHERE` clauses.
5. Add a test that sorts by `createdAt` and `desc`, and checks the order of `createdAt` across all pages.

## What's next

- The notes behind this walkthrough: [part 6, Pagination](../06-pagination.md).
- The other topic that starts from the same project: [part 5, Transaction management](../05-transaction-management.md), and its [walkthrough](../support-assistant-plsql-transactions-walkthrough/README.md).
