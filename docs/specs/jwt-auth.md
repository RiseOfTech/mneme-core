# JWT authentication — spec

## Goal

Replace the static seeded bearer token with real JWT validation. After this slice,
`mneme-core` accepts access tokens issued by `mneme-auth`, verifies them locally against
a public key, and resolves the token's `sub` to the `UserId` that use cases already
receive as a parameter.

The `Authenticator` seam was designed for exactly this from the walking skeleton
onwards. Nothing above the port changes: no use case, no domain type, no controller
signature.

**The core never calls `mneme-auth`** — not per request, not at startup. Validation is
local and offline. This is the point of the design: the core stays up when auth is down.

## Token contract (normative — do not deviate)

`mneme-auth` issues a compact JWS:

```
header:  { "kid": "<key id>", "typ": "JWT", "alg": "RS256" }
payload: { "iss": "mneme-auth", "sub": "<user uuid>", "aud": "mneme-core",
           "iat": <epoch>, "exp": <iat + 15 min> }
```

The core must validate, and reject with `401` on any failure:

1. **Signature** — against the RSA public key supplied by configuration.
2. **`exp`** — expired tokens rejected. Clock-skew leeway of at most 60 seconds is
   acceptable.
3. **`iss` == `mneme-auth`** — literal match.
4. **`aud` == `mneme-core`** — literal match.

`sub` is the `UserId` (a UUID string). There is **no** `jti`, no roles, no scopes, no
email. Claims are minimal by design — do not expect or require others.

`kid` is present in every token. With a single key in the MVP the core may ignore it
when selecting a key, but must **not reject** a token for carrying one — it is the seam
for future rotation.

Failures are **not** distinguished to the client: expired, bad signature, wrong issuer
and wrong audience all produce the same opaque `401`.

## Key distribution

The RSA **public** key arrives as configuration — a PEM from the environment or a
mounted file, the same way other secrets already reach the core. There is **no JWKS
endpoint** and no key fetch: introducing one would recreate the runtime dependency on
auth that this design removes.

The private key never exists in this service. The core is verify-only by construction.

## In scope

### Security configuration replaces the hand-rolled filter

Add `spring-boot-starter-oauth2-resource-server` and configure a `SecurityFilterChain`:
stateless session policy, CSRF disabled (stateless bearer API), `oauth2ResourceServer`
with a `JwtDecoder` built from the public key plus the `iss`/`aud`/`exp` validators
above.

**Deleting the existing `AuthenticationFilter` removes behaviour that must be
preserved.** Everything it did has to move into the security chain — this is the main
risk in this slice:

- **Public paths.** The filter's `UNFILTERED_PREFIXES` (`/actuator`, `/v3/api-docs`,
  `/swagger-ui`) must become `permitAll` rules. If they are missed, the health probe and
  Swagger UI start returning `401`.
- **The ERROR dispatch must be permitted.** When an unhandled MVC exception occurs
  (wrong `Content-Type`, malformed body, wrong method), Boot forwards internally to
  `/error` as a **second dispatch**, which Spring Security re-authorizes against the path
  `/error` — matching no `permitAll` rule, so the client receives a bare `401` instead of
  the correct `400`/`405`/`415`. Permit the ERROR dispatch type ahead of the path rules.
  This is invisible to `@WebMvcTest` slices; it must be tested over real HTTP.
- **CORS.** CORS is currently handled by a `WebMvcConfigurer`. Spring Security processes
  CORS inside its own filter chain, so it must be explicitly enabled there or
  cross-origin requests from the front-end will start failing. Preflight `OPTIONS`
  requests must continue to succeed without a token.
- Everything under `/v1` requires authentication.

### The Authenticator port

Reimplement the `Authenticator` port to resolve a validated JWT's `sub` to a `UserId`.
Delete the static-token implementation, its `@ConfigurationProperties` type, and the
seeded `app.auth.tokens` configuration — including from `application.yaml` and any test
configuration.

`UserId` continues to be extracted **at the edge** and passed into use cases as an
explicit parameter, via the existing `@AuthenticatedUser` argument resolver. Neither the
domain nor the application layer reads a `SecurityContext`. This rule does not change.

### Error shape for authentication failures

Spring Security rejects invalid tokens **in the filter chain, before any controller
advice runs**, so a `401` will not pass through `ApiExceptionHandler` and would emerge
as a bare response with a `WWW-Authenticate` header — breaking the single error contract
established earlier (`application/problem+json` with a `code` property).

Add a custom `AuthenticationEntryPoint` that emits the core's existing `ProblemDetail`
shape with a new `ErrorCode` (e.g. `UNAUTHENTICATED`), so every error from this service
still looks the same to clients. Do not leak the reason for the failure into the body.

### Existing data: fresh start

Seeded users are gone, and every existing `cards.owner_id` / `reviews.owner_id`
references a seeded UUID that no real account will ever have. **The existing test data is
disposable and is being discarded** (decided by the human).

Add a migration that clears the existing rows (reviews first, then cards — the foreign
key forbids the reverse). Do **not** attempt to re-point ownership, and do not seed users
into this service: identity is owned by `mneme-auth`.

## Out of scope

- No refresh-token handling. Refresh tokens live strictly between the browser and
  `mneme-auth`. The core never sees, stores, or rotates one. A `401` from the core is the
  client's cue to refresh and retry.
- No cookie reading — the core reads the `Authorization` header only.
- No call to `mneme-auth`: no introspection, no JWKS fetch, no user lookup.
- No user table, registration, password handling, or profile in this service.
- No roles, scopes, or permission checks — one kind of user, no authority claims.
- No changes to any use case, domain type, aggregate, repository, or endpoint path.
- No key rotation mechanism beyond honouring `kid`'s presence.
- Do not delete or rewrite files outside those this spec names.

## Acceptance criteria

1. `./gradlew clean build` is green.
2. A token signed with the real private key, with valid `iss`/`aud`/`exp`, returns `200`
   from `GET /v1/me`, and the returned `userId` equals the token's `sub`.
3. Each of these returns `401`: expired token; wrong `iss`; wrong `aud`; token signed by
   a different key; malformed token; no `Authorization` header.
4. All `401` responses are `application/problem+json` carrying the error `code`, and
   none of them reveals why validation failed.
5. `GET /actuator/health` still returns `200` without a token.
6. The OpenAPI docs endpoints (`/v3/api-docs`, `/swagger-ui`) are still reachable without
   a token under the dev profile.
7. A deliberately malformed request to an authenticated endpoint (e.g. `GET` on a
   `POST`-only route, or a bad `Content-Type`) returns its proper `4xx` — **not** `401`.
   This is the ERROR-dispatch trap; it must be exercised over real HTTP with a full
   application context, not a `@WebMvcTest` slice.
8. A CORS preflight `OPTIONS` from the allowed dev origin still succeeds without a token,
   and a real cross-origin request with a valid token still carries CORS headers.
9. No reference to the old static token remains: `grep` finds no `app.auth.tokens`, no
   seeded-token properties class, and no static-token `Authenticator` implementation.
10. The domain source set still contains no framework imports.

## Tests

- Integration tests over real HTTP with a full context (Testcontainers), signing tokens
  in the test with a test key pair: the happy path and every `401` case in criterion 3.
- A test for criterion 7 (malformed request → proper `4xx`, not `401`) — this is the one
  the `@WebMvcTest` style cannot catch.
- Tests for criteria 5, 6, 8 — public paths and CORS survive the migration to Spring
  Security.
- Existing tests will break wherever they used the seeded static token; update them to
  mint a signed test token instead. That mechanism (a test key pair plus a helper that
  issues tokens) is shared infrastructure — put it somewhere reusable.

## Notes

- Do not hand-roll JWT parsing or signature verification. Use the resource-server
  support; the crypto is not ours to write.
- `mneme-auth` already runs as a resource server for its own `/v1/profile` — its
  configuration is a working reference for the same shape.
- Once merged, record in `ARCHITECTURE.md` §8 that authentication is now JWT validated
  locally against a configured public key, that identity is owned by `mneme-auth`, and
  that the static-token mechanism is gone.