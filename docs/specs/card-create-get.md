# Card: create + get by id — spec

## Goal

The first real domain slice. Introduce the `Card` and `Review` aggregates, their
repository ports and JPA adapters, the Leitner scheduling strategy, and two use cases
exposed over REST. This proves the full vertical stack — domain → application →
adapters — on proven infrastructure (the walking skeleton).

## In scope

### Endpoints

- `POST /v1/cards` → `201` with the created card. Body: word, translation,
  association, optional imageUrl, optional visibility (defaults to `PRIVATE`).
  Creating a card also creates its initial `Review` in the same transaction.
- `GET /v1/cards/{id}` → `200` with the card.
- `GET /v1/cards/{id}` for a card that does not exist **or belongs to another user**
  → `404`. Never `403`: a foreign card must not be distinguishable from a missing one
  (cards are siloed, PROJECT.md §4).
- Both endpoints require a valid bearer token (401 otherwise, already handled by the
  existing filter).

### Domain

- `Card` — aggregate root: `CardId`, `UserId owner`, `Word`, `Translation`,
  `Association`, optional `Image`, `Visibility`. Validates its invariants on
  construction. `CardId` is a UUID generated in the domain at creation time.
- `Review` — separate aggregate root: `CardId cardId`, `UserId owner`, and a
  `ReviewState`. Holds **no object reference** to `Card` — the link is the `CardId`
  value only (ARCHITECTURE §3).
- `ReviewState` — value object: `LeitnerBox box`, `Instant dueAt`.
- `Word`, `Translation`, `Association` — value objects wrapping non-blank text.
- `Visibility` — enum `PRIVATE` / `PUBLIC`. `PRIVATE` is the default; **no logic keys
  off it in MVP**.
- `LeitnerBox` — value object over the box number; knows nothing about intervals.
- Ports declared in `domain`: `CardRepository`, `ReviewRepository`.

### Scheduling

- `LeitnerStrategy` — domain strategy interface, two operations:
    - `initialState(Instant now)` → the starting `ReviewState` (box 1, `dueAt = now +
    interval(box 1)`).
    - `next(ReviewState current, Grade grade, Instant now)` → the next `ReviewState`.
      Declared now so `ReviewState` is designed against the full contract, **but not
      exercised by this slice** — no grading endpoint here.
- `LeitnerScheduler` implements it (named for its mechanism, not `Default`).
  Constructed with the interval list; a `@ConfigurationProperties` type in
  `infrastructure/config` reads `app.leitner.intervals` from `application.yaml`
  (5 boxes, default `1, 3, 7, 16, 30` days) and supplies them. The domain sees plain
  values, never Spring or YAML (ARCHITECTURE §9).
- `Grade` — enum `CORRECT` / `INCORRECT`. Declared for the strategy signature; never
  persisted.

### Application

- `CreateCardUseCase` (interface, one method) → `DefaultCreateCardUseCase`.
  Takes `(UserId owner, <card fields>)`. Creates the `Card`, asks `LeitnerStrategy`
  for the initial `ReviewState`, builds the `Review`, saves both. Annotated
  `@Transactional` — this is the one multi-write use case in the MVP (ARCHITECTURE §4).
- `GetCardUseCase` (interface, one method) → `DefaultGetCardUseCase`.
  Takes `(UserId owner, CardId id)`, returns the card or signals not-found.
- Use cases receive `UserId` as an **explicit parameter**. They never read a
  `SecurityContext`.
- Not-found is signalled with a domain/application exception (e.g.
  `CardNotFoundException`), translated to `404` at the web edge — the use case does
  not know about HTTP.

### Persistence

- `CardEntity`, `ReviewEntity` — JPA entities, **only** in
  `infrastructure/persistence`. The domain types carry no JPA annotations.
- **No `@OneToOne` / `@ManyToOne` between them.** The link is a `card_id` column
  (ARCHITECTURE §6).
- `JpaCardRepository` / `JpaReviewRepository` implement the domain ports, each using a
  Spring Data interface internally as a private detail of the adapter. A use case
  never sees `JpaRepository`.
- `CardMapper` / `ReviewMapper` — interfaces, with `DefaultCardMapper` /
  `DefaultReviewMapper` mapping domain↔entity by hand.
- `CardRepository.findByIdAndOwner(CardId, UserId)` — ownership is part of the port
  contract, so a foreign card is never loaded in the first place.
- Migration `V2__cards_and_reviews.sql` creating both tables exactly as specified in
  ARCHITECTURE §6, including:
    - `reviews.card_id` as **primary key**, with a FK to `cards(id)`
    - the mandatory index `(owner_id, due_at)` on `reviews`
    - enums stored as strings

### Web

- `CardApi` (interface) → `CardController`. Request/response DTOs in
  `infrastructure/web`. Domain types are never serialised directly.
- A `HandlerMethodArgumentResolver` that resolves the authenticated `UserId` into a
  controller method parameter, replacing the `HttpServletRequest` + `instanceof`
  pattern used in `MeController`. Refactor `MeController` to use it as well, so there
  is exactly one way to obtain the caller's identity.
- Bean validation on request DTOs for field-level constraints (non-blank word,
  translation, association).

### Tests

- Domain unit tests, plain JUnit, **no Spring context**: `Card` invariants; `Word` /
  `Translation` / `Association` rejecting blank input; `LeitnerScheduler.initialState`
  producing box 1 with the configured first interval.
- Use-case unit tests with mocked ports: `DefaultCreateCardUseCase` saves both
  aggregates; `DefaultGetCardUseCase` signals not-found for a foreign or missing card.
- Persistence integration tests with Testcontainers: save + load round-trip for both
  repositories; `findByIdAndOwner` returns empty for another owner's card.
- Web-layer tests with the use cases mocked: `201` on create, `200` on get, `404` on
  missing/foreign, `401` without a token.

## Out of scope

- No `list`, `update`, or `delete` of cards — later slices.
- No `/v1/reviews` grading endpoint, no `/v1/session/cards` — later slices.
  `LeitnerStrategy.next` stays unused and untested beyond a trivial unit test.
- No sharing, no visibility-based filtering (the flag is stored, nothing reads it).
- No Docker Compose, no Dockerfile. Testcontainers starts PostgreSQL from the test.
- No image upload handling — `Image` is an optional URL attribute for now.
- Do not delete or rewrite files outside those this spec introduces or names.

## Acceptance criteria

1. `./gradlew clean build` is green (a plain `build` may skip tests).
2. `POST /v1/cards` with a valid token creates a card and its initial review; the
   response is `201` and the card is retrievable by its id.
3. `GET /v1/cards/{id}` returns `200` for the owner, `404` for a missing id, and
   `404` for a card owned by a different user.
4. A newly created card has a review in box 1 with `dueAt = creation time + the first
   configured interval`.
5. Editing is out of scope, but verify by inspection that no code path lets a card
   write touch `ReviewState`, and vice versa.
6. The domain source set contains no imports of `jakarta.persistence`, Spring, or any
   web type.
7. `MeController` no longer takes `HttpServletRequest`.

## Notes

- Follow the interface and naming rules in the project `AGENTS.md`: use cases and
  mappers are interfaces; aggregates, value objects, entities and DTOs are concrete;
  `Default` prefix where the implementation has nothing else to say.
- Time enters the domain as a parameter. A `Clock` bean is defined in
  `infrastructure/config` and injected where "now" is needed, so tests can pin it.
- Suggested order: domain (+ its tests) → migration → persistence (+ integration
  tests) → application (+ tests) → web (+ tests). Compile after each layer.