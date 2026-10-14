# Day 4: Exception handling, relationships and composite keys

**Builds on:** [Day 3](../day-03-rest-api-dtos-and-validation/README.md)

Day 4 makes the `support-assistant` API answer errors properly, adds more ways to ask the repository for data, connects tickets to comments, and ends with a table that has a two-column key. Parts 1 to 4 change one project in place. Part 5 works in its own copy.

## Parts

| Part | What | Notes |
|---|---|---|
| 1 | Exception handling: `ApiError`, your own exception, `@RestControllerAdvice` | [01-exception-handling.md](code-and-walkthroughs/01-exception-handling.md) |
| 2 | Custom repository methods: method names and `@Query` | [02-custom-repository-methods.md](code-and-walkthroughs/02-custom-repository-methods.md) |
| 3 | Entity relationships: tickets and comments | [03-entity-relationships.md](code-and-walkthroughs/03-entity-relationships.md) |
| 4 | Lazy loading and the N+1 problem | [04-lazy-loading-and-n-plus-1.md](code-and-walkthroughs/04-lazy-loading-and-n-plus-1.md) |
| 5 | Composite keys: ticket watchers | [05-composite-keys.md](code-and-walkthroughs/05-composite-keys.md) |

## Projects

| Project | Holds | Parts |
|---|---|---|
| [support-assistant-jpa](code-and-walkthroughs/support-assistant-jpa/) | Tickets and comments in Oracle | 1 to 4. Each part changes it in place |
| [support-assistant-composite-key](code-and-walkthroughs/support-assistant-composite-key/) | The same, plus ticket watchers with a composite key | 5. A copy of `support-assistant-jpa` at the end of part 4 |

Each project has a README with build and run steps.

## Before you start

- Your project from the end of Day 3, part 6, running against your Oracle database, with the `TICKETS` table. See [Day 3, part 5](../day-03-rest-api-dtos-and-validation/code-and-walkthroughs/05-oracle-database-setup.md) if the database is not set up.
- Two tables are added during the day. Each part gives the statement to run:

| Table | Created in | Script |
|---|---|---|
| `COMMENTS` | Part 3 | `support-assistant-jpa/db/oracle/schema.sql` |
| `TICKET_WATCHERS` | Part 5 | `support-assistant-composite-key/db/oracle/schema.sql` |
