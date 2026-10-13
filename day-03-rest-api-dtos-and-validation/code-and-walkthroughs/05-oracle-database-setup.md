# Set up the Oracle database

Before the application can store tickets, there must be a database to store them in, with a `TICKETS` table. This note gets you to that point. The next note, [06-spring-data-jpa-with-oracle.md](06-spring-data-jpa-with-oracle.md), connects the application to it.

You need **one** database. Pick the option that matches your situation:

| | Option A | Option B |
|---|---|---|
| **Database** | Oracle Database 19c | Oracle Database 21c Express Edition (XE) |
| **Where it runs** | On your organization's network | On your own machine, in a Docker container |
| **Who creates the user** | Your database team | The container, when it starts |
| **Use it when** | You have been given a 19c database | You want a database on your own machine |

The application code is the same for both. Only three values differ (section 1).

## What you will do

- Get a database you can log in to.
- Create the `TICKETS` table.
- Write down the three values the application needs.

---

## 1. What the application needs

The application connects with three values. They go into `application.yaml` in the next note.

| Value | Setting in `application.yaml` | Meaning |
|---|---|---|
| Where the database is | `url` | A JDBC URL: `jdbc:oracle:thin:@//HOST:PORT/SERVICE` |
| Who you log in as | `username` | The database user that will own the `TICKETS` table |
| Its password | `password` | Keep your real password out of Git (see the warning in the next note) |

The URL has three parts:

| Part | Meaning | Option A | Option B |
|---|---|---|---|
| `HOST` | The machine that runs the database | `ORACLE_HOST` | `localhost` |
| `PORT` | The listener's port | Usually `1521` | `1521` |
| `SERVICE` | The name of the database service to connect to | `ORACLE_SERVICE` | `XEPDB1` |

> **Note:** A direct database connection does not go through the company proxy. If it fails, the cause is the host, the port, a firewall, or the login.

---

## Option A: Oracle 19c on your network

### Step A1: get the connection details

Ask your database team for:

- the **host**
- the **port**
- the **service name**
- a **user** and **password**, with permission to create a table

**Check:** you have all five values written down.

### Step A2: check that you can log in

In SQL Developer, create a connection with the host, port, and **Service name**. Or use SQL\*Plus, which asks for the password:

```bash
sqlplus YOUR_USERNAME@//ORACLE_HOST:1521/ORACLE_SERVICE
```

**Check:** you see the `SQL>` prompt. Run this and look for `connected`:

```sql
SELECT 'connected' FROM DUAL;
```

Go to section 3 to create the table.

---

## Option B: Oracle XE 21 in Docker

This option needs Docker on your machine and the image `gvenzl/oracle-xe:21-slim`.

### Step B1: start the container

```bash
docker run -d --name support-oracle -p 1521:1521 \
  -e ORACLE_PASSWORD=YOUR_SYS_PASSWORD \
  -e APP_USER=support \
  -e APP_USER_PASSWORD=YOUR_PASSWORD \
  gvenzl/oracle-xe:21-slim
```

| Part | Meaning |
|---|---|
| `-d` | Run in the background |
| `--name support-oracle` | A name you can use in later commands |
| `-p 1521:1521` | Make the database's port 1521 available on your machine |
| `ORACLE_PASSWORD` | The password of the administrator users `SYS` and `SYSTEM` |
| `APP_USER`, `APP_USER_PASSWORD` | A user for your application, created in the service `XEPDB1` |

Replace `YOUR_SYS_PASSWORD` and `YOUR_PASSWORD` with passwords of your own. You will write the second one in `application.yaml`.

> **Note:** On Windows Command Prompt, write the command on one line.

**Check:** `docker ps` lists `support-oracle`.

### Step B2: wait until the database is ready

The first start takes a while, because the database creates itself.

```bash
docker logs -f support-oracle
```

**Check:** the log ends with `DATABASE IS READY TO USE!`. Press **Ctrl + C** to stop following the log. The container keeps running.

### Step B3: check that you can log in

```bash
docker exec -it support-oracle sqlplus support/YOUR_PASSWORD@//localhost:1521/XEPDB1
```

**Check:** you see the `SQL>` prompt. Run `SELECT 'connected' FROM DUAL;` and look for `connected`. Type `EXIT` to leave.

### Looking after the container

| Command | What it does |
|---|---|
| `docker stop support-oracle` | Stops the database. The data is kept |
| `docker start support-oracle` | Starts it again, with the same data. Wait for the ready line again |
| `docker rm -f support-oracle` | Deletes the container **and its data**. Start again from step B1 |

---

## 3. Create the `TICKETS` table

You create the table yourself with SQL. The application only **checks** that it matches the Java class. It never creates or changes tables.

The script is [db/oracle/schema.sql](support-assistant-jpa/db/oracle/schema.sql):

```sql
CREATE TABLE TICKETS (
    ID           VARCHAR2(36)   PRIMARY KEY,
    TITLE        VARCHAR2(100)  NOT NULL,
    DESCRIPTION  VARCHAR2(500),
    STATUS       VARCHAR2(20)   NOT NULL,
    PRIORITY     VARCHAR2(10)   NOT NULL
);
```

| Column | Holds | Why this type |
|---|---|---|
| `ID` | The ticket's UUID, as text | A UUID is 36 characters, for example `92818c23-79b6-4d51-b5c5-617cd067fa71`. As text, you can read it and search for it |
| `TITLE` | The title | `NOT NULL`: a ticket must have one |
| `DESCRIPTION` | The description | Optional, so it may be empty |
| `STATUS` | `OPEN`, `IN_PROGRESS`, `RESOLVED`, or `CLOSED` | Text. The longest value is 11 characters |
| `PRIORITY` | `LOW`, `MEDIUM`, or `HIGH` | Text |

`PRIMARY KEY` means no two rows can have the same `ID`. `NOT NULL` means the column must have a value. `VARCHAR2` is Oracle's text type.

### Run the script

**Option A:** in SQL Developer, open the file and run it as a script with **F5**. Or from the project folder:

```bash
sqlplus YOUR_USERNAME@//ORACLE_HOST:1521/ORACLE_SERVICE @db/oracle/schema.sql
```

**Option B:** from the `support-assistant-jpa` folder:

```bash
docker exec -i support-oracle sqlplus -s support/YOUR_PASSWORD@//localhost:1521/XEPDB1 < db/oracle/schema.sql
```

**Check:** you should see:

```text
Table created.
```

Then look at the table. At the `SQL>` prompt, run `DESC TICKETS`:

```text
 Name                                      Null?    Type
 ----------------------------------------- -------- ----------------------------
 ID                                        NOT NULL VARCHAR2(36)
 TITLE                                     NOT NULL VARCHAR2(100)
 DESCRIPTION                                        VARCHAR2(500)
 STATUS                                    NOT NULL VARCHAR2(20)
 PRIORITY                                  NOT NULL VARCHAR2(10)
```

> **Warning:** Run the script once. A second run stops with `ORA-00955: name is already used by an existing object`. This means the table is already there. Nothing is wrong.

---

## 4. Write down your three values

| Setting | Option A | Option B |
|---|---|---|
| `url` | `jdbc:oracle:thin:@//ORACLE_HOST:1521/ORACLE_SERVICE` | `jdbc:oracle:thin:@//localhost:1521/XEPDB1` |
| `username` | `YOUR_USERNAME` | `support` |
| `password` | `YOUR_PASSWORD` | the `APP_USER_PASSWORD` you chose |

Use your real host, service name, user, and password in place of the placeholders. The [application.yaml](support-assistant-jpa/src/main/resources/application.yaml) in the project already has both options, and the next note shows how to choose one.

---

## Common messages

| Message | Cause | Fix |
|---|---|---|
| `ORA-01017: invalid username/password; logon denied` | A wrong user or password | Check both. For Option B, they are the `APP_USER` values |
| `ORA-12514: Cannot connect to database. Service ... is not registered with the listener` | The service name is wrong | Option A: ask your database team. Option B: use `XEPDB1` |
| `ORA-12541: Cannot connect. No listener at host ... port ...` | The host or port is wrong, or the database is not running | Check them. Option B: run `docker ps` and wait for the ready line |
| `ORA-00955: name is already used by an existing object` | The table already exists | Nothing to do |
| `ORA-01031: insufficient privileges` | The user may not create tables | Option A: ask your database team |
| `Cannot connect to the Docker daemon` | Docker is not running | Start Docker, then repeat the command |

## Check yourself

- [ ] You can log in with SQL\*Plus or SQL Developer and see the `SQL>` prompt.
- [ ] `DESC TICKETS` lists five columns.
- [ ] You can write the `url` for your database, and say which part is the host, the port, and the service name.
- [ ] You know that the application only checks the table, and that you create it.
