# support-assistant-plsql-jpa

The same ticket API as [support-assistant-plsql](../support-assistant-plsql/), calling the same Oracle PL/SQL package through **Spring Data JPA** instead of `SimpleJdbcCall`. This is the project of the optional [part 4](../04-the-same-calls-with-spring-data-jpa.md).

The endpoints, the service, the controller, and the records are the same. Only the repository, one handler, and the build file differ.

## Versions

| Item | Version |
|---|---|
| Java | 21 |
| Spring Boot | 4.1.1 |
| Hibernate | 7 (comes with Spring Boot) |
| Oracle JDBC driver | `ojdbc17` (comes with Spring Boot) |
| Oracle Database | 19c, or 21c Express Edition |

## Before you build or run

1. Run the scripts of the other project's [db/oracle](../support-assistant-plsql/db/oracle/) folder, in order. This project has a copy of them in [db/oracle](db/oracle/). Run them once. Both projects use the same tables and package.
2. Set `DB_USERNAME` and `DB_PASSWORD` as environment variables. `DB_URL` is optional.

> **Note:** with JPA the application connects to Oracle when it starts. A wrong or unset password stops the start-up with `Unable to obtain isolated JDBC connection [ORA-01017 ...`.

## Build and run

```bash
export DB_USERNAME=YOUR_USERNAME
export DB_PASSWORD=YOUR_PASSWORD
mvn clean verify
java -jar target/support-assistant-plsql-jpa-0.0.1-SNAPSHOT.jar
```

Both projects use port `8080`. Stop one before you start the other.

## What is different

| File | Change |
|---|---|
| `repository/TicketRepository.java` | Calls the package through JPA. Same four methods |
| `repository/TicketProcedures.java` | Two `@Procedure` methods |
| `repository/TicketEntity.java` | An entity that only names the repository type |
| `exception/GlobalExceptionHandler.java` | Looks for the `SQLException` inside the exceptions of JPA |
| `pom.xml`, `application.yaml` | Spring Data JPA instead of the JDBC starter |

## Running on Spring Boot 3.5

Use the same four `pom.xml` changes as in the other project. Built and tested on Spring Boot 3.5.16: all four tests pass. Hibernate 6 gives a `Timestamp` for a cursor column and Hibernate 7 gives a `LocalDateTime`. `TicketRepository` handles both.
