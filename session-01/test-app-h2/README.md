# Test app (H2): Spring Boot 3.5 without Oracle

The same small app as [../test-app](../test-app), but backed by an in-memory H2 database. Use it to check the rest of your setup when Oracle is not reachable yet: JDK 21, Maven, your IDE, port 8000, Swagger UI, and `curl` or Postman.

> **Note:** H2 is only a stand-in for this check. The programme itself uses Oracle for every day. Data here lives in memory and is lost when the app stops.

## What this checks, and what it does not

| Checked | Not checked |
|---|---|
| JDK 21 and Maven build | Oracle connectivity and credentials |
| Spring Boot starts on port 8000 | Oracle SQL syntax and the `ojdbc11` driver |
| REST `GET` and `POST`, validation, `404` and `400` | Network path to the database host |
| Swagger UI and OpenAPI JSON | HikariCP behaviour against a real server |
| Your IDE can import, run, and debug a Spring Boot project | |

## Run it

Needs JDK 21 and Maven 3.9+. No database or Docker is needed.

```bash
mvn spring-boot:run
```

| What | URL |
|---|---|
| Swagger UI | <http://localhost:8000/swagger-ui.html> |
| H2 console (browse the data) | <http://localhost:8000/h2-console> |

In the H2 console, use JDBC URL `jdbc:h2:mem:testdb`, user `sa`, and an empty password.

## API

| Method | Path | Result |
|---|---|---|
| `GET` | `/api/messages` | `200`, list of messages |
| `GET` | `/api/messages/{id}` | `200`, or `404` |
| `POST` | `/api/messages` | `201`, or `400` if `text` is blank |

```bash
curl -s http://localhost:8000/api/messages

curl -s -X POST http://localhost:8000/api/messages \
  -H 'Content-Type: application/json' \
  -d '{"text":"my first message"}'
```

## Test

```bash
mvn test
```

## How it differs from the Oracle version

| | `test-app` (Oracle) | `test-app-h2` (this app) |
|---|---|---|
| Driver | `ojdbc11` | `h2` |
| Connection | `jdbc:oracle:thin:@//host:port/service` | `jdbc:h2:mem:testdb` |
| Table creation | `db/init.sql`, run against Oracle | `schema.sql`, runs automatically at startup |
| Sample rows | In `db/init.sql` | `data.sql`, runs automatically |
| Column types | `NUMBER`, `VARCHAR2`, `TIMESTAMP` | `BIGINT`, `VARCHAR`, `TIMESTAMP` |
| Sequence | `MESSAGES_SEQ` | `MESSAGES_SEQ` (same name and use) |
| Persistence | Stays in the database | Lost when the app stops |
| Java code | Same | Same |

The entity, repository, controller, and tests are identical in both apps, so if this one works and the Oracle one does not, the problem is in the database connection, not your toolchain.

## Common pitfalls

| Symptom | Cause | Fix |
|---|---|---|
| `Port 8000 was already in use` | Another process holds the port | Stop it, or change `server.port` in `application.yml` |
| `release version 21 not supported` | Maven is running on an older JDK | Point `JAVA_HOME` at JDK 21 and run `mvn -version` |
| Data missing after a restart | In-memory database | Expected; `data.sql` re-creates the sample rows on each start |
