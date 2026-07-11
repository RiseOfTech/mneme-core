# OpenAPI documentation (springdoc) — spec

## Goal

Generate a machine-readable OpenAPI 3.1 contract for the mneme-core REST API from the
existing controllers, and serve an interactive Swagger UI. This gives three things at
once: live API documentation, a contract to hand to the future `mneme-web` client, and
a browser UI to exercise the endpoints by hand.

No behaviour of the API changes. No endpoint is added, removed, or altered. This slice
only documents what already exists.

## Compatibility (do not guess the version)

This project is on **Spring Boot 4.1.0**. springdoc's 2.x line does **not** work on
Spring Boot 4 (it fails at context startup). Use the **3.x** line. The official
compatibility matrix pairs Spring Boot 4.0.x with springdoc 3.0.1; 4.1.x needs the
latest 3.x release. Add the dependency **without pinning a version by hand if the Spring
Boot BOM manages it**; otherwise use the newest stable `3.x`. If the app fails to start
with a springdoc-related bean error, that is a version mismatch — resolve it by moving
to the correct 3.x release, not by downgrading Spring Boot.

Dependency (Gradle):
`implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui")`

Verify the exact coordinate and version against the current springdoc 3.x release at
build time. Add no other springdoc modules (no security, no hateoas, no data-rest — we
use none of those).

## In scope

### Dependency and baseline

- Add the `springdoc-openapi-starter-webmvc-ui` starter.
- With it present, springdoc auto-generates the spec from the existing `@RestController`
  classes and their `*Api` interfaces with zero further configuration. Confirm the app
  still starts and `./gradlew clean build` stays green.

### Endpoints and where they are exposed

- The generated JSON spec and the Swagger UI must be reachable **in local/dev runs**.
  Note that in springdoc 3.x these endpoints are **disabled by default** and must be
  explicitly enabled.
- Enable them **only outside production**. Add to `application.yaml` the properties that
  turn on `springdoc.api-docs` and `springdoc.swagger-ui`, but structure config so that
  a production profile can turn them off. The simplest correct approach: enable them in
  the default/dev config, and add a `prod` profile block (or documented env override)
  that sets `springdoc.api-docs.enabled=false` and `springdoc.swagger-ui.enabled=false`.
  Exposing the full API surface publicly in production is a decision we are not making
  now — default to off in prod.

### The auth scheme must appear in the spec

Every real endpoint except `/actuator/health` requires `Authorization: Bearer <token>`.
The generated contract must express this, or the `mneme-web` client generated from it
will not know to send the header.

- Declare a global security scheme of type HTTP `bearer` via a single `OpenAPI` bean (a
  `@Bean OpenAPI` in `infrastructure/config`, or wherever configuration beans live),
  carrying the API title, version, and a short description.
- Apply it so the spec marks the authenticated endpoints as requiring the bearer scheme.
  `/actuator/health` should not require it.

### Documenting the endpoints

- Base documentation is inferred automatically from method signatures, `*Api`
  interfaces, path mappings, DTO record components, and Jakarta validation annotations
  (`@NotBlank`, `@NotNull`) already on the request DTOs — these become schema
  constraints for free. Do not duplicate them.
- Add **lightweight** annotations only where the inferred output is unclear, placing them
  on the `*Api` interfaces (`CardApi`, `ReviewApi`, `SessionApi`, `MeApi`) rather than on
  the controllers — the interfaces already declare the REST contract, so the
  documentation belongs with them:
    - a one-line `@Operation(summary = ...)` per endpoint;
    - `@ApiResponse` entries for the non-obvious status codes each endpoint returns
      (e.g. `201`, `204`, `404`, `400`), so the contract lists them;
    - reference the `ProblemDetail` shape for error responses where it is not inferred.
- Use annotations from `io.swagger.v3.oas.annotations` (Swagger 3), which ship with the
  starter. Keep it minimal — the point is a usable contract, not exhaustive prose.

### Export the contract as a file

The `mneme-web` project will need the spec as a file. Document (in this slice's PR
description or a short `docs/` note) how to obtain it: run the app locally and fetch the
generated JSON from the api-docs endpoint, saving it as `docs/openapi.json`. Committing a
generated snapshot is optional and not required to pass — the live endpoint is the
source of truth. Do **not** wire up a build-time generation plugin in this slice; that is
extra machinery we do not need yet.

## Out of scope

- No build-time OpenAPI generation plugin.
- No Scalar UI (Swagger UI is enough).
- No change to any endpoint, DTO, use case, or domain type.
- No springdoc security/hateoas/data-rest modules.
- No exhaustive per-field annotation pass — only summaries and non-obvious responses.
- No decision to expose docs publicly in production — default them off there.
- No multi-module split. This stays in the single module.

## Acceptance criteria

1. `./gradlew clean build` is green and the application starts.
2. Running locally, the OpenAPI JSON is served (the api-docs endpoint) and Swagger UI
   loads in a browser.
3. The generated spec lists all existing endpoints: `/v1/cards` (GET, POST),
   `/v1/cards/{id}` (GET, PUT, DELETE), `/v1/reviews/{cardId}` (POST),
   `/v1/session/cards` (GET), `/v1/me` (GET).
4. The spec declares an HTTP bearer security scheme, and the authenticated endpoints are
   marked as requiring it; `/actuator/health` is not.
5. Request bodies show their validation constraints (e.g. `word` is required/non-blank),
   inferred from the existing `@NotBlank`/`@NotNull` annotations.
6. With the production profile active, the api-docs and swagger-ui endpoints are
   disabled.
7. No existing test breaks; the change is additive.

## Notes

- Swagger UI at its default path (`/swagger-ui.html`) is fine; no need to relocate it.
- If enabling the endpoints via properties, confirm the property names against the
  springdoc 3.x documentation — some renamed between 2.x and 3.x.
- This is infrastructure/documentation, not a feature: branch `chore/openapi`, and the
  reviewer focus is "does the contract accurately describe the real API", not domain
  correctness.