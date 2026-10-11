# Test app: Spring Boot 3.5 + Oracle XE 21c

A small app to check that JDK 21, Maven, Spring Boot, and Oracle work together. It has a `GET` list, a `GET` by id, and a `POST`, with Swagger UI on port 8000.

> **Note:** The Oracle container is a local convenience for this test app only. The programme's deployment stays on your organization's own network, with Oracle 19c.

## Folder

| Path | Purpose |
|---|---|
| `docker-compose.yml` | Oracle XE 21c container (`gvenzl/oracle-xe:21-slim`) |
| `db/init.sql` | Creates the `MESSAGES` table and two sample rows in the `testapp` schema |
| `src/` | The Spring Boot app |

## Before you start

- JDK 21, Maven 3.9+, Docker with Compose

> **Note:** The slim image is about 0.8 GB to download and 2.6 GB on disk. It is `amd64` only, so on Apple Silicon it runs under emulation.

## Run it

```bash
# 1. Start Oracle (the first run also downloads the image)
docker compose up -d
docker compose logs -f oracle        # wait for: DATABASE IS READY TO USE!

# 2. Start the app
mvn spring-boot:run
```

Open Swagger UI: <http://localhost:8000/swagger-ui.html>

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

## Look at the database

```bash
docker exec -it testapp-oracle sqlplus testapp/testapp@//localhost:1521/XEPDB1
```

Then, at the `SQL>` prompt:

```sql
SELECT * FROM MESSAGES;
```

To use SQL Developer instead: host `localhost`, port `1521`, service name `XEPDB1`, user `testapp`, password `testapp`.

## Stop and reset

```bash
docker compose stop          # stop, keep the data
docker compose down -v       # remove the container and its data
```

`db/init.sql` runs only when the database is first created. To run it again, use `down -v` and `up -d`.

## Common pitfalls

| Symptom | Fix |
|---|---|
| App fails with `ORA-12541`, `ORA-01017` or `Connection refused` | The database is still starting; wait for `DATABASE IS READY TO USE!` in `docker compose logs oracle` |
| `missing table [MESSAGES]` | `init.sql` did not complete; check `docker compose logs oracle`, then `down -v` and `up -d` |
| Port 8000 or 1521 already in use | Stop the other process, or change the port mapping in `docker-compose.yml` or `server.port` |
