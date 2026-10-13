# Day 3: REST API, DTOs and Validation

**Builds on:** [Day 2](../day-02-spring-boot-basics-and-jpa/README.md)

Day 3 improves the `support-assistant` API from Day 2, one idea at a time. Each part has a note. Parts change the current project in place, and a new copy is made only for a large change.

## Parts

| Part | What | Notes |
|---|---|---|
| 1 | DTOs: `TicketRequest` and `TicketResponse` | [01-dto-request-and-response.md](code-and-walkthroughs/01-dto-request-and-response.md) |
| 2 | Streams and `Optional` | [02-streams-and-optional.md](code-and-walkthroughs/02-streams-and-optional.md) |
| 3 | Status codes | [03-status-codes.md](code-and-walkthroughs/03-status-codes.md) |
| 4 | UUIDs for ids | [04-uuid-ids.md](code-and-walkthroughs/04-uuid-ids.md) |
| 5 | Set up the Oracle database (19c, or XE 21 in Docker) | [05-oracle-database-setup.md](code-and-walkthroughs/05-oracle-database-setup.md) |
| 6 | Store tickets in Oracle with Spring Data JPA | [06-spring-data-jpa-with-oracle.md](code-and-walkthroughs/06-spring-data-jpa-with-oracle.md) |

## Projects

| Project | Holds | Parts |
|---|---|---|
| [support-assistant](code-and-walkthroughs/support-assistant/) | Tickets in a Java list | 1 to 4. Each part changes it in place |
| [support-assistant-jpa](code-and-walkthroughs/support-assistant-jpa/) | Tickets in an Oracle database | 6 onwards. A copy of `support-assistant`, because the storage changed |

Each project has a README with build and run steps.
