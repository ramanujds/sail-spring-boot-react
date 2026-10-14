# support-assistant-jpa

The `support-assistant` application, with the tickets stored in an **Oracle database** instead of a Java list. The endpoints are the same as in [support-assistant](../support-assistant/).

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

1. Have an Oracle database you can log in to, and create the table with [db/oracle/schema.sql](db/oracle/schema.sql). See [05-oracle-database-setup.md](../05-oracle-database-setup.md).
2. Open [src/main/resources/application.yaml](src/main/resources/application.yaml) and set the connection:

| Setting | Meaning |
|---|---|
| `url` | `jdbc:oracle:thin:@//HOST:PORT/SERVICE` |
| `username` | The database user that owns the `TICKETS` table |
| `password` | Its password |

The file has two blocks: Option B (Oracle XE 21 in Docker) is active, and Option A (Oracle 19c) is commented out. Replace the placeholders and keep one block.

> **Warning:** Do not commit the file with a real password in it.

## Build and run

```bash
mvn clean package
java -jar target/support-assistant-0.0.1-SNAPSHOT.jar
```

Or run `SupportAssistantApplication` from IntelliJ. The application starts on port `8080`.

> **Note:** `mvn clean package` also runs the test, and the test starts the whole application. The database must be running and `application.yaml` must hold the right values, or the build fails with `Failed to load ApplicationContext`.

## Layout

```text
db/oracle/schema.sql                         # the TICKETS table, run once by hand
src/main/resources/application.yaml          # the settings
src/main/java/com/bsp/supportassistant
├── SupportAssistantApplication.java
├── model/Ticket.java                        # an entity: one object is one row of TICKETS
├── model/TicketStatus.java
├── model/Priority.java
├── dto/TicketRequest.java
├── dto/TicketResponse.java
├── mapper/TicketMapper.java
├── repository/TicketRepository.java         # an interface: Spring writes the class
└── api/TicketController.java
```

## Endpoints

| Method | URL | Status codes |
|---|---|---|
| `GET` | `/api/v1/tickets` and `?status=OPEN` | `200`, `400` for an unknown status |
| `GET` | `/api/v1/tickets/{id}` | `200`, `400` for an id that is not a UUID, `404` |
| `POST` | `/api/v1/tickets` | `201`, `400`, `500` when `title` or `priority` is missing |
| `PATCH` | `/api/v1/tickets/{id}` | `200`, `400`, `404` |
| `DELETE` | `/api/v1/tickets/{id}` | `204`, `400`, `404` |

Swagger UI: <http://localhost:8080/swagger-ui.html>

## What changed since support-assistant

- `pom.xml` has `spring-boot-starter-data-jpa` and the Oracle driver `ojdbc17`.
- `Ticket` is an **entity**: `@Entity`, `@Table`, `@Id`, `@GeneratedValue`, and `@Enumerated`.
- `TicketRepository` is an interface that extends `JpaRepository`. The list, the id creation, and `update` are gone.
- `TicketMapper.applyChanges` copies the fields of a `PATCH` onto the stored ticket.
- `application.properties` is replaced by `application.yaml`, which holds the database `url`, `username`, and `password`.
- The `TICKETS` table is created by `db/oracle/schema.sql`. Hibernate only checks it (`ddl-auto: validate`).

See [06-spring-data-jpa-with-oracle.md](../06-spring-data-jpa-with-oracle.md).

## What this version does not do yet

- There is no validation. A request without a `title` or a `priority` reaches the database, which refuses it, and the API answers `500`.
- The `404` response has an empty body. There is no JSON error message.
- Swagger UI lists only `200` for each endpoint.

## Running on Spring Boot 3.5

The Java code and `application.yaml` are the same. Only these lines of `pom.xml` change:

| Item in `pom.xml` | Spring Boot 4.1.1 (this project) | Spring Boot 3.5.x |
|---|---|---|
| Parent version | `4.1.1` | `3.5.16`, or another 3.5.x |
| Web starter | `spring-boot-starter-webmvc` | `spring-boot-starter-web` |
| Test starter | `spring-boot-starter-webmvc-test` | `spring-boot-starter-test` |
| Oracle driver | `ojdbc17` | `ojdbc11` |
| springdoc-openapi | `3.1.0` | `2.8.17`, or another 2.8.x |

Built and run on Spring Boot 3.5.16 (Hibernate 6.6) with these changes.

Some messages differ in wording: Hibernate writes `Schema-validation:` (with a hyphen) in schema errors, and the lazy loading error reads `failed to lazily initialize a collection of role`.

