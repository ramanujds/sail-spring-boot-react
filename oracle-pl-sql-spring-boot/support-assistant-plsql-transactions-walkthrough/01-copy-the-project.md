# Step 1 — Copy the project

> [Back to index](README.md) · Previous: none · Next: [The error log](02-the-error-log.md)

## Goal

A new project, `support-assistant-plsql-transactions`, that starts as an exact copy of `support-assistant-plsql` and passes the same four tests.

## Why this matters

The first project works. You keep it as it is, so that you can compare the two at any time. Everything in the first project (the package calls, the error handler, the bulk change) stays in the copy, and you do not write it again.

### 1.1 Copy the folder

In the folder that holds `support-assistant-plsql`:

```bash
cp -r support-assistant-plsql support-assistant-plsql-transactions
cd support-assistant-plsql-transactions
rm -rf target
```

On Windows, copy the folder in Explorer, and delete the `target` folder in the copy. `target` holds the build output of the first project. Maven builds it again.

### 1.2 Give the copy its own name

Change three values. The JAR file takes its name from `artifactId`, so two projects with the same `artifactId` would build files with the same name.

| File | Line | Old value | New value |
|---|---|---|---|
| `pom.xml` | `<artifactId>` under `<groupId>com.bsp</groupId>` | `support-assistant-plsql` | `support-assistant-plsql-transactions` |
| `pom.xml` | `<name>` | `support-assistant-plsql` | `support-assistant-plsql-transactions` |
| `src/main/resources/application.yaml` | `name:` under `application:` | `support-assistant-plsql` | `support-assistant-plsql-transactions` |

You can also change `<description>`. Leave the `<artifactId>` inside `<parent>` (`spring-boot-starter-parent`) as it is.

### 1.3 Open it in IntelliJ

Choose **File → Open**, and pick the `support-assistant-plsql-transactions` folder. Open it as a new project, in a new window. Check that the Maven home path is **Bundled (Maven 3)**, as on Day 1.

## Try it

```bash
export DB_USERNAME=YOUR_USERNAME
export DB_PASSWORD=YOUR_PASSWORD
mvn clean verify
```

Expected:

```text
[INFO] Tests run: 4, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

and a new file `target/support-assistant-plsql-transactions-0.0.1-SNAPSHOT.jar`.

## Checkpoint

`pom.xml` - Matches [../support-assistant-plsql-transactions/pom.xml](../support-assistant-plsql-transactions/pom.xml) exactly.

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
    <artifactId>support-assistant-plsql-transactions</artifactId>
    <version>0.0.1-SNAPSHOT</version>
    <name>support-assistant-plsql-transactions</name>
    <description>Support assistant: transaction management around calls to an Oracle PL/SQL package.</description>

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

`src/main/resources/application.yaml` - Matches [../support-assistant-plsql-transactions/src/main/resources/application.yaml](../support-assistant-plsql-transactions/src/main/resources/application.yaml) exactly.

<details>
<summary>Full <code>src/main/resources/application.yaml</code></summary>

```yaml
spring:
  application:
    name: support-assistant-plsql-transactions

  datasource:
    # Oracle XE on your own machine. For another database, set DB_URL:
    # jdbc:oracle:thin:@//ORACLE_HOST:1521/ORACLE_SERVICE
    url: ${DB_URL:jdbc:oracle:thin:@//localhost:1521/XEPDB1}
    # Set DB_USERNAME and DB_PASSWORD as environment variables. Do not write a password in this file.
    username: ${DB_USERNAME}
    password: ${DB_PASSWORD}
```

</details>

Every other file is the same as in [support-assistant-plsql](../support-assistant-plsql/).

## Common mistakes

| Symptom | Cause | Fix |
|---|---|---|
| The JAR in `target` is still called `support-assistant-plsql-0.0.1-SNAPSHOT.jar` | `<artifactId>` was not changed, or the parent's `<artifactId>` was changed in its place | Change the `<artifactId>` next to `<groupId>com.bsp</groupId>` |
| `Non-resolvable parent POM` | The parent's `<artifactId>` was changed | Put back `spring-boot-starter-parent` |
| The first call to the database fails with `ORA-01017` | `DB_USERNAME` or `DB_PASSWORD` is not set in this terminal | Set both, in the terminal that runs Maven |
| IntelliJ shows the old project name | The folder was opened in the window of the first project | Open the new folder as a new project |

Next: **[The error log](02-the-error-log.md)**.
