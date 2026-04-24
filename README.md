# Smart-campus-api-cheshanth

**Module:** 5COSC022W Client-Server Architectures  
**Student Name:** Jagannithy Cheshanth  
**Student ID:** w2120641  
**University:** University of Westminster  
**Stack:** Java 11 · JAX-RS (Jersey 2.41) · Grizzly embedded HTTP · Jackson JSON · Maven  
**Last Updated:** 2026-04-24

---

## API Design Overview

The Smart Campus API follows **RESTful architectural principles** to manage a hierarchy of campus **Rooms** and the **Sensors** deployed within them. Each sensor maintains a historical log of **SensorReadings**.

### Resource Hierarchy

```
/api/v1                              ← Discovery (HATEOAS entry point)
/api/v1/rooms                        ← Room collection
/api/v1/rooms/{roomId}               ← Individual room
/api/v1/sensors                      ← Sensor collection
/api/v1/sensors?type={type}          ← Filtered sensor list
/api/v1/sensors/{sensorId}           ← Individual sensor
/api/v1/sensors/{sensorId}/readings  ← Sensor reading history (sub-resource)
```

### Data Models

| Model | Key Fields |
|---|---|
| `Room` | `id`, `name`, `capacity`, `sensorIds[]` |
| `Sensor` | `id`, `type`, `status` (ACTIVE/MAINTENANCE/OFFLINE), `currentValue`, `roomId` |
| `SensorReading` | `id` (UUID), `timestamp` (epoch ms), `value` |

### Design Decisions

- **In-memory storage only** — `ConcurrentHashMap` for rooms/sensors, `CopyOnWriteArrayList` for reading histories.
- **No database, no Spring Boot** — pure JAX-RS (Jersey) as required.
- **Singleton DataStore** — shared across all request-scoped resource instances via `DataStore.getInstance()`.
- **All errors return structured JSON** — `{ "error": "...", "message": "...", "status": ... }`. No stack traces are ever returned to clients.

---

## How to Build and Run

### Prerequisites

- Java 11 or higher (`java -version`)
- Apache Maven 3.6+ (`mvn -version`)
- Internet access for first build (Maven downloads dependencies)

### 1. Clone the repository

```bash
git clone https://github.com/cheshanth-14/smart-campus-api..git
cd smart-campus-api.
```

### 2. Build the project

```bash
mvn clean package
```

This compiles the code and produces a fat/uber JAR at `target/smart-campus-api-cheshanth-1.0.0.jar`.

### 3. Run the server

**Option A — Maven exec plugin (development):**

```bash
mvn exec:java
```

**Option B — Fat JAR (production / demo):**

```bash
java -jar target/smart-campus-api-cheshanth-1.0.0.jar
```

The server starts on **http://localhost:8080**. Press `ENTER` in the terminal to stop it.

### 4. Verify it works

```bash
curl -s http://localhost:8080/api/v1/ | python3 -m json.tool
```

You should see the discovery JSON with version and resource links.

---

## Sample curl Commands

### 1. Discovery endpoint — GET /api/v1/

```bash
curl -s -X GET http://localhost:8080/api/v1/ \
  -H "Accept: application/json" | python3 -m json.tool
```

Expected: `200 OK` with API metadata and resource map.

---

### 2. Create a Room — POST /api/v1/rooms

```bash
curl -s -X POST http://localhost:8080/api/v1/rooms \
  -H "Content-Type: application/json" \
  -d '{"id":"LIB-301","name":"Library Quiet Study","capacity":40}' \
  | python3 -m json.tool
```

Expected: `201 Created` with a `Location` header and the created Room JSON.

---

### 3. Register a Sensor in that Room — POST /api/v1/sensors

```bash
curl -s -X POST http://localhost:8080/api/v1/sensors \
  -H "Content-Type: application/json" \
  -d '{"id":"CO2-001","type":"CO2","status":"ACTIVE","currentValue":0.0,"roomId":"LIB-301"}' \
  | python3 -m json.tool
```

Expected: `201 Created` with sensor JSON.

---

### 4. Filter sensors by type — GET /api/v1/sensors?type=CO2

```bash
curl -s -X GET "http://localhost:8080/api/v1/sensors?type=CO2" \
  -H "Accept: application/json" | python3 -m json.tool
```

Expected: `200 OK` — array containing only CO2 sensors.

---

### 5. Post a sensor reading — POST /api/v1/sensors/{sensorId}/readings

```bash
curl -s -X POST http://localhost:8080/api/v1/sensors/CO2-001/readings \
  -H "Content-Type: application/json" \
  -d '{"value":850.5}' | python3 -m json.tool
```

Expected: `201 Created`. The sensor's `currentValue` is now updated to `850.5`.

---

### 6. Attempt to delete a Room that still has sensors — expect 409

```bash
curl -s -X DELETE http://localhost:8080/api/v1/rooms/LIB-301 \
  -H "Accept: application/json" | python3 -m json.tool
```

Expected: `409 Conflict` — structured JSON error body.

---

### 7. Attempt to POST a sensor with a non-existent roomId — expect 422

```bash
curl -s -X POST http://localhost:8080/api/v1/sensors \
  -H "Content-Type: application/json" \
  -d '{"type":"Temperature","roomId":"DOES-NOT-EXIST"}' \
  | python3 -m json.tool
```

Expected: `422 Unprocessable Entity`.

---

### 8. Post a reading to a MAINTENANCE sensor — expect 403

```bash
# First set sensor to MAINTENANCE (via POST with updated status, or create one in MAINTENANCE)
curl -s -X POST http://localhost:8080/api/v1/sensors \
  -H "Content-Type: application/json" \
  -d '{"id":"TEMP-MAINT","type":"Temperature","status":"MAINTENANCE","roomId":"LIB-301"}' \
  | python3 -m json.tool

# Now attempt a reading — should return 403
curl -s -X POST http://localhost:8080/api/v1/sensors/TEMP-MAINT/readings \
  -H "Content-Type: application/json" \
  -d '{"value":22.1}' | python3 -m json.tool
```

Expected: `403 Forbidden`.

---

## Report — Answers to Questions

### Part 1.1 — JAX-RS Resource Lifecycle & In-Memory Data Management

By default, JAX-RS creates a **new instance** of each resource class for every incoming HTTP request (request-scoped lifecycle). This means that any instance variables declared on a resource class are not shared between requests — each request gets its own fresh object. The runtime does not treat resource classes as singletons unless explicitly annotated with `@Singleton`.

This architectural decision has a direct impact on in-memory data management. If each request-scoped resource instance held its own `HashMap`, data written by one request would be invisible to all other requests, making persistence impossible. To solve this, the Smart Campus API uses the **Singleton DataStore pattern**: a static, eagerly-initialised `DataStore.getInstance()` object holds all `ConcurrentHashMap` and `CopyOnWriteArrayList` collections. Every request-scoped resource instance calls `DataStore.getInstance()` and thereby reads and writes the same shared in-memory structures. `ConcurrentHashMap` is chosen over a plain `HashMap` because it permits concurrent reads and fine-grained lock-striped writes, preventing data corruption when multiple requests arrive simultaneously without the bottleneck of a fully synchronised block.

---

### Part 1.2 — HATEOAS and Hypermedia-Driven APIs

HATEOAS (Hypermedia As The Engine Of Application State) is considered a hallmark of mature RESTful design because it makes an API **self-describing and navigable at runtime**. Instead of relying on external, potentially stale, static documentation, each response includes links to related resources and available actions. For example, a discovery endpoint at `GET /api/v1` returns a map of resource URLs (`rooms`, `sensors`), so a client can explore the entire API starting from a single known entry point without hardcoding paths.

Compared to static documentation, HATEOAS provides several benefits for client developers: the API is always accurate because the links come from the live server; clients do not break when base URLs or paths change (they follow links, not hardcoded strings); and it reduces the cognitive burden of reading and maintaining separate API docs. It also enables progressive API evolution — new resource links can be added without breaking existing clients. This aligns with Roy Fielding's original REST constraints, where the uniform interface and discoverability are central principles.

---

### Part 2.1 — Returning IDs vs. Full Objects in a List

Returning **only IDs** in a list response minimises payload size and reduces bandwidth, which is beneficial when a collection is large and the client only needs identifiers (e.g., to build a dropdown list). However, this forces the client to make N additional GET requests to retrieve the full details of each item — the classic **N+1 request problem** — which increases total latency and server load.

Returning **full objects** allows the client to render a complete list in a single round-trip, significantly improving perceived performance. The trade-off is a larger payload, which may be unnecessary if the client only needs a few fields. A pragmatic best practice is to return a **summary representation** — include the most commonly needed fields (id, name, capacity) plus a `self` link — rather than the full object with nested collections. This balances bandwidth efficiency with client usability and follows the HATEOAS principle by embedding navigation links within each list item.

---

### Part 2.2 — DELETE Idempotency

Yes, the DELETE operation is **idempotent** in this implementation. Idempotency means that making the same request N times produces the same server state as making it once. The first `DELETE /api/v1/rooms/{roomId}` call removes the room from the data store and returns `204 No Content`. Any subsequent calls to the same URL return `404 Not Found` because the room no longer exists. Critically, the **server state is identical** after the first and all subsequent calls — the room is gone regardless of how many times the request is sent. The HTTP status code may differ (204 vs 404), but idempotency is defined in terms of server-side state, not response codes. This behaviour is consistent with RFC 9110, which defines DELETE as idempotent.

---

### Part 3.1 — @Consumes and Media Type Mismatch

The `@Consumes(MediaType.APPLICATION_JSON)` annotation instructs the JAX-RS runtime to only invoke the annotated method when the incoming request's `Content-Type` header is `application/json`. If a client sends data with a different media type — such as `text/plain` or `application/xml` — the runtime performs content negotiation **before** the resource method is invoked. It finds no matching method for the declared content type and returns **HTTP 415 Unsupported Media Type** automatically. The resource method code is never reached, and no custom handling is required. This is a clean separation of concerns: the framework enforces the contract, and the application code can assume the body is valid JSON when it receives it. It also means clients receive a meaningful, standards-compliant error rather than a cryptic deserialization exception.

---

### Part 3.2 — @QueryParam vs. Path Segment for Filtering

Path parameters (e.g., `/api/v1/sensors/type/CO2`) are designed to **identify a resource** uniquely within a hierarchy. Using a path segment for filtering implies that "CO2" is itself a sub-resource of "type", which is semantically incorrect and creates a misleading resource model. It also makes combining filters awkward: `/sensors/type/CO2/status/ACTIVE` becomes a deeply nested, fragile URL.

`@QueryParam` (e.g., `/api/v1/sensors?type=CO2`) is the correct approach because query parameters are intended to **modify or narrow a collection** without changing the resource identity. They are optional by nature, naturally support multiple simultaneous filters (`?type=CO2&status=ACTIVE`), are easily ignored when absent, and keep the canonical resource URL (`/api/v1/sensors`) stable. This is also more cacheable because the base URL remains constant. REST API design guidelines, including those from Google and Microsoft, consistently recommend query parameters for filtering, sorting, and searching collection resources.

---

### Part 4.1 — Sub-Resource Locator Pattern Benefits

The sub-resource locator pattern delegates path resolution to a separate, dedicated resource class rather than defining every nested endpoint in a single monolithic controller. In the Smart Campus API, `SensorResource` contains a locator method annotated only with `@Path("/{sensorId}/readings")` that returns a `SensorReadingResource` instance. JAX-RS then dispatches all HTTP method matching for that path to `SensorReadingResource`.

The architectural benefits are significant. First, it enforces **separation of concerns** — reading-history logic is encapsulated in `SensorReadingResource` and does not pollute `SensorResource`. Second, it improves **testability** because `SensorReadingResource` can be unit-tested in isolation with a mock `sensorId`. Third, it scales well: as the API grows (e.g., `/sensors/{id}/alerts`, `/sensors/{id}/config`), each concern gets its own class without making any existing class longer. Fourth, it enables **lazy instantiation** — the sub-resource object is only created when the `/readings` path is actually reached, saving overhead on other requests. Compared to defining all nested paths in one massive controller, the pattern mirrors real-world object hierarchies, keeps each class focused, and avoids the maintainability problems of god-classes.

---

### Part 5.1 — Why HTTP 422 is More Semantically Accurate than 404

HTTP `404 Not Found` signals that the **request URL itself** was not found on the server — the endpoint does not exist. In the scenario where a client POSTs a valid JSON body to `/api/v1/sensors` but the `roomId` field references a room that does not exist, the URL is perfectly valid and reachable. The problem lies entirely within the **payload content**: the JSON is syntactically correct, but the referenced entity does not satisfy a business rule.

HTTP `422 Unprocessable Entity` (defined in RFC 4918) was designed precisely for this case: the server understands the content type, the request is well-formed, but it cannot process the contained instructions due to semantic errors. Using 404 here would mislead the client into thinking the endpoint is missing. Using 422 communicates accurately that the endpoint exists, the JSON was received, but validation failed on the payload's business logic. This distinction is critical for API consumers building automated systems — they would handle a 404 by checking their URL, but handle a 422 by inspecting and correcting the payload.

---

### Part 5.2 — Security Risks of Exposing Stack Traces

Exposing raw Java stack traces to external API consumers creates several significant security risks. First, traces reveal **internal file paths and package names** (e.g., `com.smartcampus.resource.RoomResource` or `/home/ubuntu/app/...`), giving an attacker a map of the server's directory structure. Second, they expose **library names and exact version numbers** (e.g., `jersey-server-2.41.jar`, `jackson-databind-2.15.2`), enabling targeted exploitation of known CVEs for those specific versions. Third, stack traces expose **class names, method names, and line numbers**, revealing business logic flow and identifying where defensive checks may be absent. Fourth, they can expose **SQL queries, internal IDs, or configuration details** in exception messages. Together, this information constitutes detailed reconnaissance that significantly lowers the effort required for a targeted attack. The `GlobalExceptionMapper` in this project eliminates this risk by catching all `Throwable` exceptions, logging the full trace server-side for operators, and returning only a generic, safe `500` message to the client.

---

### Part 5.3 — JAX-RS Filters vs. Inline Logging

Using a JAX-RS filter (`ContainerRequestFilter` + `ContainerResponseFilter`) for logging is architecturally superior to inserting `Logger.info()` statements inside every resource method for several reasons. Logging is a **cross-cutting concern** — it applies universally to all requests regardless of business logic — and should not be mixed into business code. Inserting logging manually into every method violates the **DRY (Don't Repeat Yourself)** principle: each new resource method added in future must remember to include the same boilerplate, making the approach error-prone and inconsistent. A filter is declared once and **automatically applied to every request and response** without modifying any resource class. It can also be enabled, disabled, or modified in a single place. This aligns with the **Aspect-Oriented Programming (AOP)** principle, which advocates separating cross-cutting concerns from core logic. Filters also receive the full `ContainerRequestContext` and `ContainerResponseContext`, giving access to method, URI, headers, and status code without any coupling to the resource implementation.

---

## Project Structure

```
smart-campus-api/
├── pom.xml
├── README.md
└── src/main/java/com/smartcampus/
    ├── SmartCampusApplication.java          ← JAX-RS Application + Grizzly main()
    ├── model/
    │   ├── Room.java
    │   ├── Sensor.java
    │   ├── SensorReading.java
    │   └── ErrorResponse.java
    ├── store/
    │   └── DataStore.java                   ← Singleton ConcurrentHashMap store
    ├── resource/
    │   ├── DiscoveryResource.java            ← GET /api/v1  (Part 1)
    │   ├── RoomResource.java                 ← /api/v1/rooms  (Part 2)
    │   ├── SensorResource.java               ← /api/v1/sensors  (Part 3)
    │   └── SensorReadingResource.java        ← sub-resource  (Part 4)
    ├── exception/
    │   ├── RoomNotEmptyException.java
    │   ├── LinkedResourceNotFoundException.java
    │   ├── SensorUnavailableException.java
    │   ├── RoomNotEmptyExceptionMapper.java  ← 409
    │   ├── LinkedResourceNotFoundExceptionMapper.java ← 422
    │   ├── SensorUnavailableExceptionMapper.java      ← 403
    │   └── GlobalExceptionMapper.java        ← 500 catch-all
    └── filter/
        └── LoggingFilter.java                ← request + response logging (Part 5)
```
