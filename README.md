# Event Ledger API

A Spring Boot REST API that acts as an idempotent financial transaction event store. It correctly handles duplicate event submissions and out-of-order event arrival from multiple upstream systems.

## Prerequisites

- **Java 21** — [Download](https://adoptium.net/)
- **Maven 3.9+** — [Download](https://maven.apache.org/download.cgi)
- No external database required — uses H2 in-memory database

Verify your setup:
```bash
java -version   # should show 21.x
mvn -version    # should show 3.9+
```

## Build

```bash
mvn clean package
```

## Run

```bash
# Option 1 — Maven plugin
mvn spring-boot:run

# Option 2 — JAR
java -jar target/event-ledger-api-1.0.0-SNAPSHOT.jar
```

The API starts on **http://localhost:8081**.

> **Note:** Data resets on every restart (in-memory database by design).

## Run Tests

```bash
mvn test
```

---

## API Endpoints

### POST /events — Submit a transaction event

Returns **201 Created** for new events. Returns **200 OK** if the same `eventId` is submitted again (idempotent — original event is returned unchanged).

```bash
curl -X POST http://localhost:8081/events \
  -H "Content-Type: application/json" \
  -d '{
    "eventId": "evt-001",
    "accountId": "acct-123",
    "type": "CREDIT",
    "amount": 150.00,
    "currency": "USD",
    "eventTimestamp": "2026-05-15T14:02:11Z",
    "metadata": {
      "source": "mainframe-batch",
      "batchId": "B-9042"
    }
  }'
```

**Response (201 Created):**
```json
{
  "eventId": "evt-001",
  "accountId": "acct-123",
  "type": "CREDIT",
  "amount": 150.00,
  "currency": "USD",
  "eventTimestamp": "2026-05-15T14:02:11Z",
  "metadata": { "source": "mainframe-batch", "batchId": "B-9042" },
  "createdAt": "2026-05-22T09:00:00Z"
}
```

---

### GET /events/{id} — Retrieve a single event by ID

```bash
curl http://localhost:8081/events/evt-001
```

Returns **200 OK** with the event, or **404 Not Found** if the event does not exist.

---

### GET /events?account={accountId} — List events for an account

Events are always returned in **chronological order by `eventTimestamp`**, regardless of the order they were submitted.

```bash
curl "http://localhost:8081/events?account=acct-123"
```

Returns **200 OK** with an array of events, or **404 Not Found** if no events exist for the account.

---

### GET /accounts/{accountId}/balance — Get current balance

Balance = sum of all CREDIT amounts − sum of all DEBIT amounts.

```bash
curl http://localhost:8081/accounts/acct-123/balance
```

**Response (200 OK):**
```json
{
  "accountId": "acct-123",
  "balance": 120.00,
  "currency": "USD"
}
```

Returns **404 Not Found** if no events exist for the account.

---

## Error Responses

All errors return a consistent JSON shape:

```json
{
  "timestamp": "2026-05-22T09:00:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "amount must be greater than 0",
  "path": "/events"
}
```

| Status | Cause |
|--------|-------|
| 400    | Missing required fields, invalid amount (≤ 0), unknown event type, malformed JSON |
| 404    | Event or account not found |
| 500    | Unexpected server error |

---

## H2 Console (Development)

The H2 web console is available while the application is running:

- URL: http://localhost:8081/h2-console
- JDBC URL: `jdbc:h2:mem:eventledgerdb`
- Username: `sa`
- Password: *(leave blank)*

---

## Design Decisions

| Decision | Rationale |
|----------|-----------|
| **BigDecimal for amounts** | Avoids floating-point precision loss inherent in `double`/`float` |
| **Idempotency on `eventId`** | Unique DB constraint + application-level check; returns original event on duplicate |
| **`eventTimestamp` drives ordering** | Stored and queried by event time, not insertion time — guarantees correct order regardless of arrival |
| **Balance via JPQL aggregate** | Computed at database level; avoids loading all rows into memory |
| **Metadata as JSON TEXT** | H2 does not support JSONB; stored as serialized JSON string, deserialized on read |
| **`DB_CLOSE_DELAY=-1`** | Keeps the H2 in-memory database alive between connection pool cycles during tests |
| **Single currency per balance** | Balance endpoint returns currency from the account's first event. Multi-currency grouping is out of scope. |
