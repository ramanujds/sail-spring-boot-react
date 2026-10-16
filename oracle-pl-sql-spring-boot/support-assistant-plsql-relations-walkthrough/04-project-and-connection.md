# Step 4 — Project and connection

> [Back to index](README.md) · Previous: [The package](03-the-package.md) · Next: [Ticket calls](05-ticket-calls.md)

## Goal

A Spring Boot application that starts, with the Oracle connection settings read from environment variables.

## Why this matters

Nothing here is new. The project is set up exactly as in the first walkthrough, so only the names change. The relationship adds no dependency: a foreign key is a rule inside Oracle, and Java does not need to know it exists.

### 4.1 Create the project

Use Spring Initializr with:

| Field | Value |
|---|---|
| Group | `com.bsp` |
| Artifact | `support-assistant-plsql-relations` |
| Java | 21 |
| Dependencies | Spring Web, JDBC API, Oracle Driver |

Do **not** add Spring Data JPA. Keep the base package `com.bsp.supportassistant`. The reason for each dependency is in [step 3 of the first walkthrough](../support-assistant-plsql-walkthrough/03-project-and-connection.md).

### 4.2 The connection

`src/main/resources/application.yaml` reads the user and the password from environment variables:

```yaml
spring:
  datasource:
    url: ${DB_URL:jdbc:oracle:thin:@//localhost:1521/XEPDB1}
    username: ${DB_USERNAME}
    password: ${DB_PASSWORD}
```

### 4.3 Set the variables

| Where you run | How |
|---|---|
| IntelliJ | **Run | Edit Configurations | Environment variables**: `DB_USERNAME=YOUR_USERNAME;DB_PASSWORD=YOUR_PASSWORD` |
| Terminal | `export DB_USERNAME=YOUR_USERNAME` and `export DB_PASSWORD=YOUR_PASSWORD` |

## Try it

Run `SupportAssistantApplication`.

```text
Started SupportAssistantApplication in 1.4 seconds
```

> **Note:** the application starts even when the password is wrong. Nothing connects to Oracle until the first call. A wrong password shows up as `ORA-01017` in step 5.

## Checkpoint

`pom.xml` - Matches [../support-assistant-plsql-relations/pom.xml](../support-assistant-plsql-relations/pom.xml) exactly.

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
    <artifactId>support-assistant-plsql-relations</artifactId>
    <version>0.0.1-SNAPSHOT</version>
    <name>support-assistant-plsql-relations</name>
    <description>Support assistant: tickets and their comments, through an Oracle PL/SQL package. No JPA entities.</description>

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

`src/main/resources/application.yaml` - Matches [../support-assistant-plsql-relations/src/main/resources/application.yaml](../support-assistant-plsql-relations/src/main/resources/application.yaml) exactly.

<details>
<summary>Full <code>src/main/resources/application.yaml</code></summary>

```yaml
spring:
  application:
    name: support-assistant-plsql-relations

  datasource:
    # Oracle XE on your own machine. For another database, set DB_URL:
    # jdbc:oracle:thin:@//ORACLE_HOST:1521/ORACLE_SERVICE
    url: ${DB_URL:jdbc:oracle:thin:@//localhost:1521/XEPDB1}
    # Set DB_USERNAME and DB_PASSWORD as environment variables. Do not write a password in this file.
    username: ${DB_USERNAME}
    password: ${DB_PASSWORD}
```

</details>

`src/main/java/com/bsp/supportassistant/SupportAssistantApplication.java` - Matches [../support-assistant-plsql-relations/src/main/java/com/bsp/supportassistant/SupportAssistantApplication.java](../support-assistant-plsql-relations/src/main/java/com/bsp/supportassistant/SupportAssistantApplication.java) exactly.

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
| `package org.springframework.jdbc.core does not exist` in the next step | The JDBC starter is missing | Add `spring-boot-starter-jdbc` |
| The application does not start: `Failed to load driver class oracle.jdbc.OracleDriver` | The Oracle driver is missing | Add `ojdbc17` |
| The application starts, then the first call fails with `ORA-01017` | `DB_USERNAME` or `DB_PASSWORD` is not set in the place where you run, or the password is wrong | Set them in the run configuration, or `export` them in the terminal that starts Maven |

Next: **[Ticket calls](05-ticket-calls.md)**.
