# Step 3 — Project and connection

> [Back to index](README.md) · Previous: [Tables and the PL/SQL package](02-database-objects.md) · Next: [Call a function](04-call-a-function.md)

## Goal

A Spring Boot application that starts, with the Oracle connection settings read from environment variables.

## Why this matters

Spring Boot builds the connection pool and a `JdbcTemplate` from `application.yaml`. You do not write connection code. The password is read from the environment, so it never appears in a file that you commit.

### 3.1 Create the project

Use Spring Initializr with:

| Field | Value |
|---|---|
| Group | `com.bsp` |
| Artifact | `support-assistant-plsql` |
| Java | 21 |
| Dependencies | Spring Web, JDBC API, Oracle Driver |

Do **not** add Spring Data JPA. Keep the base package `com.bsp.supportassistant`.

### 3.2 The dependencies

`pom.xml` has three that matter:

| Dependency | Why |
|---|---|
| `spring-boot-starter-webmvc` | Controllers and JSON |
| `spring-boot-starter-jdbc` | The pool, `JdbcTemplate`, `SimpleJdbcCall`, `@Transactional` |
| `ojdbc17` (runtime) | Lets Java talk to Oracle |

### 3.3 The connection

```yaml
spring:
  datasource:
    url: ${DB_URL:jdbc:oracle:thin:@//localhost:1521/XEPDB1}
    username: ${DB_USERNAME}
    password: ${DB_PASSWORD}
```

`${DB_USERNAME}` reads an environment variable. `${DB_URL:...}` has a default.

### 3.4 Set the variables

| Where you run | How |
|---|---|
| IntelliJ | **Run | Edit Configurations | Environment variables**: `DB_USERNAME=YOUR_USERNAME;DB_PASSWORD=YOUR_PASSWORD` |
| Terminal | `export DB_USERNAME=YOUR_USERNAME` and `export DB_PASSWORD=YOUR_PASSWORD` |

## Try it

Run `SupportAssistantApplication`.

```text
Started SupportAssistantApplication in 1.5 seconds
```

> **Note:** the application starts even when the password is wrong. Nothing connects to Oracle until the first call.

## Checkpoint

`pom.xml` - Matches [../support-assistant-plsql/pom.xml](../support-assistant-plsql/pom.xml) exactly.

<details>
<summary>Full <code>pom.xml</code></summary>

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>4.1.1</version>
        <relativePath/>
    </parent>

    <groupId>com.bsp</groupId>
    <artifactId>support-assistant-plsql</artifactId>
    <version>0.0.1-SNAPSHOT</version>
    <name>support-assistant-plsql</name>
    <description>Support assistant: a ticket API that calls an Oracle PL/SQL package. No JPA entities.</description>

    <properties>
        <java.version>21</java.version>
    </properties>

    <dependencies>
        <!-- Web: REST controllers, JSON, embedded Tomcat -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-webmvc</artifactId>
        </dependency>

        <!-- JDBC: the connection pool, JdbcTemplate, SimpleJdbcCall and @Transactional -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-jdbc</artifactId>
        </dependency>

        <!-- Oracle JDBC driver. Needed when the application runs, not when you compile -->
        <dependency>
            <groupId>com.oracle.database.jdbc</groupId>
            <artifactId>ojdbc17</artifactId>
            <scope>runtime</scope>
        </dependency>

        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-webmvc-test</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
            </plugin>
        </plugins>
    </build>
</project>
```

</details>

`src/main/resources/application.yaml` - Matches [../support-assistant-plsql/src/main/resources/application.yaml](../support-assistant-plsql/src/main/resources/application.yaml) exactly.

<details>
<summary>Full <code>src/main/resources/application.yaml</code></summary>

```yaml
spring:
  application:
    name: support-assistant-plsql

  datasource:
    # Oracle XE on your own machine. For another database, set DB_URL:
    # jdbc:oracle:thin:@//ORACLE_HOST:1521/ORACLE_SERVICE
    url: ${DB_URL:jdbc:oracle:thin:@//localhost:1521/XEPDB1}
    # Set DB_USERNAME and DB_PASSWORD as environment variables. Do not write a password in this file.
    username: ${DB_USERNAME}
    password: ${DB_PASSWORD}
```

</details>

`src/main/java/com/bsp/supportassistant/SupportAssistantApplication.java` - Matches [../support-assistant-plsql/src/main/java/com/bsp/supportassistant/SupportAssistantApplication.java](../support-assistant-plsql/src/main/java/com/bsp/supportassistant/SupportAssistantApplication.java) exactly.

<details>
<summary>Full <code>src/main/java/com/bsp/supportassistant/SupportAssistantApplication.java</code></summary>

```java
package com.bsp.supportassistant;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class SupportAssistantApplication {

    public static void main(String[] args) {
        SpringApplication.run(SupportAssistantApplication.class, args);
    }
}
```

</details>

## Common mistakes

| Symptom | Cause | Fix |
|---|---|---|
| First call answers `500`, and the log shows `ORA-01017: invalid username/password; logon denied` | `DB_USERNAME` or `DB_PASSWORD` is wrong, or not set. An unset variable is read as the text `${DB_PASSWORD}` | Set both variables in the run configuration or the terminal |
| `ORA-12541: Cannot connect. No listener at host localhost port 1599` | The host or port in `DB_URL` is wrong, or the database is not running | Check the host and port with your database team |
| `ORA-12514: ... Service NOSUCHSERVICE is not registered with the listener` | The service name in `DB_URL` is wrong | Use the service name, not the SID |

Next: **[Call a function](04-call-a-function.md)**.
