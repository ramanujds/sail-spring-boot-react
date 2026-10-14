# support-assistant-composite-key

[support-assistant-jpa](../support-assistant-jpa/) at the end of Day 4, part 4, plus **ticket watchers**: a table whose primary key is two columns. This is the project of Day 4, part 5.

## Versions

| Item | Version |
|---|---|
| Java | 21 |
| Spring Boot | 4.1.1 |
| Hibernate | 7.4.5 (comes with Spring Boot) |
| Oracle JDBC driver | `ojdbc17` (comes with Spring Boot) |
| Oracle Database | 19c, or 21c Express Edition |
| springdoc-openapi (Swagger UI) | 3.1.0 |

## Before you build or run

1. Have an Oracle database with the three tables of [db/oracle/schema.sql](db/oracle/schema.sql): `TICKETS`, `COMMENTS`, and `TICKET_WATCHERS`. If you already have the first two, run only the `CREATE TABLE TICKET_WATCHERS` statement.
2. Open [src/main/resources/application.yaml](src/main/resources/application.yaml) and set `url`, `username`, and `password`, as in [support-assistant-jpa](../support-assistant-jpa/README.md).

> **Warning:** Do not commit the file with a real password in it.

## Build and run

```bash
mvn clean package
java -jar target/support-assistant-0.0.1-SNAPSHOT.jar
```

The application starts on port `8080`. The test starts the whole application, so all three tables must exist.

## What is new compared with support-assistant-jpa

```text
db/oracle/schema.sql                         # adds TICKET_WATCHERS
src/main/java/com/bsp/supportassistant
├── model/TicketWatcherId.java               # the key: ticketId and username
├── model/TicketWatcher.java                 # the entity, with @EmbeddedId
├── dto/WatcherRequest.java
├── dto/WatcherResponse.java
├── mapper/WatcherMapper.java
├── repository/TicketWatcherRepository.java
├── exception/AlreadyWatchingException.java  # answered with 409
└── api/WatcherController.java
```

## Endpoints added

| Method | URL | Status codes |
|---|---|---|
| `GET` | `/api/v1/tickets/{ticketId}/watchers` | `200`, `404` |
| `POST` | `/api/v1/tickets/{ticketId}/watchers`, body `{"username":"karan"}` | `201`, `404`, `409` when the person already watches |
| `DELETE` | `/api/v1/tickets/{ticketId}/watchers/{username}` | `204` |

The endpoints of [support-assistant-jpa](../support-assistant-jpa/README.md) are unchanged. Deleting a ticket deletes its watchers, because the foreign key has `ON DELETE CASCADE`.

See [05-composite-keys.md](../05-composite-keys.md).

## Running on Spring Boot 3.5

The Java code and `application.yaml` are the same. Only these lines of `pom.xml` change:

| Item in `pom.xml` | Spring Boot 4.1.1 (this project) | Spring Boot 3.5.x |
|---|---|---|
| Parent version | `4.1.1` | `3.5.16`, or another 3.5.x |
| Web starter | `spring-boot-starter-webmvc` | `spring-boot-starter-web` |
| Test starter | `spring-boot-starter-webmvc-test` | `spring-boot-starter-test` |
| Oracle driver | `ojdbc17` | `ojdbc11` |
| springdoc-openapi | `3.1.0` | `2.8.17`, or another 2.8.x |

Built and run on Spring Boot 3.5.16 (Hibernate 6.6) with these changes. An `@Embeddable` record works as the key on both.

Some messages differ in wording: Hibernate writes `Schema-validation:` (with a hyphen) in schema errors, and the lazy loading error reads `failed to lazily initialize a collection of role`.

