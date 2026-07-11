# CORS configuration — spec

## Goal

Allow the future `mneme-web` browser client to call the API cross-origin during
development, without weakening production. This is the one change the core needs so a
separate front-end can talk to it. No endpoint, DTO, or domain type changes.

Browsers block cross-origin requests (front-end on `http://localhost:5173`, API on
`http://localhost:8080`) unless the server explicitly opts in. Because the API uses an
`Authorization: Bearer` header, the browser also sends a **preflight** `OPTIONS` request
first; if the server does not allow that header and method on the preflight, the real
request never fires.

## In scope

### CORS via WebMvcConfigurer, origins from config

- A `WebMvcConfigurer` bean (in `infrastructure/web` or `infrastructure/config`) that
  registers CORS mappings. Do **not** use `@CrossOrigin` annotations on controllers —
  the policy lives in one place, driven by configuration.
- Allowed origins come from a `@ConfigurationProperties` type (e.g. `app.cors.allowed-origins`,
  a list), **not** hard-coded. The domain and controllers know nothing about CORS.
- Apply the mapping to the API paths (`/v1/**`). It need not cover `/actuator/**`.

### What the policy must allow

- **Origins:** from config. In the dev/default profile, `http://localhost:5173` (Vite's
  default). Production origins are supplied via configuration/environment when a real
  front-end domain exists — none are hard-coded for prod.
- **Methods:** `GET, POST, PUT, DELETE, OPTIONS`. `OPTIONS` must be allowed for
  preflight to succeed.
- **Headers:** the request must be allowed to carry `Authorization` and `Content-Type`.
  If `Authorization` is not in the allowed request headers, the browser blocks every
  authenticated call.
- Credentials (cookies) are **not** used — this API is stateless bearer-token auth. Do
  not enable `allowCredentials` unless a concrete need appears; combined with a wildcard
  origin it is also invalid, so leave it off.

### Profile behaviour

- Dev/default profile: `localhost:5173` allowed, so local front-end development works.
- Production profile: allowed origins default to **empty / none** unless explicitly
  configured. CORS must not silently allow all origins in production. Mirror how the
  OpenAPI docs endpoints are already gated by profile.

### The auth filter must not 401 the preflight

The CORS preflight `OPTIONS` request is sent by the browser **without** an
`Authorization` header. The existing `AuthenticationFilter` currently rejects any
request lacking a valid token — which would make it reject every preflight with `401`,
breaking CORS before the real request is attempted.

The filter must let `OPTIONS` preflight requests through (Spring's CORS handling
processes them). The cleanest fix consistent with the existing code: extend the
filter's `shouldNotFilter` (which already skips actuator and docs paths via the
`UNFILTERED_PREFIXES` constant) to also skip requests whose method is `OPTIONS`. Add
this to the same single place that already lists what the filter ignores.

## Out of scope

- No cookies, no session, no `allowCredentials`.
- No production origins hard-coded — production is configured at deploy time.
- No change to authentication itself, to any endpoint, DTO, use case, or domain type.
- No CORS on `/actuator/**`.
- Do not delete or rewrite files outside those this spec names.

## Acceptance criteria

1. `./gradlew clean build` is green.
2. With the dev profile, a preflight `OPTIONS` to `/v1/cards` from origin
   `http://localhost:5173` receives a successful CORS response (not `401`, not blocked),
   allowing methods `GET,POST,PUT,DELETE,OPTIONS` and the `Authorization` header.
3. A real cross-origin `GET /v1/session/cards` with a valid bearer token from
   `http://localhost:5173` succeeds and returns the CORS response headers.
4. A request from an origin **not** in the allowed list does not receive
   `Access-Control-Allow-Origin` for that origin.
5. With the production profile and no configured origins, no origin is allowed by
   default.
6. Allowed origins are read from configuration, not hard-coded in a `WebMvcConfigurer`
   literal.
7. `AuthenticationFilter` no longer returns `401` for `OPTIONS` preflight requests.

## Tests

- `@WebMvcTest` (or a slice test): a preflight `OPTIONS` to `/v1/cards` with
  `Origin: http://localhost:5173` and `Access-Control-Request-Method: POST` returns a
  2xx/allowed CORS response with the expected allow-methods and allow-headers; the same
  from a disallowed origin does not echo that origin.
- A test confirming the auth filter does not reject an `OPTIONS` request lacking a token.
- Configuration binding test: allowed origins populate from properties.

## Notes

- This is the only change the API surface needs to support a browser client; everything
  else the front-end requires it gets from the OpenAPI contract.
- Keep the CORS properties documented in `application.yaml` so it is obvious where to add
  a production origin later.
- Branch `chore/cors`; reviewer focus is "does dev work and does prod stay closed".