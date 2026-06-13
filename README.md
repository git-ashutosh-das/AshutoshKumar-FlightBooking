# AshutoshKumar-FlightBooking

REST API for a flight ticket booking system built with Spring Boot and Maven. Flights and seats are stored in memory — no database setup required.

## Prerequisites

- **Java 17** or later
- **Maven 3.8+**

Verify your setup:

```bash
java -version
mvn -version
```

## Clone and run

```bash
git clone https://github.com/git-ashutosh-das/AshutoshKumar-FlightBooking.git
cd AshutoshKumar-FlightBooking
mvn spring-boot:run
```

The application starts on **http://localhost:8080**.

## Sample flights

Sample data is loaded automatically at startup:

| Flight | Seats |
|--------|-------|
| AI101  | 1A, 1B, 1C, 2A, 2B, 2C |
| AI202  | 1A, 1B, 2A, 2B |
| 6E303  | 1A, 1B, 1C, 1D |

## API

### Book a seat

**`POST /api/bookings`**

```bash
curl -X POST http://localhost:8080/api/bookings \
  -H "Content-Type: application/json" \
  -d '{
    "flight": "AI101",
    "seat": "1A",
    "name": "Ashutosh Kumar"
  }'
```

**Request body**

| Field   | Description              |
|---------|--------------------------|
| `flight`| Flight number (e.g. AI101) |
| `seat`  | Seat number (e.g. 1A)    |
| `name`  | Passenger name           |

**Responses**

| Status | Description |
|--------|-------------|
| `201 Created` | Booking successful |
| `400 Bad Request` | Missing or invalid fields |
| `404 Not Found` | Flight or seat does not exist |
| `409 Conflict` | Seat is already booked |

**Success response example**

```json
{
  "id": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
  "flight": "AI101",
  "seat": "1A",
  "name": "Ashutosh Kumar"
}
```

## Run tests

```bash
mvn test
```

## Project structure

```
src/main/java/com/flightbooking/
├── config/          # Startup data initialization
├── controller/      # REST endpoints
├── dto/             # Request and response objects
├── exception/       # Error handling
├── model/           # Flight, Seat, Booking entities
├── repository/      # In-memory storage
└── service/         # Booking business logic
```

## Notes

- Single application instance with in-memory storage (data resets on restart).
- No authentication or flight search — the client must know the flight number and seat.
- Overbooking is prevented; each seat can only be booked once.
