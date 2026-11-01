## Purpose

Gives clients one error shape to handle, with a stable machine-readable code, and keeps
internal detail out of responses.

## ADDED Requirements

### Requirement: Single error shape

Error responses SHALL use `application/problem+json` with `status`, `title`, `detail` and a
`code` property. Clients branch on `code`, whose values are `CARD_NOT_FOUND`,
`REVIEW_NOT_FOUND`, `VALIDATION_FAILED`, `INTERNAL_ERROR`, `UNAUTHENTICATED` and
`METHOD_NOT_ALLOWED`.

#### Scenario: Handled error
- **WHEN** a request fails with a handled error
- **THEN** the content type is `application/problem+json`
- **AND** the body carries the matching `code`

### Requirement: Validation failures name their fields

When a request field fails validation, the response SHALL be `400` with code
`VALIDATION_FAILED` and an `errors` map from request field name to reason. A body that cannot
be parsed SHALL also be `400` with that code.

#### Scenario: Invalid field
- **WHEN** a request has a blank `word`
- **THEN** the response is `400` with code `VALIDATION_FAILED`
- **AND** `errors` names `word`

#### Scenario: Unparseable body
- **WHEN** the body is malformed or holds an unknown enum value
- **THEN** the response is `400` with code `VALIDATION_FAILED`

### Requirement: Not-found responses leak nothing

Not-found responses MUST NOT contain the requested id, the request path with that id, or any
owner information.

#### Scenario: Missing card
- **WHEN** a card is not found
- **THEN** the body contains no card id

### Requirement: Unexpected failures are opaque and logged

An unexpected failure SHALL produce `500` with code `INTERNAL_ERROR` and a fixed `detail`. The
response MUST NOT contain the exception message, class name or stack trace, and the failure
MUST be logged. Violated domain invariants SHALL surface this way rather than as client errors.

#### Scenario: Unexpected exception
- **WHEN** handling a request throws an unexpected exception
- **THEN** the response is `500` with code `INTERNAL_ERROR`
- **AND** the body contains neither the exception message nor its class name

### Requirement: Unsupported method

The system SHALL answer an unsupported HTTP method with `405` and code `METHOD_NOT_ALLOWED`.

#### Scenario: Wrong method on a route
- **WHEN** a caller sends `GET` to a `POST`-only route
- **THEN** the response is `405` with code `METHOD_NOT_ALLOWED`
