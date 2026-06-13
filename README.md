# Flight Ticket Booking API

## Description & Requirements
- Design and implement a small REST API for a flight ticket booking system. Share the github project once done.
## Technical expectations
- Use Spring Boot and Java
- Single application instance (no distributed systems concerns)
- No authentication, authorization, rate limiting
- No flight search or destination logic
- All booking operations assume the client already knows the flight number, no search required
- In-memory storage only (no database required)
- Do not allow overbooking the flights
- Do not need APIs to retrieve bookings. Only to book. 
- Model REST endpoints as you see fit
- Use appropriate HTTP methods and status codes

## What I implemented

- **Multi-seat booking** — One passenger can book up to four seats in a single request. Duplicate seats in the same request are rejected.

- **No overbooking** — A seat can only be booked once. A second request for an already-booked seat is rejected with a conflict response.

- **No partial booking** — If any seat in a multi-seat request is unavailable, the entire booking fails. No seats from that request are reserved.

- **Safe concurrent booking** — When multiple users book at the same time (including overlapping seats), the system keeps seat state consistent and does not double-book.

- **Retry-safe booking** — Clients can retry a failed or timed-out request using the same idempotency key without creating a duplicate booking.

- **Input validation** — Missing flight, empty seat list, blank name, or too many seats are rejected before any booking is attempted.

- **Meaningful error responses** — Unknown flights, invalid seats, conflicts, and bad input return clear error messages with the right HTTP status.

- **Automated test coverage** — Tests verify successful booking, rejection cases, concurrent retries, and overlapping seat requests.

## What could be improved

- **Extract seat locking** — Lock acquire, validate, and release logic lives inside `BookingService`. Moving it to a dedicated helper or class would simplify the service and make the concurrency model easier to test in isolation.

- **Payload-based idempotency** — Currently only the header key is checked. Could also hash the request body so the same key with a different payload is rejected instead of silently replaying an unrelated booking. Currently if a request as same idempotency key the previous response is sent regardless of the content of payload

- **Idempotency cache eviction** — Stored responses never expire, so memory grows with every unique key. A TTL or LRU eviction policy would bound storage while still supporting reasonable retry windows.

- **Hold Seat Status** - Status HOLD for seats to incorporate additional operations like payment which can be coupled with idempotency

- **Richer debug logging** — Current logs cover main paths only. Could add correlation ids, thread names, and timing around lock acquire/release to make concurrent issues easier to trace in production.


## Prerequisites

- Java 17+
- Maven 3.8+

```bash
java -version
mvn -version
```

## Run the project

```bash
git clone https://github.com/git-ashutosh-das/AshutoshKumar-FlightBooking.git
cd AshutoshKumar-FlightBooking
mvn spring-boot:run
```

Server starts at **http://localhost:8080**. Data is seeded on startup and resets when the app restarts.

## Sample flights

| Flight   | Seats        |
|----------|--------------|
| `flight1` | `1`–`6` (6 seats) |
| `flight2` | `1`–`8` (8 seats) |
| `flight3` | `1`–`10` (10 seats) |

## API

**`POST /api/bookings`**

| Header            | Required | Description                                      |
|-------------------|----------|--------------------------------------------------|
| `Idempotency-Key` | Yes      | Client-generated key; retries return same booking |
| `Content-Type`    | Yes      | `application/json`                               |

**Request body**

| Field    | Description                          |
|----------|--------------------------------------|
| `flight` | Flight number (e.g. `flight1`)       |
| `seats`  | Array of 1–4 unique seat labels      |
| `name`   | Passenger name                       |

**Responses**

| Status | Meaning                                      |
|--------|----------------------------------------------|
| `201`  | New booking created                          |
| `200`  | Idempotent replay (same key, prior success)  |
| `400`  | Validation error                             |
| `404`  | Flight or seat not found                     |
| `409`  | Seat already booked or lock contention       |

---

## curl — positive scenarios

### 1. Single-seat booking (`201 Created`)

```bash
curl -i -X POST http://localhost:8080/api/bookings \
  -H "Idempotency-Key: demo-single-1" \
  -H "Content-Type: application/json" \
  -d '{
    "flight": "flight1",
    "seats": ["1"],
    "name": "Ashutosh Kumar"
  }'
```

### 2. Multi-seat booking (`201 Created`)

```bash
curl -i -X POST http://localhost:8080/api/bookings \
  -H "Idempotency-Key: demo-multi-1" \
  -H "Content-Type: application/json" \
  -d '{
    "flight": "flight2",
    "seats": ["1", "2", "3"],
    "name": "Ashutosh Kumar"
  }'
```

### 3. Idempotent retry (`200 OK`)

Run the same request twice with the **same** `Idempotency-Key`. The second call returns `200` with the original booking — no duplicate reservation.

```bash
curl -i -X POST http://localhost:8080/api/bookings \
  -H "Idempotency-Key: demo-retry-1" \
  -H "Content-Type: application/json" \
  -d '{
    "flight": "flight1",
    "seats": ["2"],
    "name": "Ashutosh Kumar"
  }'
```

Run the exact command again — expect `HTTP/1.1 200` and the same booking `id`.

---

## curl — negative scenarios

Use a **fresh seat** for conflict tests if you already booked that seat above (or restart the app).

### 4. Unknown flight (`404 Not Found`)

```bash
curl -i -X POST http://localhost:8080/api/bookings \
  -H "Idempotency-Key: demo-404-flight" \
  -H "Content-Type: application/json" \
  -d '{
    "flight": "flight99",
    "seats": ["1"],
    "name": "Ashutosh Kumar"
  }'
```

Expected: `{"error":"Flight not found: flight99"}`

### 5. Seat not on flight (`404 Not Found`)

```bash
curl -i -X POST http://localhost:8080/api/bookings \
  -H "Idempotency-Key: demo-404-seat" \
  -H "Content-Type: application/json" \
  -d '{
    "flight": "flight1",
    "seats": ["99"],
    "name": "Ashutosh Kumar"
  }'
```

Expected: `{"error":"Seat 99 not found on flight flight1"}`

### 6. Seat already booked (`409 Conflict`)

Book first:

```bash
curl -i -X POST http://localhost:8080/api/bookings \
  -H "Idempotency-Key: demo-conflict-first" \
  -H "Content-Type: application/json" \
  -d '{
    "flight": "flight1",
    "seats": ["3"],
    "name": "First Passenger"
  }'
```

Then book the same seat with a **different** key:

```bash
curl -i -X POST http://localhost:8080/api/bookings \
  -H "Idempotency-Key: demo-conflict-second" \
  -H "Content-Type: application/json" \
  -d '{
    "flight": "flight1",
    "seats": ["3"],
    "name": "Second Passenger"
  }'
```

Expected: `{"error":"Seat 3 on flight flight1 is already booked"}`

### 7. Missing passenger name (`400 Bad Request`)

```bash
curl -i -X POST http://localhost:8080/api/bookings \
  -H "Idempotency-Key: demo-400-name" \
  -H "Content-Type: application/json" \
  -d '{
    "flight": "flight1",
    "seats": ["4"]
  }'
```

Expected: `{"error":"Passenger name is required"}`

### 8. Empty seats list (`400 Bad Request`)

```bash
curl -i -X POST http://localhost:8080/api/bookings \
  -H "Idempotency-Key: demo-400-empty-seats" \
  -H "Content-Type: application/json" \
  -d '{
    "flight": "flight1",
    "seats": [],
    "name": "Ashutosh Kumar"
  }'
```

Expected: `{"error":"At least one seat is required"}`

### 9. More than 4 seats (`400 Bad Request`)

```bash
curl -i -X POST http://localhost:8080/api/bookings \
  -H "Idempotency-Key: demo-400-max-seats" \
  -H "Content-Type: application/json" \
  -d '{
    "flight": "flight1",
    "seats": ["1", "2", "3", "4", "5"],
    "name": "Ashutosh Kumar"
  }'
```

Expected: `{"error":"A booking can include at most 4 seats"}`

### 10. Duplicate seats in one request (`400 Bad Request`)

```bash
curl -i -X POST http://localhost:8080/api/bookings \
  -H "Idempotency-Key: demo-400-duplicate" \
  -H "Content-Type: application/json" \
  -d '{
    "flight": "flight1",
    "seats": ["1", "1"],
    "name": "Ashutosh Kumar"
  }'
```

Expected: `{"error":"Duplicate seat numbers are not allowed in a single booking"}`

### 11. Missing flight number (`400 Bad Request`)

```bash
curl -i -X POST http://localhost:8080/api/bookings \
  -H "Idempotency-Key: demo-400-no-flight" \
  -H "Content-Type: application/json" \
  -d '{
    "seats": ["1"],
    "name": "Ashutosh Kumar"
  }'
```

Expected: `{"error":"Flight number is required"}`

### 12. Missing Idempotency-Key (`400 Bad Request`)

```bash
curl -i -X POST http://localhost:8080/api/bookings \
  -H "Content-Type: application/json" \
  -d '{
    "flight": "flight1",
    "seats": ["5"],
    "name": "Ashutosh Kumar"
  }'
```

Expected: `400` — Spring rejects the request when the required header is absent.

---

## Run tests

```bash
mvn test
```

## Project structure

```
src/main/java/com/flightbooking/
├── config/          # Startup flight data
├── controller/      # REST endpoints
├── dto/             # Request/response objects
├── exception/       # Error handling
├── model/           # Flight, Seat, Booking
├── repository/      # In-memory storage
└── service/         # Booking + idempotency logic
```
