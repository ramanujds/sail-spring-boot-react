# Step 8 — Turn package errors into answers

> [Back to index](README.md) · Previous: [Change the status](07-change-status.md) · Next: [Several calls, all or none](09-transactions.md)

## Goal

Error numbers from `RAISE_APPLICATION_ERROR` become `400`, `404`, and `409`, with the text that the package author wrote.

## Why this matters

A package can raise only numbers from 20000 to 20999 with `RAISE_APPLICATION_ERROR`. Spring wraps them in an `UncategorizedSQLException`, and `getSQLException()` gives the number and the text. Mapping the number to a status code in one place keeps the web rules out of the package. Any other database error is a fault of the server, so the client gets a general message, and the real text stays in the log.

### 8.1 The error body

```java
public record ApiError(int status, String error, String message, LocalDateTime timestamp) {
}
```

Put it in the package `exception`. The real file has the same fields on separate lines.

### 8.2 The handler

`GlobalExceptionHandler` turns the exception into an `ApiError`:

```java
@ExceptionHandler(UncategorizedSQLException.class)
public ResponseEntity<ApiError> handleDatabase(UncategorizedSQLException ex) {
    SQLException sqlException = ex.getSQLException();
    int code = sqlException.getErrorCode();
    if (code >= 20000 && code <= 20999) {
        return build(statusFor(code), messageOf(sqlException));
    }
    return build(HttpStatus.INTERNAL_SERVER_ERROR, "The database call failed");
}
```

| Number | Status |
|---|---|
| `20003` | `404 Not Found` |
| `20004` | `409 Conflict` |
| other `200xx` | `400 Bad Request` |

`messageOf` keeps the first line of the Oracle message, after the colon. The full class is in the checkpoint.

## Try it

Restart. Ticket 1 is `IN_PROGRESS`, tickets 2 and 3 are `OPEN`.

```bash
curl -s -X POST http://localhost:8080/api/tickets -H "Content-Type: application/json" \
  -d '{"description":"no title","priority":"LOW"}'
curl -s -X POST http://localhost:8080/api/tickets -H "Content-Type: application/json" \
  -d '{"title":"x","priority":"URGENT"}'
curl -s -X PATCH http://localhost:8080/api/tickets/9999/status -H "Content-Type: application/json" \
  -d '{"status":"IN_PROGRESS"}'
curl -s -X PATCH http://localhost:8080/api/tickets/2/status -H "Content-Type: application/json" \
  -d '{"status":"CLOSED"}'
```

Expected:

```json
{"status":400,"error":"Bad Request","message":"Title is required","timestamp":"..."}
{"status":400,"error":"Bad Request","message":"Priority must be HIGH, MEDIUM or LOW","timestamp":"..."}
{"status":404,"error":"Not Found","message":"Ticket 9999 does not exist","timestamp":"..."}
{"status":409,"error":"Conflict","message":"Ticket 2 is OPEN. The next status is IN_PROGRESS","timestamp":"..."}
```

## Checkpoint

`src/main/java/com/bsp/supportassistant/exception/ApiError.java` - Matches [../support-assistant-plsql/src/main/java/com/bsp/supportassistant/exception/ApiError.java](../support-assistant-plsql/src/main/java/com/bsp/supportassistant/exception/ApiError.java) exactly.

<details>
<summary>Full <code>src/main/java/com/bsp/supportassistant/exception/ApiError.java</code></summary>

```java
package com.bsp.supportassistant.exception;

import java.time.LocalDateTime;

// The body of every error response.
public record ApiError(
        int status,
        String error,
        String message,
        LocalDateTime timestamp) {
}
```

</details>

`src/main/java/com/bsp/supportassistant/exception/GlobalExceptionHandler.java` - Grows in a later step.

<details>
<summary>Full <code>src/main/java/com/bsp/supportassistant/exception/GlobalExceptionHandler.java</code></summary>

```java
package com.bsp.supportassistant.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.UncategorizedSQLException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.sql.SQLException;
import java.time.LocalDateTime;

// One place that turns exceptions from every controller into an ApiError.
@RestControllerAdvice
public class GlobalExceptionHandler {

    // A PL/SQL program called RAISE_APPLICATION_ERROR. Oracle numbers these from 20000 to 20999.
    // Any other database error is a fault of the server, so the client gets a general message.
    @ExceptionHandler(UncategorizedSQLException.class)
    public ResponseEntity<ApiError> handleDatabase(UncategorizedSQLException ex) {
        SQLException sqlException = ex.getSQLException();
        int code = sqlException.getErrorCode();
        if (code >= 20000 && code <= 20999) {
            return build(statusFor(code), messageOf(sqlException));
        }
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "The database call failed");
    }

    // The error numbers are the ones listed at the top of the package specification.
    private HttpStatus statusFor(int oracleCode) {
        if (oracleCode == 20003) {
            return HttpStatus.NOT_FOUND;
        }
        if (oracleCode == 20004) {
            return HttpStatus.CONFLICT;
        }
        return HttpStatus.BAD_REQUEST;
    }

    // Oracle writes "ORA-20001: Title is required", then lines about where it happened.
    // Keep the text of the first line, after the colon.
    private String messageOf(SQLException sqlException) {
        String firstLine = sqlException.getMessage().lines().findFirst().orElse("");
        return firstLine.substring(firstLine.indexOf(": ") + 2);
    }

    private ResponseEntity<ApiError> build(HttpStatus status, String message) {
        ApiError body = new ApiError(status.value(), status.getReasonPhrase(), message, LocalDateTime.now());
        return ResponseEntity.status(status).body(body);
    }
}
```

</details>

## Common mistakes

| Symptom | Cause | Fix |
|---|---|---|
| `500` and `The database call failed` for a call that should be a rule error | The error is not from `RAISE_APPLICATION_ERROR`. For example `ORA-04063: package body ... has errors` | Read the log. Fix the package with `SHOW ERRORS` |
| A new `RAISE_APPLICATION_ERROR(-20006, ...)` answers `400`, and you wanted `404` | `statusFor` has no line for the new number | Add the number to `statusFor` |

Next: **[Several calls, all or none](09-transactions.md)**.
