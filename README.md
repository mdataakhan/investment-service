# Investment Service

A Spring Boot microservice for creating, funding, and settling investment funding requests backed by MongoDB. It exposes REST APIs to:

- Create funding requests (by a funder)
- List all requests
- Fetch a request by ID
- Update a request (owned by funder)
- Invest in a request (by investors)
- Distribute returns to investors (debits funder, credits investors)
- Fetch all requests created by a specific funder (via header or path variant)

## Tech Stack

- Java 21
- Spring Boot 3 (Web, WebFlux for reactive WebClient, Validation)
- Spring Data MongoDB
- Logback (JSON encoder)
- OpenAPI UI (springdoc)

## Project Structure

- `src/main/java/com/nexus/investment_service`
  - `InvestmentServiceApplication.java` — application entrypoint
  - `config/WebClientConfig.java` — configures `WebClient` for user service calls
  - `controller/FundingRequestController.java` — HTTP endpoints
  - `dto/*` — request DTOs
  - `model/FundingRequest.java` — MongoDB document model
  - `repository/FundingRequestRepository.java` — Mongo repository
  - `service/FundingRequestService.java` — core domain logic
  - `utils/Constants.java` — status constants and user service base URL
  - `utils/Validation.java` — validations (ownership, state, amounts)
- `src/main/resources`
  - `application.properties` — Spring/MongoDB configuration
  - `logback-spring.xml` — logging configuration
  - `static/apispec.yaml` — API spec (optional UI)

## Configuration

The service expects a running MongoDB and reaches an external User service to adjust wallets.

Environment/configuration you may need (examples):

- MongoDB URI (set in `application.properties` or env):
  - `spring.data.mongodb.uri=mongodb://localhost:27017/investment-db`
- Server port (optional):
  - `server.port=8080`
- User service base URL (defaults to `http://localhost:3000/api/v1/users`, see `utils/Constants.USER_SERVICE_BASE_URL`). Override by editing `Constants.java` or wiring a property.

## Domain Model: `FundingRequest`

Key fields:
- `id` (String)
- `title` (String)
- `requiredAmount` (double)
- `currentFunded` (double)
- `funderId` (String)
- `status` (OPEN | FUNDED | CLOSED)
- `createdAt` (LocalDateTime)
- `deadline` (LocalDateTime)
- `investorAmounts` (Map<investorId, investedAmount>)
- `committedReturnAmount` (double)
- `description` (String)
- `returnDistributed` (boolean)

## API Endpoints

Base path: `/api/v1/funding-requests`

- POST `/` — Create a funding request
  - Headers: `X-User-Id: <funderId>`
  - Body: `{ title, requiredAmount, deadline, committedReturnAmount, description }`
  - Responses: `201 Created` with created `FundingRequest`

- GET `/` — List all funding requests
  - Responses: `200 OK` with `FundingRequest[]`

- GET `/{requestId}` — Get funding request by ID
  - Path: `requestId`
  - Responses: `200 OK`, `404 Not Found` if missing

- PUT `/{requestId}` — Update a funding request (title/deadline/description/committedReturnAmount)
  - Headers: `X-User-Id: <funderId>` (must match owner)
  - Body: partial fields
  - Responses: `200 OK`, `400 Bad Request` if not OPEN, `403/400` on validation, `404` if missing

- POST `/{requestId}/investment` — Invest in a funding request
  - Headers: `X-User-Id: <investorId>`
  - Body: `{ walletAdjustment: -<amount> }` (negative to deduct from investor wallet)
  - Effects:
    - Deducts investor wallet via User service
    - Accumulates `currentFunded` and `investorAmounts`
    - If fully funded, credits funder with total raised amount and sets status to `FUNDED`
  - Responses: `200 OK`, `400 Bad Request` on validation

- POST `/{requestId}/distribute-returns` — Distribute returns after funding
  - Preconditions: status `FUNDED`, not previously distributed, has investors
  - Effects:
    - Debits funder: `currentFunded + committedReturnAmount`
    - Credits each investor with principal + pro-rata share of committed return
  - Responses: `200 OK`, `400 Bad Request` on validation

- GET `/mine` — List funding requests for the current funder
  - Headers: `X-User-Id: <funderId>`
  - Responses: `200 OK` with `FundingRequest[]`

Alternative path form (if preferred in another repo):
- GET `/funder/{funderId}` — List funding requests by funderId (can be added alongside `/mine`)

## Validation & Error Handling

- Creation/update uses Bean Validation annotations in DTOs.
- Service enforces:
  - Ownership for updates (`funderId` must match)
  - Request must be `OPEN` to update/invest
  - Investment amount sanity and caps (via `Validation` helpers)
  - Clear `ResponseStatusException` with proper HTTP codes

## OpenAPI UI

Springdoc is included. If enabled via configuration, visit:
- Swagger UI: `http://localhost:8080/swagger-ui/index.html`
- OpenAPI JSON: `http://localhost:8080/v3/api-docs`

## Run Locally

Prerequisites:
- Java 21
- Maven 3.9+
- MongoDB server running (e.g., on `localhost:27017`)
- The external User service reachable at `Constants.USER_SERVICE_BASE_URL` (default `http://localhost:3000/api/v1/users`) supporting `PUT /{userId}` with wallet adjustment payload

Build & run:

```zsh
# From project root
./mvnw clean package
./mvnw spring-boot:run
```

Alternatively, run the JAR:

```zsh
java -jar target/investment-service-0.0.1-SNAPSHOT.jar
```

## Quick Smoke Tests (curl)

Create a funding request:
```zsh
curl -sS -X POST http://localhost:8080/api/v1/funding-requests \
  -H 'Content-Type: application/json' \
  -H 'X-User-Id: FUND123' \
  -d '{
    "title": "Solar Project",
    "requiredAmount": 5000.0,
    "deadline": "2030-01-01T00:00:00",
    "committedReturnAmount": 500.0,
    "description": "Install solar panels"
  }'
```

List all requests:
```zsh
curl -sS http://localhost:8080/api/v1/funding-requests | jq
```

Fetch by ID:
```zsh
curl -sS http://localhost:8080/api/v1/funding-requests/REQUEST_ID | jq
```

Update a request:
```zsh
curl -sS -X PUT http://localhost:8080/api/v1/funding-requests/REQUEST_ID \
  -H 'Content-Type: application/json' \
  -H 'X-User-Id: FUND123' \
  -d '{ "title": "Solar Project Phase 2" }'
```

Invest:
```zsh
curl -sS -X POST http://localhost:8080/api/v1/funding-requests/REQUEST_ID/investment \
  -H 'Content-Type: application/json' \
  -H 'X-User-Id: INVEST456' \
  -d '{ "walletAdjustment": -2500.0 }'
```

Distribute returns (after funded):
```zsh
curl -sS -X POST http://localhost:8080/api/v1/funding-requests/REQUEST_ID/distribute-returns
```

List requests by funder (current user):
```zsh
curl -sS http://localhost:8080/api/v1/funding-requests/mine -H 'X-User-Id: FUND123' | jq
```

## Testing

Run unit tests:
```zsh
./mvnw test
```

## Notes & Extensibility

- Authentication/authorization can be layered via a gateway or Spring Security to validate the `X-User-Id` header.
- Consider replacing `Constants.USER_SERVICE_BASE_URL` with a configurable property and service discovery.
- Pagination and filtering can be added to listing endpoints.
- Add indexes on `funderId` in MongoDB for large datasets.