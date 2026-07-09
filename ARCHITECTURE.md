# mneme-core — Architecture

Authoritative design for the `mneme-core` service. This is the "how".

- **Scope and product decisions** live in `PROJECT.md`. Not repeated here.
- **Engineering standards** (Clean Architecture, SOLID, Clean Code, testing) live in
  the global `AGENTS.md`. Not repeated here — this document assumes them and only
  records project-specific structure and decisions.
- **Stack and conventions** live in the project `AGENTS.md`.

When this document conflicts with a lower-level guess, this document wins. When a
decision is genuinely not made yet, it is listed under "Open / deferred" — do not
invent it, ask.

---

## 1. Style and layering

Clean Architecture, layer-first. The dependency rule is absolute: outer layers depend
on inner layers, never the reverse. Frameworks (Spring, JPA, HTTP) are details at the
edges. The domain is the most stable layer and compiles without `jakarta.persistence`,
Spring, or any web type on its classpath.

**Three model kinds — never collapse them into fewer:**

1. **Domain model** — `Card`, `Review`, value objects, `LeitnerBox`, `Grade`. Pure
   Java, zero framework annotations. Holds behaviour and invariants.
2. **Persistence model** — JPA entities (`CardEntity`, `ReviewEntity`). Live **only**
   inside the persistence adapter. Never returned to or accepted from upper layers.
3. **DTOs** — request/response types at the REST edge. Live only in the web adapter.

Two mappings, both at the edges: domain↔persistence (in the persistence adapter) and
domain↔dto (in the web adapter). The domain in the middle knows about neither.
Mapping is written by hand to start (the models are not 1:1 — e.g. `Word` is a value
object in the domain, a `text` column in the table). Introduce MapStruct only if the
number of mappers grows enough to justify it.

---

## 2. Module and package layout

**Single Gradle module.** Multi-module (`:domain`, `:application`, `:infrastructure`)
is a recorded upgrade path — it turns the dependency rule into a compiler-enforced
wall — but is premature for a single-developer MVP. Adopt it only if the boundary
starts leaking or the k8s/deploy goal specifically calls for it.

Layer-first packages under the root package `io.github.ifamneme.mneme_core`:

```
io.github.ifamneme.mneme_core
├── domain              // entities, value objects, domain services,
│                       // repository PORT interfaces, LeitnerStrategy interface
├── application         // use cases; view models (ReviewSessionView);
│                       // transaction boundary
└── infrastructure
    ├── persistence     // JPA entities, Spring Data interfaces,
    │                   // repository port implementations, mappers
    ├── web             // controllers, request/response DTOs,
    │                   // Authenticator, auth filter
    └── config          // Spring configuration, @ConfigurationProperties
```

**Ports live with the domain, not with the adapters.** `CardRepository` (interface) is
in `domain`; `JpaCardRepository` (implementation) is in `infrastructure/persistence`.
Infrastructure depends on the domain by implementing its interfaces — this is the
Dependency Inversion made visible in the package topology. If an interface for an
outbound dependency ends up in `infrastructure`, the boundary has been drawn the wrong
way round.

---

## 3. Domain model

### Aggregates

Two aggregates, deliberately split:

- **`Card`** — the content: `word`, `translation`, `association`, optional `image`,
  `visibility`. The authoring lifecycle.
- **`Review`** — the repetition state: `box`, `dueAt`. The study lifecycle.

They are linked **only by `CardId` value** — no object references between the two
roots, and (see §6) no JPA association between their entities. The relationship is
strictly 1:1.

**Why two aggregates, not one.** Content and repetition state change independently and
for different reasons (authoring vs studying), and there is no invariant between them
that must hold atomically — there is no rule "the box must match some content field".
When two things need not be consistent within one transaction, they can be separate
aggregates. This also makes a required invariant structural rather than disciplinary:

> **Editing card content never touches review state.**

With two aggregates the content-editing path physically never loads `Review`, so it
*cannot* alter the box — the guarantee holds by construction, not by reviewer
vigilance.

What this split is **not**: not two services, not eventual consistency, not domain
events between them. Everything is synchronous, in-process, in one transaction where
needed (§4). The separation is at the aggregate/repository level only.

### Identity and ownership

- `UserId` is part of the domain from the start and is present on **both** aggregates
  (`Review` carries `owner`, not only `cardId`) — otherwise every session query would
  have to load `Card` just to check ownership, quietly re-coupling the two.
- `UserId` arrives in the application layer as an **explicit use-case parameter**. It
  is never read from a `SecurityContext` inside the domain or use case. Extraction from
  the authentication mechanism happens at the edge (§8).
- Card `id` is a UUID **generated in the domain** at creation time (the card has
  identity in memory before it is ever persisted). The database only stores it.

### Value objects

`Word`, `Translation`, `Association` are value objects, not bare strings — they make
illegal states harder to represent and keep validation in one place. `Grade` is a
binary value (`Correct` / `Incorrect`). `Visibility` is `Private` (default) / `Public`.

---

## 4. Application layer (use cases)

Use cases orchestrate the domain; they do not contain domain logic.

MVP use cases (indicative):

- **Create card** — creates `Card` and, in the **same transaction**, its initial
  `Review` (box 1, `dueAt` = today). "A card is ready to study immediately" is an
  invariant of *this use case*, not of either aggregate.
- **Get / list cards**, **update card content**, **delete card**.
- **Grade a review** — takes `(userId, cardId, grade)`; loads the `Review`, applies the
  scheduling strategy, saves. `Grade` is an incoming fact, never stored.
- **Review session query** — see below.

### Transaction boundary

Pragmatic choice: `@Transactional` on the application service (use case), and **only**
where a use case performs more than one write. Today that is exactly one place: create
card. Single-write use cases rely on the repository's own transactionality and get no
annotation. Never `@Transactional` on the domain or on controllers.

This is a conscious compromise: the application layer imports one Spring annotation and
is therefore not 100% framework-free. Building a `TransactionRunner` port for a single
site would be invented complexity. **Escape hatch:** if multi-write use cases reach
three or four, or use cases must run in tests with no Spring on the classpath,
introduce a `TransactionRunner` port (interface in `application`, Spring impl in the
adapter). Not before.

### Review session read

The session ("cards whose due date has arrived") crosses both aggregates: filter by
`Review.dueAt`, show `Card` content. It is assembled in the use case from the two
repositories — `ReviewRepository.findDue(userId, now)` then
`CardRepository.findAllByIds(...)` — and returned as a dedicated view model,
`ReviewSessionView` (`{cardId, word, translation, association, box, dueAt}`), ordered
by `dueAt`. Raw aggregates are never returned to the edge.

A dedicated read model (a `DueCardsQuery` port returning a projection in one query) is
a recorded door, **not** taken now — at family scale the two-query assembly is trivial
and it would be premature CQRS. Because the use case returns a view model rather than
raw aggregates, moving to a read model later is a local change inside one method; the
signature and everything above it stay put. **Triggers to revisit:** a second
cross-aggregate read scenario appears; the manual assembly starts carrying logic beyond
a simple zip; the dataset stops being family-sized.

---

## 5. Ports

Outbound dependencies are interfaces declared by the inner layers; adapters implement
them.

- **`CardRepository`, `ReviewRepository`** — declared in `domain`, in domain terms
  (`save(Card)`, `findById(CardId)`, `findDue(UserId, Instant)` …). No SQL, no table,
  no `JpaRepository` in their signatures. Two aggregates → two repositories.
- **`Authenticator`** — declared in the web adapter; resolves an incoming credential to
  a `UserId` (or rejects). See §8.
- **Scheduling** is a **domain Strategy behind an interface, not a port** — there is no
  external system behind it. See §9.

In MVP the only outbound port to an external system is persistence (Postgres). There is
**no image-generation port** in MVP (see §10).

---

## 6. Persistence adapter

Stack decision (this closes the open item in the project `AGENTS.md`, which forbade
persistence dependencies until decided here): **JPA/Hibernate + Flyway + PostgreSQL
driver.** These dependencies may now be added to the build.

JPA is used **narrowly**, as a plain object-to-table mapper. The domain has no object
graphs, no many-to-many, no lazy collections — none of JPA's heavy machinery is needed,
so none of it is used. The rule that keeps JPA from eroding the architecture:

> All JPA lives inside the persistence adapter and leaves it via no class, interface,
> or annotation.

Concrete rules:

- **Separate entities.** `CardEntity` / `ReviewEntity` carry the JPA annotations; the
  domain `Card` / `Review` stay annotation-free. Do not annotate the domain types.
- **Repository port vs Spring Data.** The domain port `CardRepository` is implemented by
  `JpaCardRepository` in the adapter, which internally uses a Spring Data
  `CardJpaRepository extends JpaRepository<CardEntity, UUID>` and maps entity↔domain.
  The Spring Data interface is a private detail of the adapter and does not escape the
  `persistence` package. A use case never depends on `JpaRepository`.
- **No JPA association between the two aggregates.** No `@OneToOne` / `@ManyToOne`
  between `CardEntity` and `ReviewEntity`. The link is the `card_id` value only.

### Tables

```
cards
  id           uuid        primary key
  owner_id     uuid        not null
  word         text        not null
  translation  text        not null
  association  text        not null
  image_url    text        null            -- Image: optional stored attribute
  visibility   text        not null         -- 'PRIVATE' / 'PUBLIC' (enum as STRING)
  created_at   timestamptz not null
  updated_at   timestamptz not null

reviews
  card_id      uuid        primary key, references cards(id)   -- encodes 1:1
  owner_id     uuid        not null
  box          int         not null
  due_at       timestamptz not null
```

- **`reviews` primary key is `card_id`** — the 1:1 relationship is enforced by the
  schema, not by hope. (Caveat under §10 if review *history* is ever needed.)
- A real foreign-key constraint `reviews.card_id → cards.id` exists at the database
  level for data integrity. It does **not** imply object navigation between entities.
- **Mandatory index `(owner_id, due_at)` on `reviews`** — this is the hot path
  (`findDue`) and the concrete lever for the p95 100–200 ms target (§8, §11). Without
  it the session scans the table. This is a design decision, not a later optimisation.
- **Enums are stored as strings** (`EnumType.STRING`), never ordinal — ordinal values
  break silently when an enum constant is inserted in the middle. `Grade` is not stored
  (it is an incoming fact).

### Schema management

Schema is owned by **Flyway migrations** (checked into git). Hibernate `ddl-auto:
validate` — never `update`/`create` in any real environment (schema drift and data
loss). Flyway enters the project together with JPA.

---

## 7. Web adapter (REST)

Versioned under `/v1`. `userId` never appears in paths — the caller's identity comes
from authentication (§8), not the URL. Three resources, each owning one domain verb:

**`/v1/cards`** — the card collection (content). CRUD.
- `POST /v1/cards` — create (internally one use case writes `Card` + initial `Review`;
  externally this is just "created a card").
- `GET /v1/cards`, `GET /v1/cards/{id}`
- `PUT /v1/cards/{id}` — content only. Box and due date are **not** accepted here; the
  request body has no such fields.
- `DELETE /v1/cards/{id}`

**`/v1/reviews/{cardId}`** — repetition state.
- `POST /v1/reviews/{cardId}` with body `{ "grade": "correct" }` — record the answer.
  The client sends a **fact**; the domain moves the box per Leitner. The client never
  writes `box` directly (that would push the algorithm out to the client).

**`/v1/session/cards`** — the study session (read-only).
- `GET /v1/session/cards` — the cards due now (`ReviewSessionView` projection). Returns
  `200 []` when nothing is due (a valid state, not an error). `session/` is a namespace
  reserved for later session-scoped endpoints (`session/stats`, …); it is a
  read/scenario resource and must not gain write endpoints — grading stays on
  `/v1/reviews`.

The core exposes universal, domain-shaped resources — never endpoints shaped to a
particular front-end screen. Aggregation for a specific client (a "give me everything
for this screen" call) is a **BFF concern and lives outside the core** (e.g. in
`mneme-web`), not in `mneme-core`.

**Controller contract split.** Each resource is declared as a dedicated `*Api`
interface (e.g. `MeApi`) carrying the method-level mapping annotations
(`@GetMapping`, `@PostMapping`, …); the `*Controller` class (`@RestController`,
class-level `@RequestMapping`) implements it. This separates the REST contract
(method signature + mapping) from the handler logic, and gives later slices a
single place to regenerate or diff the contract against an OpenAPI spec if that
becomes useful. Spring resolves the mapping annotations through the interface, so
no behavior changes — this is a structural convention, not a runtime concern.

---

## 8. Authentication

Mechanism for MVP: **a static per-user token**, sent as `Authorization: Bearer
<token>`. This is a deliberate "simple authentication" choice (per `PROJECT.md` §11);
no passwords, no hashing, no login flow.

- Token verification and `UserId` extraction happen **only** in `infrastructure/web`,
  behind the `Authenticator` port (credential → `UserId`, or reject with `401`). The
  use case receives `UserId` as a parameter; the domain and application never see the
  token.
- **Users are seeded** (via migration/config); there is no self-registration endpoint
  in MVP.
- **Secrets from the environment** — tokens are never in code or git.
- The mechanism is a replaceable detail. Upgrading to real authentication does not
  touch the domain or use cases.

---

## 9. Scheduling (Leitner)

The review algorithm is a **domain Strategy behind an interface**, designed strictly
for MVP needs (Leitner) — no speculative generalisation for future algorithms (YAGNI).
The interface is essentially one operation:

> `(current ReviewState, Grade) → new ReviewState`

Given where the card is and how it was answered, produce the new box and `dueAt`. No
SM-2 fields (ease factor, etc.) — they are not part of the interface.

Policy and rules:

- **Global policy**, not per-user configuration.
- **5 boxes.** Intervals are **configurable** in `application.yaml`, default `1 / 3 / 7
  / 16 / 30` days. `dueAt = answer time + interval(new box)`.
- `Correct` → promote to the next box. `Incorrect` (any box) → reset to box 1.
- **Last box:** `Correct` keeps the card there at the maximum interval — there is no
  separate "mastered" state in MVP.
- Parameters reach the domain as **plain values**: `LeitnerStrategy` is constructed
  with the intervals; a `@ConfigurationProperties` bean in `infrastructure/config`
  reads them from `application.yaml` and injects them. The domain knows nothing of
  Spring or YAML.

---

## 10. Phase-2 seams (present as boundaries, not built now)

The following are explicitly **not** built in MVP; they are noted so the seams are in
the right place:

- **Image generation.** In MVP `Image` is only an optional stored attribute (URL or
  upload). Phase 2 introduces an outbound `ImageGenerator` port whose adapter calls the
  separate `mneme-ai` service; the core does not depend on it. There is no such port
  now.
- **Auth service.** The `Authenticator` port is the seam. Today its implementation is a
  local token lookup; a future dedicated auth service becomes a new implementation
  (an HTTP call) of the same port — nothing else changes. Build the core as a single
  service first; extract auth as a *later* step, after the core works.
- **Session read model.** See §4 — the `ReviewSessionView` return type is the seam for
  moving to a dedicated read/CQRS projection if the triggers fire.
- **Review history.** The `reviews` PK on `card_id` binds one review row per card. If
  Phase 2 ever needs a history of reviews per card, this PK must become a surrogate key
  with a unique index elsewhere. Not a concern for MVP.

---

## 11. Non-functional notes

- **Latency** (p95 ~100–200 ms) is addressed by the `(owner_id, due_at)` index and
  simple queries, not by horizontal scaling. Throughput is not a goal (family scale).
- **Durability.** The card collection must survive. This is an **infrastructure
  concern**, not application code — there is no `BackupService` in the domain or
  adapters. It has two halves, both covered: the **schema** is reproducible from Flyway
  migrations (in git); the **data** is protected by PostgreSQL backups kept **off-host**
  (self-hosted: scheduled `pg_dump`, optionally WAL archiving for PITR; managed
  Postgres: provider snapshots + PITR). A backup that has never been restored is not a
  backup — restore is tested. **No HA / replication / multi-region** (per `PROJECT.md`
  §8). The mechanism is chosen at deploy time.

---

## 12. Open / deferred (decide at implementation time, keep consistent)

- - **Error responses** — decided: `application/problem+json` (`ProblemDetail`) with a
    custom `code` property carrying a machine-readable `ErrorCode`. `type` stays
    `about:blank`. Domain invariant violations are not translated to `4xx`; they surface
    as `500`, because reaching the domain with invalid data means edge validation failed.
- **Validation placement** — field-level validation at the edge (Jakarta Validation on
  request DTOs) and/or via value-object construction in the domain. Decide per field;
  do not duplicate the same rule in both places without reason.
- **Session sizing** — currently "all cards due now, ordered by `dueAt`". Add a
  limit/paging only if a real need appears.
- **User provisioning details** — seeded users' exact source (migration row vs config)
  and token issuance procedure.