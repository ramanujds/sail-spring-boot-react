# Spring Boot Project Structure

A new Spring Boot project comes with a handful of files and folders. This note tells you what each one is for, which ones you will touch, and where your own classes go.

## What you will learn

- What every file in a new project does.
- Which files you edit, and which you leave alone.
- Where to put the classes you write, and why the location matters.

---

## 1. What a new project looks like

Generate a project with Spring Initializr (the generator in IntelliJ, or `start.spring.io`) using these values:

| Field | Value |
|---|---|
| Project | Maven |
| Language | Java |
| Group | `com.helpdeskpro` |
| Artifact | `helpdesk-pro` |
| Packaging | Jar |
| Java | 21 |
| Dependencies | Spring Web |

This is what you get:

```text
helpdesk-pro/
├── .mvn/wrapper/maven-wrapper.properties
├── mvnw
├── mvnw.cmd
├── .gitignore
├── .gitattributes
├── HELP.md
├── pom.xml
└── src/
    ├── main/
    │   ├── java/com/helpdeskpro/HelpdeskProApplication.java
    │   └── resources/application.properties
    └── test/
        └── java/com/helpdeskpro/HelpdeskProApplicationTests.java
```

After your first build, a `target/` folder appears next to `src/`.

> **Note:** The version numbers in your `pom.xml` may differ from the ones in this note. The structure is the same.

---

## 2. What each part does

| Part | What it is | Do you edit it? |
|---|---|---|
| `pom.xml` | The project description: name, Java version, libraries | Yes, when you add a library |
| `src/main/java` | Your Java code | Yes. This is where you work |
| `src/main/resources` | Settings and other non-code files | Yes, `application.properties` |
| `src/test/java` | Your tests | Yes, from Day 4 |
| `target/` | The build output (`.class` files and the `.jar`) | Never. `mvn clean` deletes it |
| `.mvn/`, `mvnw`, `mvnw.cmd` | The Maven wrapper, a script that downloads its own Maven | No |
| `.gitignore` | Tells Git which files to skip, such as `target/` | Rarely |
| `.gitattributes` | Git line-ending settings | No |
| `HELP.md` | Links to Spring documentation | No. You can delete it |

> **Note:** In IntelliJ, set the Maven home path to **Bundled (Maven 3)** and you will never use the wrapper files. See [IntelliJ: use the bundled Maven](../day-01-java-maven-spring-boot/intellij-maven-home-path.md).

---

## 3. The `pom.xml`

The generated file has four parts that matter.

```xml
<parent>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-parent</artifactId>
    <version>4.1.1</version>
</parent>

<groupId>com.helpdeskpro</groupId>
<artifactId>helpdesk-pro</artifactId>
<version>0.0.1-SNAPSHOT</version>

<properties>
    <java.version>21</java.version>
</properties>

<dependencies>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-webmvc</artifactId>
    </dependency>
</dependencies>
```

| Part | Meaning |
|---|---|
| `parent` | Picks matching versions for hundreds of libraries, so you do not choose them one by one |
| `groupId`, `artifactId`, `version` | The name tag of your project |
| `java.version` | The Java version Maven builds for |
| `dependencies` | The libraries your project uses |

A **starter** is a single dependency that pulls in everything for one job. `spring-boot-starter-webmvc` brings a web server and what you need to write REST APIs. You will meet more starters, such as one for JPA on Day 2 and one for security on Day 6.

> **Note:** Spring Boot 3.x calls this starter `spring-boot-starter-web`. It does the same job.

The generated file also has empty tags: `<description/>`, `<url/>`, `<licenses>`, `<developers>`, and `<scm>`. They do nothing. You can delete them.

---

## 4. The main class

```java
package com.helpdeskpro;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class HelpdeskProApplication {

    public static void main(String[] args) {
        SpringApplication.run(HelpdeskProApplication.class, args);
    }
}
```

- **`main`** is where Java starts, as in any Java program.
- **`SpringApplication.run`** starts Spring Boot, which starts the web server.
- **`@SpringBootApplication`** marks this class as the starting point and tells Spring where to look for your classes (see section 6).

You do not add code to this class. It stays this small.

---

## 5. Resources and tests

**`application.properties`** holds settings as `key=value` lines. A new project has one line:

```properties
spring.application.name=helpdesk-pro
```

Later you add the port, the database address, and more here. A settings file can also be written as `application.yml`. It means the same thing. HelpDesk Pro uses `.yml` once there are many settings.

**The test class** is generated for you:

```java
@SpringBootTest
class HelpdeskProApplicationTests {

    @Test
    void contextLoads() {
    }
}
```

`contextLoads` starts the whole application and checks that every part can be created. It has no code of its own. If it fails, something in your project is wired wrongly. It runs every time you run `mvn package` or `mvn test`.

---

## 6. Where your classes go

Your classes live in `src/main/java`, in the **same package as the main class, or in a package below it**. Spring Boot only looks there.

For HelpDesk Pro, the base package is `com.helpdeskpro`. You add sub-packages, one for each kind of class:

```text
com.helpdeskpro
├── HelpdeskProApplication     starts the application
├── domain                     Ticket, User, Comment             (Day 2)
├── repository                 TicketRepository, ...             (Day 2)
├── dto                        TicketRequestDTO, ...             (Day 3)
├── web                        TicketController                  (Day 3)
├── exception                  ApiError and error handling       (Day 3)
├── service                    TicketService                     (Day 4)
├── security                   login and token classes           (Day 6)
└── config                     settings classes, when needed
```

| Package | What lives there | One-line job |
|---|---|---|
| `domain` | The classes that match your tables | What the data is |
| `repository` | Interfaces that read and write the database | How to store and find it |
| `service` | The rules, such as which status moves are allowed | What the business does |
| `web` | Controllers | How the outside world asks |
| `dto` | The shapes sent and received over the API | What goes in and out |

### The scanning rule

Spring Boot finds your classes by **scanning**: it looks at the main class's package and everything below it. A class outside that package is invisible to Spring.

A controller in `com.helpdeskpro.web` works. A controller in `com.other` is not found:

| Request | Controller's package | Result |
|---|---|---|
| `GET /tickets` | `com.helpdeskpro.web` | `200` |
| `GET /outside` | `com.other` | `404` |

The application starts normally. It just does not know the class exists. This is one of the most common beginner mistakes.

---

## 7. Try it

1. Generate the project with the values from section 1 and open it in IntelliJ.
2. Open `pom.xml` and find the `parent`, `java.version`, and the starter.
3. Run `HelpdeskProApplication`. In the log, find the line `Tomcat started on port 8080`.
4. Create a package `com.helpdeskpro.web` and add this class:

   ```java
   package com.helpdeskpro.web;

   import org.springframework.web.bind.annotation.GetMapping;
   import org.springframework.web.bind.annotation.RestController;

   @RestController
   public class HelloController {

       @GetMapping("/hello")
       public String hello() {
           return "Hello from HelpDesk Pro!";
       }
   }
   ```

5. Restart the application and open `http://localhost:8080/hello`.

   **You should see:** `Hello from HelpDesk Pro!`

6. Move `HelloController` into a new package called `com.other`. Restart and open `/hello` again.

   **You should see:** a `404` error page. The application started, but Spring did not find the class.

7. Move the class back into `com.helpdeskpro.web` and check that `/hello` works again.

---

## Common mistakes

| Symptom | Cause | Fix |
|---|---|---|
| `404` for a URL you wrote a method for | The class is outside the main class's package | Move it into `com.helpdeskpro` or a sub-package |
| `404` for a URL, and the class is in the right package | A missing `@RestController`, or a mistyped path | Check the annotation and the path in `@GetMapping` |
| Everything under `target/` is red or missing | You ran `mvn clean` and have not built again | Run `mvn package` or run the application again |
| A new library is not recognised | The `pom.xml` change is not loaded | In IntelliJ click **Reload All Maven Projects** |
| `Port 8080 was already in use` | An earlier run is still going | Stop it, then start again |

## Practice

1. Add the packages `domain`, `repository`, `service`, and `dto` under `com.helpdeskpro`. Put one empty class in each. Name them `Ticket`, `TicketRepository`, `TicketService`, and `TicketRequestDTO`.
2. Delete the empty tags from `pom.xml` and run `mvn clean package`. Check it still ends with `BUILD SUCCESS`.
3. Change `server.port` in `application.properties` to `9090`. Restart and call `/hello` on the new port.

## Recap

- `pom.xml` describes the project, `src/main/java` holds your code, `src/main/resources` holds settings, and `target/` is build output you never edit.
- A **starter** is one dependency that brings everything for one job.
- `@SpringBootApplication` marks the starting class and tells Spring where to scan.
- Put your classes in the main class's package or below it, in sub-packages named for their job.
