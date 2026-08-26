# Hotel Management System

A full-stack hotel management application — originally a console-based Java system, extended with a MySQL persistence layer, a REST-style HTTP backend, and an HTML/CSS/JS frontend. Core booking logic uses an interval-scheduling algorithm to check room availability efficiently.

---

## Architecture

```
┌─────────────────────┐
│   Frontend (Web)     │
│  book.html            │  HTML + CSS + vanilla JS (fetch API)
│  bookings.html         │
└──────────┬───────────┘
           │ HTTP (GET / POST / DELETE, JSON)
           ▼
┌─────────────────────┐
│   Server.java         │  Java's built-in com.sun.net.httpserver.HttpServer
│   (port 8080)          │  Routes: /bookings, /rooms/available
└──────────┬───────────┘
           │
           ▼
┌─────────────────────┐
│  RoomAvailabilityService │  Interval-scheduling logic (see below)
└──────────┬───────────┘
           │
           ▼
┌─────────────────────┐
│  BookingDAO / StaffDAO /  │  JDBC data-access layer
│  ComplaintDAO              │
└──────────┬───────────┘
           │
           ▼
┌─────────────────────┐
│      MySQL             │  guests, rooms, bills, staff, complaints
└─────────────────────┘
```

The original console application (`HotelManagement`, `GuestHandling`, `Advance_Booking`, `StaffHandling`, `Complaints`) still runs independently and shares the same MySQL database and DAO layer as the web frontend — both interfaces stay in sync through the same data source.

**Scope note:** The web UI covers the guest-facing booking flow (book + view + cancel), since that's where the interval-scheduling logic lives and it mirrors what an actual hotel guest would use online. Staff scheduling and complaint handling remain console-only, as they're internal/admin tools — those modules are still persisted to MySQL, just not exposed over HTTP.

---

## The core algorithm: interval scheduling for room availability

**Problem:** Given a room and a requested date range, determine whether it's free — without scanning every booking that room has ever had.

**Approach:** Bookings for each room are loaded into a `TreeMap<LocalDate, LocalDate>` (check-in date → check-out date), which keeps them sorted by check-in date automatically. To check if a new range `[newIn, newOut)` is free, only two neighboring entries need to be checked, both in O(log n):

- `floorEntry(newIn)` — the booking that starts at or before the requested check-in. If its checkout is after the requested check-in, the ranges overlap.
- `ceilingEntry(newIn)` — the booking that starts at or after the requested check-in. If it starts before the requested check-out, the ranges overlap.

If neither neighbor overlaps, the room is free — there's no need to inspect any other booking, because a sorted structure guarantees no other interval could possibly overlap without also being adjacent to one of these two.

```java
private boolean isRoomFree(int roomNo, LocalDate newIn, LocalDate newOut) {
    TreeMap<LocalDate, LocalDate> intervals = ...; // room's bookings, check-in -> check-out

    var floorEntry = intervals.floorEntry(newIn);
    if (floorEntry != null && floorEntry.getValue().isAfter(newIn)) return false;

    var ceilingEntry = intervals.ceilingEntry(newIn);
    if (ceilingEntry != null && ceilingEntry.getKey().isBefore(newOut)) return false;

    return true;
}
```

This replaced the original implementation, which used fixed-size `boolean[]` arrays per room category and assigned the first `false` slot with no awareness of dates — meaning it could not detect overlapping bookings for future stays.

---

## Database schema (ERD)

Five tables, three foreign-key relationships:

- **rooms** (`room_no` PK) — room type, price, status
- **guests** (`guest_id` PK) — booking records; FK → `rooms.room_no`
- **bills** (`bill_id` PK) — FK → `guests.guest_id`
- **complaints** (`complaint_id` PK) — FK → `guests.guest_id` (nullable)
- **staff** (`staff_id` PK) — standalone, no booking-table relationship

Sample queries the schema supports:
- Overlap detection for a given room and date range (the interval-scheduling query, above)
- Revenue by month (`GROUP BY`, `SUM`)
- Most-booked room type (`JOIN` + `GROUP BY` + `ORDER BY`)
- Rooms currently available (`LEFT JOIN` excluding active bookings in a date range)

---

## Tech stack

| Layer | Technology |
|---|---|
| Language | Java (core OOP, no frameworks) |
| Database | MySQL |
| DB access | JDBC (`java.sql`) |
| HTTP server | `com.sun.net.httpserver.HttpServer` (built into the JDK) |
| Frontend | HTML, CSS, vanilla JavaScript (`fetch` API) |

Deliberately framework-free on the backend (no Spring Boot) — the goal was to demonstrate the underlying HTTP/JDBC mechanics directly rather than through an abstraction layer.

---

## API endpoints

| Method | Endpoint | Description |
|---|---|---|
| GET | `/bookings` | Returns all bookings as JSON |
| POST | `/bookings` | Creates a booking. Body: `name, phno, type, checkin, days` (form-urlencoded) |
| DELETE | `/bookings?name=&phno=&room=` | Cancels a booking |
| GET | `/rooms/available?type=&checkin=&days=` | Runs the availability check, returns `{"room": N}` or `{"room": -1}` |

---

## Running the project

1. **Set up MySQL** — create the database and run the schema (see `/sql/schema.sql` if included, or the `CREATE TABLE` statements above).
2. **Configure the connection** — update `DBConnection.java` with your MySQL username/password.
3. **Run the console app** — compile and run `HotelManagement.java` for the terminal interface.
4. **Run the web backend** — compile and run `Server.java`. It starts on `http://localhost:8080`.
5. **Open the frontend** — open `book.html` directly in a browser (no separate web server needed; it calls the Java backend directly via `fetch`). Use `bookings.html` to view and cancel bookings.

---

## Known limitations / possible next steps

- Staff and complaint modules aren't exposed over HTTP (console-only, by design — see Scope note above)
- No authentication/authorization layer
- No "my bookings" filtered view for individual guests (currently shows all bookings)
- No input validation beyond basic type parsing on both client and server
