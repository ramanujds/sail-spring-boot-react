# support-assistant-jpa

The Day 5 `support-assistant` project with tests. The files under `src/main` are the same as at the end of [Day 5](../../../day-05-transactions-plsql-pagination-and-specifications/code-and-walkthroughs/support-assistant-jpa/). Everything new is in `src/test/java`.

## Versions

| Item | Version |
|---|---|
| Java | 21 |
| Spring Boot | 4.1.1 |
| JUnit | 6 (the `org.junit.jupiter.api` package, as in JUnit 5) |
| Mockito | 5 |
| Hibernate | 7.4.5 (comes with Spring Boot) |
| Oracle JDBC driver | `ojdbc17` (comes with Spring Boot) |
| springdoc-openapi (Swagger UI) | 3.1.0 |

## Run the tests

```bash
mvn test -Dtest='!SupportAssistantApplicationTests'
```

You should see `Tests run: 27, Failures: 0, Errors: 0`. No database is needed.

| Command | What it runs |
|---|---|
| `mvn test` | Every test, including `SupportAssistantApplicationTests`, which needs the Oracle database and the tables |
| `mvn test -Dtest=SlaPolicyTest` | One test class |
| `mvn test -Dtest=TicketControllerTest#getByIdAnswers200WithTheTicket` | One test method |

In IntelliJ, click the green triangle next to a class or a method.

## Tests

```text
src/test/java/com/bsp/supportassistant
├── service/SlaPolicyTest.java               # 6 tests. Plain JUnit
├── mapper/TicketMapperTest.java             # 3 tests. Plain JUnit
├── service/TicketServiceByHandTest.java     # 2 tests. mock(...) written out
├── service/TicketServiceMockitoTest.java    # 4 tests. @Mock and @InjectMocks
├── service/TicketServiceSpringTest.java     # 3 tests. @MockitoBean in a small Spring context
├── api/TicketControllerTest.java            # 9 tests. @WebMvcTest and MockMvc
└── SupportAssistantApplicationTests.java    # 1 test. Starts the application, needs Oracle
```

## Running on Spring Boot 3.5

The tests are the same. Besides the `pom.xml` lines in the [Day 5 README](../../../day-05-transactions-plsql-pagination-and-specifications/code-and-walkthroughs/support-assistant-jpa/README.md), change one import in `TicketControllerTest`:

| Spring Boot 4.1.1 (this project) | Spring Boot 3.5.x |
|---|---|
| `org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest` | `org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest` |

`@MockitoBean` is in the same package on both: `org.springframework.test.context.bean.override.mockito.MockitoBean`. All 27 tests pass on Spring Boot 3.5.16 with this change.
