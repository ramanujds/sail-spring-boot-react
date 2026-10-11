# Step 0 — Setup

> [Back to index](README.md) · Next: [Step 1 — How the lab works](01-how-the-lab-works.md)

## Goal

Build the project on JDK 21 and see the test suite fail for the right reason.

## Why this matters

The project is deliberately incomplete: it compiles, but 21 of its 22 tests fail until you implement the TODOs. Seeing that red run first confirms your toolchain works end to end, and gives you a baseline that every later step moves toward green.

A build error here (as opposed to failing tests) means the environment is wrong, not the code. It is much easier to fix now than after you have written anything.

## 1. Check the toolchain

```bash
java -version
mvn -version
```

**Expected:** `java -version` reports 21, and the `Java version:` line of `mvn -version` also reports 21. If either does not, use the [setup checklist](../../notes/01-dev-setup.md).

## 2. Open the project

```bash
cd session-01/code-and-walkthroughs/java-features-lab
```

In your IDE, open the `java-features-lab` folder as a Maven project (open the `pom.xml`). The IDE should detect JDK 21 and show the `com.helpdeskpro.lab` packages.

## 3. Compile

```bash
mvn -q compile
```

**Expected:** no output and a zero exit status. The first run downloads dependencies, so it takes longer.

## Try it

Run the whole suite:

```bash
mvn test
```

**Expected:** the build fails, and the summary near the end reads:

```text
[ERROR] Tests run: 22, Failures: 4, Errors: 17, Skipped: 0
```

Above it, the error lines carry messages such as `TODO 1.2: implement toSummaries`. Each message names the TODO you need to complete. One test passes already; it checks behaviour the record gives you for free.

## Checkpoint

Nothing for you to edit yet. The `pom.xml` is provided, so the listing is for reference: it sets the compiler release to 21 and adds JUnit 5 as the only dependency.

<details>
<summary>Full <code>pom.xml</code></summary>

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <groupId>com.helpdeskpro</groupId>
    <artifactId>java-features-lab</artifactId>
    <version>0.1.0-SNAPSHOT</version>
    <name>java-features-lab</name>
    <description>Practice project for Java 9 to 21 language and API features</description>

    <properties>
        <maven.compiler.release>21</maven.compiler.release>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
        <junit.version>5.11.4</junit.version>
    </properties>

    <dependencies>
        <dependency>
            <groupId>org.junit.jupiter</groupId>
            <artifactId>junit-jupiter</artifactId>
            <version>${junit.version}</version>
            <scope>test</scope>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-compiler-plugin</artifactId>
                <version>3.13.0</version>
            </plugin>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-surefire-plugin</artifactId>
                <version>3.5.2</version>
            </plugin>
        </plugins>
    </build>
</project>
```

</details>

This matches [../java-features-lab/pom.xml](../java-features-lab/pom.xml) exactly.

## Common mistakes

| Symptom | Cause | Fix |
|---|---|---|
| `error: release version 21 not supported` | Maven runs on an older JDK | Point `JAVA_HOME` at JDK 21 and open a new terminal |
| `mvn: command not found` | Maven is not installed or not on `PATH` | Install Maven 3.9 or later |
| Dependencies cannot be downloaded | Maven cannot reach its repository | Use your organization's Maven repository settings in `~/.m2/settings.xml` |
| The IDE shows unresolved imports for JUnit | The project was opened as plain folders | Re-import it as a Maven project |

Next: **[Step 1 — How the lab works](01-how-the-lab-works.md)**.
