# Step 3 — Call the log procedure

> [Back to index](README.md) · Previous: [The error log](02-the-error-log.md) · Next: [Record the failure](04-record-the-failure.md)

## Goal

`ErrorLogRepository`, which calls `SUPPORT_ERROR_PKG.LOG_ERROR`, and `ErrorLogService`, which the ticket service will call.

## Why this matters

The repository calls the database. The service decides the transaction. You keep that split from the first project. The service starts with a plain `@Transactional`, the default. In step 4 you see what the default does to the log line, and in step 5 you change it.

### 3.1 The repository

Create `ErrorLogRepository` in the package `repository`. It is the pattern of `TicketRepository`, with one `SimpleJdbcCall`:

```java
public ErrorLogRepository(JdbcTemplate jdbcTemplate) {
    this.logError = new SimpleJdbcCall(jdbcTemplate)
            .withCatalogName("SUPPORT_ERROR_PKG").withProcedureName("LOG_ERROR");
}

public void logError(String message) {
    logError.execute(message);
}
```

### 3.2 The service

Create `ErrorLogService` in the package `service`:

```java
@Transactional
public void record(String message) {
    errorLogRepository.logError(message);
}
```

The import is `org.springframework.transaction.annotation.Transactional`.

## Try it

```bash
mvn -q compile
```

Expected: no output. Nothing calls the new classes yet.

## Checkpoint

`src/main/java/com/bsp/supportassistant/repository/ErrorLogRepository.java` - Matches [../support-assistant-plsql-transactions/src/main/java/com/bsp/supportassistant/repository/ErrorLogRepository.java](../support-assistant-plsql-transactions/src/main/java/com/bsp/supportassistant/repository/ErrorLogRepository.java) exactly.

<details>
<summary>Full <code>src/main/java/com/bsp/supportassistant/repository/ErrorLogRepository.java</code></summary>

```java
package com.bsp.supportassistant.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.simple.SimpleJdbcCall;
import org.springframework.stereotype.Repository;

// Calls the package SUPPORT_ERROR_PKG.
@Repository
public class ErrorLogRepository {

    private final SimpleJdbcCall logError;

    public ErrorLogRepository(JdbcTemplate jdbcTemplate) {
        this.logError = new SimpleJdbcCall(jdbcTemplate)
                .withCatalogName("SUPPORT_ERROR_PKG").withProcedureName("LOG_ERROR");
    }

    public void logError(String message) {
        logError.execute(message);
    }
}
```

</details>

`src/main/java/com/bsp/supportassistant/service/ErrorLogService.java` - Changes in a later step.

<details>
<summary>Full <code>src/main/java/com/bsp/supportassistant/service/ErrorLogService.java</code></summary>

```java
package com.bsp.supportassistant.service;

import com.bsp.supportassistant.repository.ErrorLogRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// Writes a line to SUPPORT_ERROR_LOG.
@Service
public class ErrorLogService {

    private final ErrorLogRepository errorLogRepository;

    public ErrorLogService(ErrorLogRepository errorLogRepository) {
        this.errorLogRepository = errorLogRepository;
    }

    // A plain @Transactional. Step 5 changes it.
    @Transactional
    public void record(String message) {
        errorLogRepository.logError(message);
    }
}
```

</details>

## Common mistakes

| Symptom | Cause | Fix |
|---|---|---|
| `PLS-00201: identifier 'SUPPORT_ERROR_PKG.LOG_ERROR' must be declared` in the log, at the first call | `03-error-log.sql` was not run, or was run as another user | Run it as the application user |
| `mvn -q compile` fails with `cannot find symbol ... class SimpleJdbcCall` | An import is missing | Import `org.springframework.jdbc.core.simple.SimpleJdbcCall` and `org.springframework.jdbc.core.JdbcTemplate` |

Next: **[Record the failure](04-record-the-failure.md)**.
