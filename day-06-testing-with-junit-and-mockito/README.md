# Day 6: Testing

**Builds on:** [Day 5](../day-05-transactions-plsql-pagination-and-specifications/README.md)

Day 6 adds tests to the `support-assistant` project. It starts with the smallest kind of test, a JUnit test of plain Java. It then adds mocks, so a class can be tested without its collaborators, then mocks inside Spring with `@MockitoBean`, and ends with `MockMvc`, which tests the API endpoints without a database. The application code does not change.

## Parts

| Part | What | Notes |
|---|---|---|
| 1 | Unit testing: JUnit, assertions, arrange-act-assert | [01-unit-testing-basics.md](code-and-walkthroughs/01-unit-testing-basics.md) |
| 2 | Mocks with Mockito: `mock`, `when`, `verify` | [02-mocks-with-mockito.md](code-and-walkthroughs/02-mocks-with-mockito.md) |
| 3 | Mocking the repository in a Spring test with `@MockitoBean` | [03-mocking-in-spring-boot.md](code-and-walkthroughs/03-mocking-in-spring-boot.md) |
| 4 | Testing the endpoints with `@WebMvcTest` and `MockMvc` | [04-testing-endpoints-with-mockmvc.md](code-and-walkthroughs/04-testing-endpoints-with-mockmvc.md) |

## Project

| Project | Holds | Parts |
|---|---|---|
| [support-assistant-jpa](code-and-walkthroughs/support-assistant-jpa/) | The Day 5 project, with test classes in `src/test/java` | 1 to 4 |

## Before you start

- Your project from the end of Day 5, part 5.
- No database is needed for the tests of this day. The generated test `SupportAssistantApplicationTests` still needs one, because it starts the whole application. Run the other tests with `mvn test -Dtest='!SupportAssistantApplicationTests'`.
