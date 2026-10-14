# support-assistant

> This is the in-memory version, with parts 1 to 4 applied. The version that stores tickets in Oracle is [support-assistant-jpa](../support-assistant-jpa/).

A small Spring Boot application that manages support tickets. It has one class for the data (`Ticket`), one class that stores the tickets (`TicketRepository`), and one controller that exposes them as a REST API.

## Versions

| Item | Version |
|---|---|
| Java | 21 |
| Spring Boot | 4.1.1 |
| springdoc-openapi (Swagger UI) | 3.1.0 |

## Build and run

```bash
mvn clean package
java -jar target/support-assistant-0.0.1-SNAPSHOT.jar
```

Or run `SupportAssistantApplication` from IntelliJ. The application starts on port `8080`.

## Layout

```text
src/main/java/com/bsp/supportassistant
├── SupportAssistantApplication.java   # starts the application
├── model/Ticket.java                  # the ticket data
├── model/TicketStatus.java            # OPEN, IN_PROGRESS, RESOLVED, CLOSED
├── model/Priority.java                # LOW, MEDIUM, HIGH
├── dto/TicketRequest.java             # what a client sends
├── dto/TicketResponse.java            # what a client receives
├── mapper/TicketMapper.java           # converts between Ticket and the DTOs
├── repository/TicketRepository.java   # keeps the tickets in a List
└── api/TicketController.java          # the REST endpoints
```

## Endpoints

| Method | URL | What it does | Status codes |
|---|---|---|---|
| `GET` | `/api/v1/tickets` | Lists all tickets (`TicketResponse`) | `200` |
| `GET` | `/api/v1/tickets?status=OPEN` | Lists the tickets with one status | `200`, `400` for an unknown status |
| `GET` | `/api/v1/tickets/{id}` | Reads one ticket | `200`, `400` for an id that is not a UUID, `404` |
| `POST` | `/api/v1/tickets` | Creates a ticket from a `TicketRequest`. The status is always `OPEN` | `201`, `400` |
| `PATCH` | `/api/v1/tickets/{id}` | Changes only the fields you send | `200`, `404`, `400` |
| `DELETE` | `/api/v1/tickets/{id}` | Deletes a ticket | `204`, `404`, `400` for an id that is not a UUID |

Swagger UI: <http://localhost:8080/swagger-ui.html>
OpenAPI document: <http://localhost:8080/v3/api-docs>

## Try it

```bash
curl -i -X POST http://localhost:8080/api/v1/tickets \
  -H "Content-Type: application/json" \
  -d '{"title":"Cannot print invoices","description":"Printer on floor 2","status":"OPEN","priority":"HIGH"}'
```

```text
HTTP/1.1 201
{"id":"92818c23-79b6-4d51-b5c5-617cd067fa71","title":"Cannot print invoices","description":"Printer on floor 2","status":"OPEN","priority":"HIGH"}
```

Change only the status of a ticket. Use the `id` from the response above in place of `TICKET_ID`:

```bash
curl -i -X PATCH http://localhost:8080/api/v1/tickets/TICKET_ID \
  -H "Content-Type: application/json" \
  -d '{"status":"IN_PROGRESS"}'
```

List the tickets that are in progress:

```bash
curl "http://localhost:8080/api/v1/tickets?status=IN_PROGRESS"
```

## What changed since part 3

- A ticket's `id` is a `UUID` instead of a number. `TicketRepository.save` creates it with `UUID.randomUUID()`, and the counter is gone.
- The `{id}` in the URL is read as a `UUID`. A value that is not a UUID, such as `/api/v1/tickets/1`, returns `400`.

See [04-uuid-ids.md](../04-uuid-ids.md).

## What this version does not do yet

- The tickets live in a `List`, so they are lost when the application stops.
- There is no validation. A ticket with no title is accepted.
- The `404` response has an empty body. There is no JSON error message.
- Swagger UI lists only `200` for each endpoint.

## Running on Spring Boot 3.5

The Java code and `application.yaml` are the same. Only these lines of `pom.xml` change:

| Item in `pom.xml` | Spring Boot 4.1.1 (this project) | Spring Boot 3.5.x |
|---|---|---|
| Parent version | `4.1.1` | `3.5.16`, or another 3.5.x |
| Web starter | `spring-boot-starter-webmvc` | `spring-boot-starter-web` |
| Test starter | `spring-boot-starter-webmvc-test` | `spring-boot-starter-test` |
| springdoc-openapi | `3.1.0` | `2.8.17`, or another 2.8.x |

Built and run on Spring Boot 3.5.16 with these changes.
