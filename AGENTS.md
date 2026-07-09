# mneme-core — Project guidelines

This file extends the global engineering standards (`~/.config/opencode/AGENTS.md`).
Project-specific rules below take precedence when they conflict with global rules.

`mneme-core` is the backend service of the **Mneme** product: it owns the cards
domain, spaced repetition (Leitner), and the public REST API. Product scope lives in
`PROJECT.md`; the authoritative design lives in `ARCHITECTURE.md`. Read `ARCHITECTURE.md`
before implementing — if it is absent, the architecture has not been decided yet, so
do not invent structure: ask.

## Stack

- Language: Java 21
- Framework: Spring Boot 4.1 (Spring Framework 7, Jakarta EE 11, Jackson 3)
- Build: Gradle (Kotlin DSL)
- Persistence: PostgreSQL — **the persistence approach is not yet chosen** (see rules)
- Group: `io.github.ifamneme`
- Root package: `io.github.ifamneme.mneme_core` (sub-package layout per `ARCHITECTURE.md`)

## Build and run

- Build: `./gradlew build`
- Test: `./gradlew test`
- Run: `./gradlew bootRun`
- Health (k8s probes): `GET /actuator/health`

Run `./gradlew build` after any significant change to confirm it compiles and tests pass.

## Project-specific rules

- REST endpoints are versioned under `/v1`.
- Do NOT add persistence dependencies (Spring Data JPA/JDBC, the Postgres driver,
  Flyway) or model any database concern until the persistence approach is decided in
  `ARCHITECTURE.md`. This is deliberate — persistence is an architectural decision, and
  JPA entities must not leak into upper layers.
- Do NOT invent the package/module layout; it is defined in `ARCHITECTURE.md`. Until
  then keep changes to the existing skeleton minimal.
- This repo knows only its own models. It shares no model libraries with other services
  (`mneme-ai`, `mneme-web`, `mneme-mobile`). Cross-service contracts are expressed as
  OpenAPI/spec; external models are mapped to ours at the edge (anti-corruption layer).
- Image generation is NOT part of this repo — it is a separate Phase 2 service
  (`mneme-ai`). In MVP an image is just an optional stored attribute (URL or upload).
- Interfaces at behavioural boundaries. Every use case is an interface in
  `application` with a single method, implemented by exactly one class. Controllers
  and other adapters depend on the interface, never the implementation. Ports
  (`CardRepository`, `LeitnerStrategy`) are interfaces declared by the inner layer.
  Mappers are interfaces too.
- No interfaces over data. Aggregates, value objects, JPA entities, DTOs, and
  `@ConfigurationProperties` types stay concrete. An interface with no behaviour and
  only one conceivable implementation adds noise, not abstraction.
- Implementation naming: when the implementation is characterised by its mechanism,
  the prefix names it (`JpaCardRepository`, `TokenAuthenticator`, `LeitnerScheduler`).
  When there is nothing to say beyond "the one implementation", prefix with `Default`
  (`DefaultCreateCardUseCase`, `DefaultCardMapper`).
- The domain never calls `Instant.now()`. Time enters as a parameter or via an
  injected `java.time.Clock`.
## Testing

- JUnit 5 (Spring Boot default). Per the global standards: unit-test the domain and use
  cases without a Spring context; integration-test adapters against real infrastructure
  (Testcontainers) once persistence exists.

## Out of scope (MVP)

See `PROJECT.md`. In short: no AI image pipeline, no sharing/subscriptions, no advanced
SRS. Do not build these.