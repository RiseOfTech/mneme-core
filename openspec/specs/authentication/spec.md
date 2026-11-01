# authentication Specification

## Purpose

Identifies the caller from an access token issued by `mneme-auth`. Tokens are verified locally
and offline, so the service keeps working while `mneme-auth` is down.

## Requirements

### Requirement: Access token validation

The system SHALL require a signed token in the `Authorization: Bearer` header for every
`/v1` request. It SHALL verify the RS256 signature against the configured RSA public key,
require an unexpired `exp` with at most 60 seconds of clock skew, and require `iss` to equal
`mneme-auth` and `aud` to equal `mneme-core`. It MUST NOT contact `mneme-auth` to do so. A
`kid` header MUST NOT cause rejection.

#### Scenario: Valid token
- **WHEN** a request carries a valid token
- **THEN** it is served as the user named by the token's `sub`

#### Scenario: Invalid token
- **WHEN** the token is expired, malformed, signed by another key, or has the wrong issuer or audience
- **THEN** the response is `401` with code `UNAUTHENTICATED`

#### Scenario: No token
- **WHEN** a `/v1` request has no `Authorization` header
- **THEN** the response is `401` with code `UNAUTHENTICATED`

#### Scenario: Failure reason stays hidden
- **WHEN** authentication fails for any reason
- **THEN** the response does not reveal the reason

### Requirement: Caller identity endpoint

The system SHALL return the authenticated caller's user id at `GET /v1/me`.

#### Scenario: Authenticated caller
- **WHEN** a caller with a valid token requests `GET /v1/me`
- **THEN** the response is `200` with `userId` equal to the token's `sub`

### Requirement: Public paths

The system SHALL serve the health probe without a token, as well as the OpenAPI document and
Swagger UI assets where those are enabled.

#### Scenario: Health probe
- **WHEN** `GET /actuator/health` is requested without a token
- **THEN** the response is `200`

#### Scenario: API documentation
- **WHEN** `GET /v3/api-docs` or `GET /swagger-ui/index.html` is requested without a token
- **THEN** the response is `200`

### Requirement: Request errors are not masked by authentication

The system SHALL report a request that fails after authentication with its own status, not
with `401`.

#### Scenario: Wrong method
- **WHEN** an authenticated caller uses a method the route does not support
- **THEN** the response is `405`, not `401`
