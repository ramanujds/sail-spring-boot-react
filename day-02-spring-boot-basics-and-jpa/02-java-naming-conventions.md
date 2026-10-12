# Java Naming Conventions

A **naming convention** is an agreement on how names are written. Java does not force you to follow it, but every Java developer expects it. Following it makes your code easy for other people, and for you next week, to read.

## What you will learn

- The naming rule for each kind of name in Java.
- The names Spring projects use, such as `TicketService`.
- How Java names, database names, and URLs differ for the same idea.

---

## 1. The core rules

| What | Rule | Example |
|---|---|---|
| **Class** | `PascalCase`, a noun | `Ticket`, `TicketService` |
| **Interface** | `PascalCase`, a noun, no `I` prefix | `TicketRepository` |
| **Enum** | `PascalCase`, a noun | `TicketPriority` |
| **Enum value** | `UPPER_SNAKE_CASE` | `IN_PROGRESS` |
| **Method** | `camelCase`, starts with a verb | `findAll()`, `pickUp()` |
| **Variable and field** | `camelCase`, a noun | `ticketId`, `assignedTo` |
| **Constant** | `UPPER_SNAKE_CASE`, `static final` | `MAX_TITLE_LENGTH` |
| **Package** | all lowercase, no underscores | `com.helpdeskpro.service` |
| **File** | Exactly the public class name, plus `.java` | `Ticket.java` |

How the case styles look:

| Style | Look | Rule |
|---|---|---|
| `PascalCase` | `TicketService` | Every word starts with a capital letter |
| `camelCase` | `assignedTo` | The first word is lowercase, the rest start with a capital |
| `UPPER_SNAKE_CASE` | `MAX_TITLE_LENGTH` | All capitals, words joined with `_` |

---

## 2. See it in one class

```java
public class Ticket {

    private static final int MAX_TITLE_LENGTH = 100;

    private String title;
    private boolean overdue;
    private TicketPriority priority;

    public Ticket(String title, TicketPriority priority) {
        this.title = title;
        this.priority = priority;
    }

    public boolean isOverdue() {
        return overdue;
    }

    public boolean hasValidTitle() {
        return title != null && title.length() <= MAX_TITLE_LENGTH;
    }

    public String getTitle() {
        return title;
    }
}
```

```java
public enum TicketPriority {
    LOW, MEDIUM, HIGH
}
```

| Name | Why it is written that way |
|---|---|
| `Ticket`, `TicketPriority` | Classes and enums are `PascalCase` nouns |
| `MAX_TITLE_LENGTH` | A constant is `UPPER_SNAKE_CASE` |
| `title`, `overdue`, `priority` | Fields are `camelCase` nouns |
| `isOverdue()`, `hasValidTitle()` | A method that returns `true` or `false` starts with `is` or `has` |
| `getTitle()` | A method that returns a field starts with `get` |
| `LOW`, `MEDIUM`, `HIGH` | Enum values are `UPPER_SNAKE_CASE` |

---

## 3. Names you will see in Spring projects

Spring projects add a **suffix** that tells you the job of a class:

| Suffix | Job | Example | Package |
|---|---|---|---|
| `Controller` | Receives web requests | `TicketController` | `web` |
| `Service` | Holds the rules | `TicketService` | `service` |
| `Repository` | Reads and writes the database | `TicketRepository` | `repository` |
| `DTO` | A shape sent or received over the API | `TicketRequestDTO`, `TicketResponseDTO` | `dto` |
| `Exception` | An error with a meaning | `TicketNotFoundException` | `exception` |
| `Config` | Settings written as code | `SecurityConfig` | `config` |
| `Application` | The class that starts the application | `HelpdeskProApplication` | the base package |

The suffix lets you find the right class without opening it. You see `TicketService` and you know it is where the ticket rules live.

> **Note:** Some teams write `TicketRequestDto`. HelpDesk Pro writes `DTO` in capitals, and uses it the same way everywhere.

---

## 4. Same idea, different style

One idea has a different spelling in each place. The rule for each place is different.

| Place | Style | The "raised by" column |
|---|---|---|
| Java field | `camelCase` | `raisedBy` |
| Database column (Oracle) | `UPPER_SNAKE_CASE` | `RAISED_BY` |
| JSON sent by the API | `camelCase` | `"raisedBy"` |
| Java class and the table it maps to | `Ticket` and `TICKETS` | singular class, plural table |
| URL path | lowercase, plural nouns | `/tickets` |
| URL path with two words | lowercase, joined with `-` | `/pick-up` |
| Maven `artifactId` | lowercase, joined with `-` | `helpdesk-pro` |
| Maven `groupId` and package | lowercase, reversed domain | `com.helpdeskpro` |

You will not need to convert between these by hand. On Day 2 you will see how JPA connects `raisedBy` to `RAISED_BY`.

---

## 5. Good names and poor names

| Poor name | Problem | Better name |
|---|---|---|
| `ticket_service` | A class is not `snake_case` | `TicketService` |
| `ticketservice` | Words are not separated | `TicketService` |
| `Ticket1`, `Data`, `Manager` | Says nothing about the job | `TicketRequestDTO`, `TicketService` |
| `ITicketRepository` | The `I` prefix is not used in Java | `TicketRepository` |
| `Ticket.getAllTheTickets()` | Too long, repeats the class name | `findAll()` |
| `status()` | A method should be an action | `getStatus()` |
| `maxTitleLength` as a constant | A constant is all capitals | `MAX_TITLE_LENGTH` |
| `com.HelpDeskPro.Service` | A package is all lowercase | `com.helpdeskpro.service` |
| `x`, `tmp`, `t` | Cannot be understood later | `ticket`, `assignedAgent` |
| `getOverdue()` for a true/false value | A true/false getter starts with `is` | `isOverdue()` |

### Short rules to remember

- Name things for **what they are** (a noun), and actions for **what they do** (a verb).
- A longer, clear name is better than a short, unclear one.
- Be consistent. If one class is `TicketService`, the next is `CommentService`, not `ServiceForComment`.
- Write names in English, without abbreviations that only you understand.

---

## 6. Try it

1. Create a folder with three files: `TicketPriority.java`, `Ticket.java`, and `NamingDemo.java`. Use the classes from section 2 for the first two.
2. Add this class:

   ```java
   public class NamingDemo {
       public static void main(String[] args) {
           Ticket ticket = new Ticket("Cannot print", TicketPriority.HIGH);
           System.out.println(ticket.getTitle() + " valid=" + ticket.hasValidTitle() + " overdue=" + ticket.isOverdue());
       }
   }
   ```

3. Compile and run:

   ```bash
   javac *.java
   java NamingDemo
   ```

   **You should see:**

   ```text
   Cannot print valid=true overdue=false
   ```

4. Rename the file `Ticket.java` to `ticket.java` and compile again.

   **You should see:** an error. The file name must match the class name exactly, including the capital letter.

---

## Common mistakes

| Symptom | Cause | Fix |
|---|---|---|
| `class Ticket is public, should be declared in a file named Ticket.java` | The file name and class name differ | Rename the file or the class so they match |
| Spring does not find a class | The package name has capitals or is outside the base package | Use lowercase and keep it under `com.helpdeskpro` |
| A boolean getter does not work as expected | Named `getOverdue()` or `overdue()` | Name it `isOverdue()` |
| Code is hard to read after a week | Names like `data`, `temp`, `x` | Rename each to say what it holds |

## Practice

1. Rename each of these to follow the convention:
   - the class `ticket_comment`
   - the method `GetAssignedAgent()`
   - the constant `defaultPageSize`
   - the package `com.HelpDeskPro.DTO`
   - the interface `IUserRepository`
   - the enum value `inProgress`
2. Write the Java name, the Oracle column name, and the URL path for each of these ideas: "due by", "assigned to", and "pick up a ticket".
3. Look at the classes in your project. Find one name that could be clearer, and rename it with IntelliJ's rename (**Shift + F6**).

## Recap

- Classes are `PascalCase`, methods and variables are `camelCase`, constants and enum values are `UPPER_SNAKE_CASE`, and packages are lowercase.
- A file is named exactly like its public class.
- Spring projects add a suffix (`Controller`, `Service`, `Repository`, `DTO`) that tells you the class's job.
- The same idea is spelled differently in Java, the database, JSON, and URLs.
- Clear, consistent names matter more than short ones.
