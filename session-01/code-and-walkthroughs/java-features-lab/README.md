# java-features-lab

A small Maven project for trying the Java 17 and 21 features you use most in a Spring Boot application. Most of the code is provided and runnable. Eight places are marked `TODO` for you to implement, each with a test that tells you when it is done.

| | |
|---|---|
| **Java** | 21 (`maven.compiler.release` is 21) |
| **Build** | Maven 3.9 or later |
| **Dependencies** | JUnit 5 for tests, nothing else |
| **Starting state** | Compiles. `mvn test` reports 22 tests, 21 of them red until you finish the TODOs |

## Run

```bash
mvn test                              # all tests
mvn test -Dtest=TicketSummaryTest     # one test class
mvn -q compile                        # compile, then run any demo:
java -cp target/classes com.helpdeskpro.lab.records.RecordsDemo
```

## Packages

Each package is named after the feature it practises.

| Package | Feature | Where it appears in a Spring Boot project | Demo class (has `main`) |
|---|---|---|---|
| `records` | Records | Request and response DTOs | `RecordsDemo` |
| `sealedtypes` | Sealed interfaces | The closed set of ticket events | `SealedTypesDemo` |
| `patternmatching` | `instanceof` patterns, record patterns, `switch` patterns | Handling events and statuses in the service layer | `PatternMatchingDemo` |
| `switchexpressions` | Switch expressions and `yield` | Mapping a ticket status to a label or a rule | `SwitchExpressionsDemo` |
| `textblocks` | Text blocks | Multi-line JPQL and SQL in `@Query` | `TextBlocksDemo` |
| `virtualthreads` | Virtual threads | `spring.threads.virtual.enabled=true` | `VirtualThreadsLab` |
| `domain` | Shared `TicketStatus` enum | Provided; nothing to implement | none |

## Walkthrough

Follow [java-features-lab-walkthrough](../java-features-lab-walkthrough/README.md) for the steps in order.
