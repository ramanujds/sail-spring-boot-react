# support-assistant

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
├── repository/TicketRepository.java   # keeps the tickets in a List
└── api/TicketController.java          # the REST endpoints
```

## Endpoints

| Method | URL | What it does |
|---|---|---|
| `GET` | `/api/v1/tickets` | Lists all tickets |
| `GET` | `/api/v1/tickets?status=OPEN` | Lists the tickets with one status |
| `GET` | `/api/v1/tickets/{id}` | Reads one ticket |
| `POST` | `/api/v1/tickets` | Creates a ticket |
| `PATCH` | `/api/v1/tickets/{id}` | Changes only the fields you send |
| `DELETE` | `/api/v1/tickets/{id}` | Deletes a ticket |

Swagger UI: <http://localhost:8080/swagger-ui.html>
OpenAPI document: <http://localhost:8080/v3/api-docs>

## Try it

```bash
curl -i -X POST http://localhost:8080/api/v1/tickets \
  -H "Content-Type: application/json" \
  -d '{"title":"Cannot print invoices","description":"Printer on floor 2","status":"OPEN","priority":"HIGH"}'
```

```text
HTTP/1.1 200
{"description":"Printer on floor 2","id":1,"priority":"HIGH","status":"OPEN","title":"Cannot print invoices"}
```

Change only the status of ticket 1:

```bash
curl -i -X PATCH http://localhost:8080/api/v1/tickets/1 \
  -H "Content-Type: application/json" \
  -d '{"status":"IN_PROGRESS"}'
```

List the tickets that are in progress:

```bash
curl "http://localhost:8080/api/v1/tickets?status=IN_PROGRESS"
```

## What this version does not do yet

- The tickets live in a `List`, so they are lost when the application stops.
- The controller returns the `Ticket` class itself. There are no request or response classes.
- There is no validation and no error handling. Asking for a ticket that does not exist returns `200` with an empty body. A `status` or `priority` that is not in the enum returns `400` with Spring's default error body.
- Every success returns `200`. Creating a ticket does not return `201`.
