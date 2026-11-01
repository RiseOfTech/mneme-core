# cross-origin-access Specification

## Purpose

Lets a browser client on another origin call the API, without opening the production service
to every origin.

## Requirements

### Requirement: Configured origins only

The system SHALL allow cross-origin access to `/v1/**` only from configured origins. The
default configuration allows `http://localhost:5173`; the `prod` profile MUST allow no origin
unless one is configured. Allowed methods SHALL be `GET`, `POST`, `PUT`, `DELETE` and
`OPTIONS`, and allowed request headers SHALL be `Authorization` and `Content-Type`. Credentials
MUST NOT be enabled.

#### Scenario: Preflight from an allowed origin
- **WHEN** an allowed origin sends a preflight `OPTIONS` without a token
- **THEN** it succeeds with the allowed origin, methods and headers

#### Scenario: Disallowed origin
- **WHEN** an origin that is not configured sends a preflight
- **THEN** the response does not allow that origin

#### Scenario: Authenticated cross-origin request
- **WHEN** an allowed origin sends a real request with a valid token
- **THEN** it succeeds and carries the CORS response headers

#### Scenario: No origins configured
- **WHEN** no origins are configured
- **THEN** no origin is allowed
