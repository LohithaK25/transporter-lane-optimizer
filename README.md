# FreightFox Transport Optimizer

A Spring Boot REST API that stores trade lanes and transporter quotes, then computes a cost-aware assignment with full lane coverage and a configurable cap on the number of distinct transporters used.

## Features

- Submit and replace lane, transporter, and quote data through a REST endpoint.
- Validate lane IDs, transporter IDs, quote references, and duplicate lane/transporter quotes before replacing stored data.
- Assign exactly one transporter to each lane.
- Maximize the number of distinct transporters used up to the requested limit, then minimize total quote cost among assignments that achieve that utilization.
- Use `BigDecimal` for quote and total-cost calculations.
- Return deterministic assignments ordered by lane ID, with selected transporter IDs ordered consistently.
- Generate OpenAPI documentation and an interactive Swagger UI.
- Run integration tests against an isolated H2 database; run the application against PostgreSQL.

## Technology

- Java 21 language target (the development machine used Java 23)
- Spring Boot 4.2.0
- Spring MVC and Jakarta Bean Validation
- Spring Data JPA / Hibernate
- PostgreSQL 17 for local development
- H2 for automated tests
- Maven Wrapper
- Docker Compose
- springdoc-openapi / Swagger UI

## Architecture

The application follows a layered structure:

```text
com.freightfox.transporter
├── api
│   ├── controller       # HTTP endpoints
│   └── dto              # Request and response contracts
├── config               # OpenAPI configuration
├── domain
│   ├── entity           # Lane, Transporter, LaneQuote
│   └── repository       # JPA repositories
├── exception            # Domain exceptions and global HTTP error mapping
└── service
    ├── optimization     # Exact bitmask dynamic-programming optimizer
    ├── TransporterInputService
    └── TransporterAssignmentService
```

### Request flow

**Input:** Controller validates the request DTO, then `TransporterInputService` performs cross-record validation and replaces the persisted dataset in a transaction. Quotes are removed before their parent records so dependent data can be safely replaced. Invalid input is rejected before the old dataset is deleted.

**Assignment:** `TransporterAssignmentController` calls `TransporterAssignmentService`, which reads the lane and quote data in a read-only `REPEATABLE_READ` transaction. The optimizer computes the assignments, and the service maps the result into the API response DTO.

## Optimization approach

The optimizer uses exact dynamic programming with a bitmask representing the set of distinct transporters used so far.

1. Sort lane and transporter IDs to keep output deterministic.
2. Process lanes one at a time. Each lane must be assigned to one transporter that has submitted a quote for that lane.
3. A DP state is keyed by a transporter-use bitmask and stores the lowest cost found for that state, along with predecessor information needed to reconstruct assignments.
4. After all lanes are processed, consider only states that use no more than `maxTransporters` distinct transporters.
5. Prefer states with the largest number of distinct transporters. Among states with that utilization, choose the lowest total cost.
6. Reconstruct one transporter assignment for every lane.

### Objective interpretation

The implementation treats full lane coverage as a hard constraint. It then maximizes distinct transporter usage up to `maxTransporters`, and minimizes total cost among assignments with that maximum feasible usage. Consequently, increasing transporter utilization can increase the total cost. This is the explicit tie-break interpretation used by the implementation for the brief's two optimization objectives.

The supplied assignment PDF's Test Case 1 example response appears internally inconsistent with its own input: the listed five assignments use transporter IDs 4, 6, 2, and 3 (four distinct transporters) despite a limit of three, and the listed quotes sum to 69,001 rather than the stated 75,000. The implementation prioritizes the written constraints and returns a self-consistent assignment rather than reproducing that inconsistent example response.

### Complexity and supported size

Let:

- `L` = number of lanes
- `T` = number of active transporters (transporters with at least one quote)

There are at most `2^T` transporter-use masks. Each lane can transition through the available transporter quotes, giving a worst-case time bound of `O(L × T × 2^T)`. The optimizer currently guards the exact bitmask approach at **16 active transporters**. This is an intentional bound to keep the state space manageable; larger transporter sets require a different optimization strategy or an integer-programming solver.

## API

Base URL for local development: `http://localhost:8080`

### 1. Submit input data

`POST /api/v1/transporters/input`

Example request shape:

```json
{
  "lanes": [
    { "id": 1, "origin": "Mumbai", "destination": "Delhi" }
  ],
  "transporters": [
    {
      "id": 1,
      "name": "Transporter T1",
      "laneQuotes": [
        { "laneId": 1, "quote": 5000 }
      ]
    }
  ]
}
```

Success response (`200 OK`):

```json
{
  "status": "success",
  "message": "Input data saved successfully."
}
```

The endpoint **replaces** the currently stored dataset; it does not append to it. Validation failures return `400 Bad Request`. An invalid request must not partially replace the previously stored data.

### 2. Get optimized assignments

`POST /api/v1/transporters/assignment`

Request:

```json
{
  "maxTransporters": 3
}
```

Example response for the supplied nine-lane dataset:

```json
{
  "status": "success",
  "totalCost": 134876,
  "assignments": [
    { "laneId": 1, "transporterId": 4 },
    { "laneId": 2, "transporterId": 1 },
    { "laneId": 3, "transporterId": 4 },
    { "laneId": 4, "transporterId": 4 },
    { "laneId": 5, "transporterId": 4 },
    { "laneId": 6, "transporterId": 1 },
    { "laneId": 7, "transporterId": 1 },
    { "laneId": 8, "transporterId": 6 },
    { "laneId": 9, "transporterId": 6 }
  ],
  "selectedTransporters": [1, 4, 6]
}
```

If no feasible full-coverage assignment exists, the API returns `422 Unprocessable Content`. Invalid request bodies or validation errors return `400 Bad Request`.

### Error response shape

```json
{
  "status": "error",
  "message": "A useful validation or optimization message"
}
```

| Status | Meaning |
|---|---|
| `200` | Request completed successfully |
| `400` | Invalid request, malformed JSON, or invalid input data |
| `422` | No feasible assignment can be generated |
| `500` | Unexpected server error; implementation returns a generic client message and logs the exception server-side |

## Local setup

### Prerequisites

- JDK 21 or later
- Docker Desktop with Docker Compose
- PowerShell (commands below use PowerShell syntax)

### 1. Start PostgreSQL

From the project root:

```powershell
docker compose up -d
docker compose ps
```

The Compose configuration publishes PostgreSQL on host port **5433** because a local PostgreSQL instance may already occupy port 5432. The container itself listens on port 5432.

### 2. Configure the database connection

The default local application configuration points to:

```text
jdbc:postgresql://127.0.0.1:5433/freightfox
```

It uses the `freightfox` database/user configured for local development. You can override the connection with environment variables:

```powershell
$env:DB_URL = "jdbc:postgresql://127.0.0.1:5433/freightfox"
$env:DB_USERNAME = "freightfox"
$env:DB_PASSWORD = "<your-local-db-password>"
```

Do not use local development credentials in a deployed environment. Supply production credentials through the hosting platform's secret manager or environment configuration.

### 3. Run the application

```powershell
.\mvnw.cmd spring-boot:run
```

The API starts on port `8080`.

### 4. Open Swagger UI

- Swagger UI: <http://localhost:8080/swagger-ui/index.html>
- OpenAPI JSON: <http://localhost:8080/v3/api-docs>
- Actuator health: <http://localhost:8080/actuator/health>

### 5. Run tests

```powershell
.\mvnw.cmd clean test
```

The tests use the `test` profile and an in-memory H2 database; they do not require the PostgreSQL container.

## Sample data and reproducing the supplied CSV result

Both the original assignment dataset and a ready-to-submit API payload are included in `sample-data/`:

```text
sample-data/
├── transporter_assignment_data.csv  # Original assignment file, kept unchanged
└── transporter-input.json           # Equivalent JSON request for the input API
```

The CSV is the source dataset supplied with the assignment. It uses a wide format: each row represents a trade lane, and columns `T1` through `T7` contain that transporter's quote for the lane. It contains 9 lanes, 7 transporter columns, and 63 quotes.

The API currently accepts JSON; it does not expose a CSV-upload endpoint. `transporter-input.json` is the normalized representation of the CSV data that can be submitted directly to `POST /api/v1/transporters/input`. The CSV is included for source-data traceability and inspection; the JSON is included for easy API testing.

The source CSV does not contain geographic origin/destination values. The JSON therefore uses `Not provided in source CSV` for those descriptive fields. The optimizer uses lane IDs and quotes, so these placeholders do not affect the assignment result.

With that input submitted and `maxTransporters` set to `3`, the expected optimized result is:

- Total cost: **134,876**
- Selected transporter IDs: **[1, 4, 6]**

| Lane ID | Transporter ID | Quote |
|---:|---:|---:|
| 1 | 4 | 14,400 |
| 2 | 1 | 10,512 |
| 3 | 4 | 14,316 |
| 4 | 4 | 10,678 |
| 5 | 4 | 13,032 |
| 6 | 1 | 13,567 |
| 7 | 1 | 10,015 |
| 8 | 6 | 10,492 |
| 9 | 6 | 37,864 |
| **Total** |  | **134,876** |

From PowerShell, submit the complete input file and then request an assignment:

```powershell
$inputBody = Get-Content -Raw .\sample-data\transporter-input.json

Invoke-RestMethod `
  -Uri "http://localhost:8080/api/v1/transporters/input" `
  -Method Post `
  -ContentType "application/json" `
  -Body $inputBody

Invoke-RestMethod `
  -Uri "http://localhost:8080/api/v1/transporters/assignment" `
  -Method Post `
  -ContentType "application/json" `
  -Body '{"maxTransporters":3}'
```

> **Important:** submitting this endpoint replaces whatever dataset is currently stored. Use the sample file when you intentionally want to load the supplied CSV data.

## Production hardening considerations

Before using this service in production:

- Replace `spring.jpa.hibernate.ddl-auto=update` with controlled schema migrations (for example, Flyway or Liquibase) and a validation-oriented Hibernate setting.
- Keep database credentials outside source control and use managed secrets.
- Add authentication and authorization to both write and assignment endpoints.
- Restrict request sizes and validate operational limits based on measured workload.
- Add structured logs, metrics, tracing, database backups, and deployment health checks.
- Revisit the 16-transporter exact-DP bound if production workloads can exceed it; choose a scalable solver or optimization service.

