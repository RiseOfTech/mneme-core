# Error handling — spec

## Goal

Give the API one consistent error contract before more endpoints exist, and close two
findings from the review of the Card slice. Every error response leaves the service in
the same shape; no internal detail leaks; the `/actuator/health` probe stops being
rejected.

This slice adds no domain logic and no new endpoint.

## In scope

### 1. Error response contract

All error responses use `application/problem+json` via Spring's `ProblemDetail`
(RFC 7807). Fields:

- `status` — the HTTP status.
- `title` — short, human-readable, stable per error class.
- `detail` — human-readable specifics. Never contains stack traces, exception class
  names, SQL, or internal identifiers belonging to other users.
- `code` — an additional custom property carrying a machine-readable error code, so
  clients branch on `code` rather than on `title` text.
- `type` — left at the default (`about:blank`). Do not invent URIs.

`ErrorCode` — an enum in `infrastructure/web`, the single place where every API error
code is declared. Handlers reference its constants; no error-code strings are written
inline. Initial constants: `CARD_NOT_FOUND`, `VALIDATION_FAILED`, `INTERNAL_ERROR`.

### 2. Exception translation

`ApiExceptionHandler` (`@RestControllerAdvice`, replacing the current minimal one)
handles exactly three cases:

- **`CardNotFoundException`** → `404`, code `CARD_NOT_FOUND`.
  `detail` states that the card was not found. It must **not** echo the card id or any
  owner information — a foreign card and a missing card are indistinguishable to the
  caller (PROJECT.md §4).
- **`MethodArgumentNotValidException`** (bean validation on a request DTO failed) →
  `400`, code `VALIDATION_FAILED`. The response lists which fields failed and why, as
  a structured property (e.g. `errors: { "word": "must not be blank" }`). Field names
  are the DTO's, not the domain's.
- **`Exception`** (catch-all) → `500`, code `INTERNAL_ERROR`. `detail` is a fixed
  generic string. The exception's own message, class name, and stack trace are **never**
  serialised. The full exception **is** logged at ERROR level, with the stack trace.

Domain exceptions (e.g. `IllegalArgumentException` from `Word`, `LeitnerBox`) are
deliberately **not** mapped to `400`. Input is validated at the edge; if invalid data
reaches the domain, that is a bug, and the catch-all `500` is the correct signal. Do
not add handlers that convert domain invariant violations into client errors.

### 3. Fix: the auth filter must not block actuator (review MAJOR)

`AuthenticationFilter` currently applies to every URL, so `GET /actuator/health` — the
k8s liveness/readiness probe named in the project `AGENTS.md` — receives `401`. Probes
carry no bearer token; in a cluster this restarts the pod in a loop.

Fix by overriding `shouldNotFilter` to skip paths under `/actuator`. Do not introduce
`FilterRegistrationBean`; keep the exclusion inside the filter, next to the logic it
qualifies.

Add a test asserting `GET /actuator/health` returns `200` without an `Authorization`
header. Without it, this regresses silently.

### 4. Fix: the domain `Visibility` enum must not appear in web DTOs (review MINOR)

`CreateCardRequestDto` and `CardResponseDto` currently import and expose the domain
`Visibility` enum, so the wire contract is dictated by a domain type. The persistence
adapter already duplicates it correctly (`CardVisibility`); apply the same discipline
at the web edge.

Introduce a web-local enum (`VisibilityDto`) with the same constants, mapped to and
from the domain `Visibility` in the web layer. Renaming a domain constant must not be
able to break the public API.

## Out of scope

- No new endpoints, no grading, no session, no card list/update/delete.
- No `type` URIs, no error documentation pages.
- No internationalisation of messages.
- No retry, circuit-breaker, or resilience concerns.
- Do not change `LeitnerBox`, `LeitnerScheduler`, or any domain type except as required
  by item 4 (which should require no domain change at all).
- Do not delete or rewrite files outside those this spec names.

## Also fix (review NIT)

`LeitnerBox.reset()` is currently unused because `LeitnerScheduler.next()` calls
`LeitnerBox.first()` directly on the incorrect-grade path. Do **not** delete `reset()`.
Instead, call it from `next()`: resetting is an operation of the box, not knowledge the
scheduler should hold about which box is first. Behaviour is unchanged; the existing
tests must still pass.

## Acceptance criteria

1. `./gradlew clean build` is green.
2. `GET /actuator/health` returns `200` with no `Authorization` header.
3. `GET /v1/cards/{id}` for a missing card returns `404`, content type
   `application/problem+json`, with `code: CARD_NOT_FOUND` and no card id in the body.
4. `GET /v1/cards/{id}` for a card owned by another user returns a response
   **byte-identical** to the missing-card case.
5. `POST /v1/cards` with a blank `word` returns `400`, `code: VALIDATION_FAILED`, and
   names `word` among the failing fields.
6. An endpoint that throws an unexpected exception returns `500` with
   `code: INTERNAL_ERROR`, and the response body contains neither the exception
   message nor its class name. The exception is present in the logs.
7. Web DTOs contain no import of the domain `Visibility` enum.
8. `LeitnerBox.reset()` is referenced by `LeitnerScheduler.next()`.

## Notes

- `ProblemDetail` is a web-layer concern. `CardNotFoundException` stays in
  `application` and knows nothing about HTTP or `ProblemDetail`.
- Criterion 6 needs a test that provokes an unexpected exception — mock a use case to
  throw a `RuntimeException` in a `@WebMvcTest`, and assert on the body.
- Once merged, record in `ARCHITECTURE.md` §12 that the error-body shape is decided:
  `application/problem+json` with a custom `code` property. That entry stops being an
  open question.