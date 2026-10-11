# Setup checklist

Complete this checklist before Session 1 starts. Each item has a command to run and the result you should see, so you can tell in under a minute whether a tool is installed and working.

Install anything missing from your organization's approved software source. Nothing in this programme needs a cloud account or a container runtime.

## Checklist

Tick each box once its check passes.

- [ ] **1.** JDK 21
- [ ] **2.** Maven 3.9 or later, running on Java 21
- [ ] **3.** Git
- [ ] **4.** IDE with Java and Spring support
- [ ] **5.** Oracle client tool (SQL Developer or SQL*Plus)
- [ ] **6.** Network path to the on-prem Oracle instance
- [ ] **7.** Oracle schema login and `SELECT 1 FROM DUAL`
- [ ] **8.** `curl` (or any HTTP client) for calling endpoints

```mermaid
flowchart LR
    J["1. JDK 21"] --> M["2. Maven"]
    M --> G["3. Git"]
    G --> I["4. IDE"]
    I --> C["5. Oracle client"]
    C --> N["6. Network path"]
    N --> L["7. Schema login"]
```

How to read it: work top to bottom. Maven depends on the JDK, and the Oracle checks depend on each other, so a failure early on explains failures after it.

> **Note:** Open a **new** terminal window after installing anything, so the updated `PATH` and `JAVA_HOME` are picked up.

---

## 1. JDK 21

**Check**

```bash
java -version
javac -version
```

**Expected:** both commands report version 21, for example `openjdk version "21.0.x"` and `javac 21.0.x`.

`javac` matters: it confirms you have a full JDK, not only a runtime.

**Deeper check (also a first look at the single-file launch you use in Session 1).** Create a file `Check.java`:

```java
public class Check {
    record Probe(String name, int value) {}

    public static void main(String[] args) {
        System.out.println("Java feature release: " + Runtime.version().feature());
        System.out.println(new Probe("records work", 1));
    }
}
```

```bash
java Check.java
```

**Expected:**

```text
Java feature release: 21
Probe[name=records work, value=1]
```

| Symptom | Likely cause | Fix |
|---|---|---|
| `command not found` / `'java' is not recognized` | JDK not installed, or not on `PATH` | Install JDK 21 and open a new terminal |
| Reports `1.8.0_xxx`, `11`, or `17` | An older JDK comes first on `PATH` | Point `JAVA_HOME` and `PATH` at JDK 21 |
| `java` works but `javac` is missing | Only a JRE is installed | Install a JDK, not a JRE |
| `class file version 65.0` error | The `java` launcher is older than 21 | Same as the older-version row |

---

## 2. Maven 3.9 or later

**Check**

```bash
mvn -version
```

**Expected:** Maven 3.9 or later, and the `Java version:` line reports 21.

```text
Apache Maven 3.9.x
Maven home: ...
Java version: 21.0.x, vendor: ...
```

| Symptom | Likely cause | Fix |
|---|---|---|
| `command not found` | Maven not installed or not on `PATH` | Install Maven and add its `bin` folder to `PATH` |
| Maven below 3.9 | Older Maven earlier on `PATH` | Install 3.9+ and update `PATH` |
| `Java version: 17` (or 11, 8) | Maven uses the `JAVA_HOME` JDK, not the one `java -version` shows | Set `JAVA_HOME` to the JDK 21 folder |
| `JAVA_HOME is not defined correctly` | `JAVA_HOME` points at a missing or JRE-only folder | Point it at the JDK 21 install folder |

Check `JAVA_HOME` directly:

```bash
echo $JAVA_HOME          # macOS / Linux
echo %JAVA_HOME%         # Windows cmd
```

---

## 3. Git

**Check**

```bash
git --version
git config user.name
git config user.email
```

**Expected:** a version number, then your name and email.

If the last two print nothing, set them once:

```bash
git config --global user.name "Your Name"
git config --global user.email "you@your-org.example"
```

---

## 4. IDE with Java and Spring support

Use IntelliJ IDEA or VS Code.

**Check**

| IDE | How to check | Expected |
|---|---|---|
| IntelliJ IDEA | Open **File > Project Structure > SDKs** (or **Platform Settings > SDKs**) | JDK 21 is listed |
| IntelliJ IDEA | Open **Settings > Build Tools > Maven** | Maven home points at 3.9+; the Maven runner JRE is 21 |
| VS Code | Open the Extensions view and search **Extension Pack for Java** and **Spring Boot Extension Pack** | Both show as installed |
| VS Code | Run **Java: Configure Java Runtime** from the Command Palette | JDK 21 is detected |

**Final IDE test:** open the folder containing `Check.java` from step 1 and run it from the IDE. You should see the same two lines of output.

---

## 5. Oracle client tool

You need one of these two. SQL Developer is the easier choice if you are unsure.

| Tool | Check | Expected |
|---|---|---|
| SQL Developer | Launch the application | The window opens and shows a **Connections** panel |
| SQL*Plus | `sqlplus -V` | `SQL*Plus: Release 19.x.x.x.x` (or similar) |

| Symptom | Likely cause | Fix |
|---|---|---|
| `sqlplus: command not found` | Oracle client not installed or not on `PATH` | Install the Oracle client, or use SQL Developer |
| SQL Developer asks for a JDK on first launch | It needs a JDK path | Point it at your JDK 21 folder |

---

## 6. Network path to the on-prem Oracle instance

Before you try to log in, confirm your machine can reach the database host and port. Get the host, port, and service name or SID from your organization's database team.

**Check** (replace the placeholders):

```bash
# macOS / Linux
nc -vz <db-host> <db-port>

# Windows PowerShell
Test-NetConnection <db-host> -Port <db-port>
```

**Expected:**

- `nc`: `Connection to <db-host> port <db-port> [tcp/*] succeeded!`
- PowerShell: `TcpTestSucceeded : True`

| Symptom | Likely cause | Fix |
|---|---|---|
| `TcpTestSucceeded : False` or the command hangs | Not on the right network, VPN not connected, or a firewall rule | Connect to the organization network and ask the network team to allow your machine |
| `could not resolve host` | The hostname is wrong or local DNS is not in use | Re-check the hostname, or use the IP address you were given |
| `Connection refused` | Host reachable, but nothing listening on that port | Re-check the port with the database team |

---

## 7. Oracle schema login and `SELECT 1 FROM DUAL`

This is the check that matters most. Every later session depends on it.

**SQL Developer**

1. Create a new connection. Enter your schema user, password, host, port, and either **SID** or **Service name**, whichever the database team gave you.
2. Choose **Test**. The status should read `Success`.
3. Connect, open a worksheet, and run:

```sql
SELECT 1 FROM DUAL;
```

**SQL*Plus** (it prompts for the password, so it never appears in your shell history):

```bash
sqlplus <schema-user>@//<db-host>:<db-port>/<service-name>
```

```sql
SELECT 1 FROM DUAL;
```

> **Note:** If you were given an SID instead of a service name, use SQL Developer's **SID** option, or ask the database team for a `tnsnames.ora` entry.

**Expected:**

```text
         1
----------
         1
```

Also confirm which user and schema you are in:

```sql
SELECT USER FROM DUAL;
```

**Expected:** your schema name.

| Symptom | Likely cause | Fix |
|---|---|---|
| `ORA-01017: invalid username/password` | Wrong credentials | Re-check them with the database team |
| `ORA-12541: TNS:no listener` | Wrong host or port | Re-run step 6 and re-check the port |
| `ORA-12514` / `ORA-12505` | Wrong service name or SID | Ask the database team which one applies |
| `ORA-28000: the account is locked` | Too many failed logins, or not yet unlocked | Ask the database team to unlock it |
| Hangs, then times out | Network path blocked | Go back to step 6 |

---

## 8. `curl` (or another HTTP client)

You use this from Session 1 onward to call `/actuator/health`.

**Check**

```bash
curl --version
```

**Expected:** a version line such as `curl 8.x.x`.

> **Note:** On Windows PowerShell, `curl` can be an alias for `Invoke-WebRequest`. Use `curl.exe --version` to test the real tool.

---

## Quick all-in-one check

Run this in a new terminal (macOS / Linux). Anything that prints `MISSING` needs attention.

```bash
for tool in java javac mvn git curl sqlplus; do
  command -v "$tool" >/dev/null && echo "OK       $tool" || echo "MISSING  $tool"
done
java -version 2>&1 | head -1
mvn -version | head -3
```

`sqlplus` reports `MISSING` if you chose SQL Developer instead. That is fine.

---

## Needed later in the programme

You do not need these for Session 1, but checking them early avoids surprises.

| Tool | Needed from | Check | Expected |
|---|---|---|---|
| Postman (or equivalent HTTP client) | Day 3 | Launch it and send a `GET` to any URL | A response with a status code |
| SSH access to the shared on-prem Linux server | Day 8 | `ssh <user>@<server-host>` | A login prompt or shell on the server |
| Modern browser with developer tools | Day 9 | Press `F12` | The developer tools panel opens |
| Node.js (current LTS) and npm | Day 10 | `node -v` and `npm -v` | Version numbers for both |

---

## When everything passes

You are ready for Session 1 when all eight boxes are ticked and `SELECT 1 FROM DUAL` returns a row from the on-prem Oracle instance.

If a check still fails after the fix in its table, note the exact command and the full error text before asking for help. That is usually all that is needed to find the cause.
