# Walking skeleton — spec

## Goal

Prove the end-to-end wiring of the service on the thinnest possible slice, before any
real domain logic. One authenticated endpoint returns the caller's identity, and the
app boots against a real PostgreSQL in an integration test. This de-risks the
infrastructure (web + auth edge + datasource + Flyway + Testcontainers) so later slices
add domain logic onto proven pipes.

## In scope

- `GET /v1/me` → `200 { "userId": "<uuid>" }` for a request with a valid
  `Authorization: Bearer <token>`.
- `401` for a missing or unknown token.
- Identity resolution at the edge: an `Authenticator` port (declared in the web
  adapter) maps a bearer token to a `UserId`. The implementation resolves against a
  seeded token→userId map bound from configuration.
- Persistence bootstrap only: the application connects to PostgreSQL on startup, Flyway
  is enabled, `ddl-auto: validate`. The baseline migration may be empty — the first
  real tables arrive with the Card slice.
- Tests: a web-layer test for `/v1/me` (200 and 401, no database), and one integration
  test that boots the full context against PostgreSQL via Testcontainers and calls
  `/v1/me`.

## Out of scope

- No domain aggregates, no `Card` / `Review`, no domain tables (they belong to the Card
  slice).
- No application-layer use case: returning the caller's identity is an edge concern. The
  application layer gets its first real exercise in the Card slice.
- No Spring Security starter — a plain servlet filter is enough for this slice. Whether
  to adopt Spring Security later is a separate decision, not made here.
- No self-registration, no password handling (per ARCHITECTURE §8).

## Layers touched

- **domain:** `UserId` value object (wraps a UUID). The only domain type in this slice.
- **infrastructure/web:** the `Authenticator` port + a token-based implementation, a
  bearer-token filter, the `/v1/me` controller, and its response DTO.
- **infrastructure/config:** a `@ConfigurationProperties` type holding the seeded
  token→userId map.
- **resources:** `application.yaml` (datasource, Flyway, `ddl-auto: validate`, the
  seeded tokens) and a Flyway baseline migration under `db/migration`.

## Dependencies to add

Only what ARCHITECTURE §6 already authorized, plus test infrastructure:

- main: Spring Data JPA, Flyway (with the PostgreSQL module), PostgreSQL driver.
- test: Spring Boot Testcontainers support, Testcontainers JUnit 5, Testcontainers
  PostgreSQL.

Resolve exact Gradle coordinates against the Spring Boot 4.1 BOM — do not pin versions
by hand; the build fails loudly if a coordinate is wrong. Add no other dependencies.

## Acceptance criteria

1. `./gradlew build` is green.
2. `GET /v1/me` with a valid seeded token returns `200` and that token's `userId`.
3. `GET /v1/me` with no token or an unknown token returns `401`.
4. The integration test starts PostgreSQL via Testcontainers, boots the Spring context
   (datasource connects, Flyway runs, `ddl-auto: validate` passes), and asserts `200`
   from `/v1/me`.
5. Seeded tokens are bound from configuration and overridable by environment variables;
   no real secret is committed.

## Notes

- `UserId` lives in `domain` even though only the edge uses it here — it is the identity
  that later use cases receive as an explicit parameter, so it belongs to the domain from
  the start (ARCHITECTURE §3).
- Keep the `Authenticator` implementation swappable: today a config-backed lookup, later
  (Phase 2) a call to a dedicated auth service — the same port (ARCHITECTURE §10).
- No Docker Compose, no Dockerfile, no docker-compose.yml. Testcontainers starts
    PostgreSQL from the test itself; nothing else in the repo needs Docker config.
- Do not delete or rewrite existing files outside the ones this spec introduces.