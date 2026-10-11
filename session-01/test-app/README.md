# Test app: Spring Boot 3.5 + Oracle

A small app to check that JDK 21, Maven, Spring Boot, and Oracle work together. It has a `GET` list, a `GET` by id, and a `POST`, with Swagger UI on port 8000.

Choose one way to provide the database:

| Option | Use it when |
|---|---|
| **A. Your local Oracle 19c** | You have Oracle 19c installed (the programme setup) |
| **B. Docker container** | You have no local Oracle and want a throwaway database |

## Folder

| Path | Purpose |
|---|---|
| `db/init.sql` | Creates the `MESSAGES` table and two sample rows |
| `src/main/resources/application.yml` | Default connection (the Docker container) |
| `src/main/resources/application-19c.yml` | Connection for your local Oracle 19c |
| `docker-compose.yml` | Option B only: Oracle XE 21c container (`gvenzl/oracle-xe:21-slim`) |

## Option A: local Oracle 19c

1. **Create the table.** Connect as your schema user and run [db/init.sql](db/init.sql) after deleting the two `ALTER SESSION` lines, as the comments in the file explain.

2. **Check it:**

   ```sql
   SELECT * FROM MESSAGES;
   ```

3. **Fill in [application-19c.yml](src/main/resources/application-19c.yml):** the service name, and your schema user. Ask your DBA for the service name if you do not know it.

4. **Run the app** with the `19c` profile and your password in an environment variable:

   ```bash
   export DB_PASSWORD='<your-schema-password>'
   mvn spring-boot:run -Dspring-boot.run.profiles=19c
   ```

   On Windows PowerShell, use `$env:DB_PASSWORD = '<your-schema-password>'` instead of `export`.

5. **Open Swagger UI:** <http://localhost:8000/swagger-ui.html>

### What differs from the Docker setup

| | Docker (option B) | Local Oracle 19c (option A) |
|---|---|---|
| Service name | `XEPDB1` | `ORCL` or `ORCLPDB1`; ask your DBA |
| User | `testapp` (created by the image) | Your own schema user |
| Table creation | Automatic, on first start | You run `db/init.sql` once |
| Password | `testapp` | Your schema password, from `DB_PASSWORD` |
| Driver and Hibernate | Same | Same; `ojdbc11` 23.x and Hibernate 6.6 both support Oracle 19c |

> **Note:** Everything stays on your machine and your organization's network. Nothing here is exposed to the internet.

## Option B: Docker container

Needs Docker with Compose. The slim image is about 0.8 GB to download and 2.6 GB on disk. It is `amd64` only, so on Apple Silicon it runs under emulation.

```bash
docker compose up -d
docker compose logs -f oracle        # wait for: DATABASE IS READY TO USE!
mvn spring-boot:run
```

To look at the data:

```bash
docker exec -it testapp-oracle sqlplus testapp/testapp@//localhost:1521/XEPDB1
```

```bash
docker compose stop          # stop, keep the data
docker compose down -v       # remove the container and its data
```

`db/init.sql` runs only when the database is first created. To run it again, use `down -v` and `up -d`.

## API

| Method | Path | Result |
|---|---|---|
| `GET` | `/api/messages` | `200`, list of messages |
| `GET` | `/api/messages/{id}` | `200`, or `404` |
| `POST` | `/api/messages` | `201`, or `400` if `text` is blank |

```bash
curl -s http://localhost:8000/api/messages

curl -s -X POST http://localhost:8000/api/messages \
  -H 'Content-Type: application/json' \
  -d '{"text":"my first message"}'
```

## Common pitfalls

| Symptom | Cause | Fix |
|---|---|---|
| `ORA-12514` or `ORA-12505` | Wrong service name | Check it with your DBA or `lsnrctl status`; update `application-19c.yml` |
| `ORA-01017: invalid username/password` | Wrong user, or `DB_PASSWORD` not set in this shell | Re-check both |
| `Could not resolve placeholder 'DB_PASSWORD'` | The `19c` profile is active but `DB_PASSWORD` is not exported | Export it, then start the app in the same shell |
| `missing table [MESSAGES]` | `db/init.sql` not run in the schema the app connects to | Run it as that user |
| `ORA-12541` or `Connection refused` | Listener not running, or wrong host or port | Check the database is up and the host and port are right |
| `ORA-01950: no privileges on tablespace` | Your schema user has no quota | Ask your DBA to grant quota on its tablespace |
| Port 8000 already in use | Another process holds the port | Stop it, or change `server.port` |
