## Purpose

Publishes a machine-readable contract of the REST API for client teams, and a browser UI for
trying it, outside production.

## ADDED Requirements

### Requirement: Published contract

Outside the `prod` profile the system SHALL serve an OpenAPI document at `/v3/api-docs` and
Swagger UI. The document SHALL list every `/v1` endpoint and declare an HTTP bearer security
scheme, with validation constraints shown on request bodies.

#### Scenario: Contract served
- **WHEN** `GET /v3/api-docs` is requested outside `prod`
- **THEN** the response is `200` with the OpenAPI document

#### Scenario: Swagger UI served
- **WHEN** `GET /swagger-ui/index.html` is requested outside `prod`
- **THEN** the response is `200`

### Requirement: No documentation in production

The `prod` profile MUST disable the OpenAPI document and Swagger UI.

#### Scenario: Production profile
- **WHEN** the service runs with the `prod` profile
- **THEN** the OpenAPI document and Swagger UI are not served
