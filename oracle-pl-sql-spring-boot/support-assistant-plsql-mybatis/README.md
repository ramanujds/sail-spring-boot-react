# support-assistant-plsql-mybatis

The same ticket API as [support-assistant-plsql](../support-assistant-plsql/), calling the same Oracle PL/SQL package through **MyBatis annotations** instead of `SimpleJdbcCall`. This is the project of the optional [part 4](../04-the-same-calls-with-mybatis.md).

The endpoints, the service, the controller, the records, the error handler, and the tests are the same. Only the repository layer and the build and settings files differ. There is no entity class and no JPA.

## Versions

| Item | Version |
|---|---|
| Java | 21 |
| Spring Boot | 4.1.1 |
| MyBatis Spring Boot starter | 4.1.0 (MyBatis 3.5.19) |
| Oracle JDBC driver | `ojdbc17` (comes with Spring Boot) |
| Oracle Database | 19c, or 21c Express Edition |

## Before you build or run

1. Run the scripts in [db/oracle](db/oracle/), in order. They are the same as in the main project, so run them once for both projects.
2. Set `DB_USERNAME` and `DB_PASSWORD` as environment variables. `DB_URL` is optional.

> **Note:** a variable that is not set reaches Oracle as the text `${DB_PASSWORD}`. The application still starts, and the first call fails with `ORA-01017`.

## Build and run

```bash
export DB_USERNAME=YOUR_USERNAME
export DB_PASSWORD=YOUR_PASSWORD
mvn clean verify
java -jar target/support-assistant-plsql-mybatis-0.0.1-SNAPSHOT.jar
```

Both projects use port `8080`. Stop one before you start the other. `mvn clean verify` runs four tests that call the package in your database. Each test rolls back.

## What is different

| File | Change |
|---|---|
| `repository/TicketMapper.java` | One method for each program. The call is in the annotation |
| `repository/TicketRepository.java` | Gives the service its four methods. Passes a `Map` to the mapper |
| `application.yaml` | `jdbc-type-for-null` and `local-cache-scope` |
| `pom.xml` | `mybatis-spring-boot-starter` instead of `spring-boot-starter-jdbc` |

## Running on Spring Boot 3.5

Use the same four `pom.xml` changes as in the main project, and set the MyBatis starter to `3.0.5`:

| Item in `pom.xml` | Spring Boot 4.1.1 (this project) | Spring Boot 3.5.x |
|---|---|---|
| Parent version | `4.1.1` | `3.5.16`, or another 3.5.x |
| Web starter | `spring-boot-starter-webmvc` | `spring-boot-starter-web` |
| Test starter | `spring-boot-starter-webmvc-test` | `spring-boot-starter-test` |
| Oracle driver | `ojdbc17` | `ojdbc11` |
| MyBatis Spring Boot starter | `4.1.0` | `3.0.5` |

Built and tested on Spring Boot 3.5.16 with these changes: all four tests pass.

## What this project does not do

- It does not check the request fields in Java. The package checks them.
- It has no login. Any caller can change any ticket.
- The mapping of a cursor to `TicketResponse` is by position. The fields must stay in the order of the columns in `find_tickets`.
