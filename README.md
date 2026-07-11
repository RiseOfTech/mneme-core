# mneme-core

Backend service of **Mneme** — a vocabulary-learning app built around personal
mnemonic associations. Owns the cards domain, Leitner spaced repetition, and the
public REST API.

- Product scope: [`PROJECT.md`](PROJECT.md)
- Architecture: [`ARCHITECTURE.md`](ARCHITECTURE.md)
- Engineering standards & conventions: [`AGENTS.md`](AGENTS.md)
- Feature specs: [`docs/specs/`](docs/specs/)

## Stack

Java 21 · Spring Boot 4.1 · PostgreSQL · Gradle (Kotlin DSL) · Flyway · Testcontainers

## Prerequisites

- JDK 21
- Docker via **Colima** (the test suite uses Testcontainers, which needs a running
  Docker daemon)

## Local Docker setup (Colima)

Testcontainers looks for the Docker socket at the default path, but Colima puts it
under your home directory. Without these environment variables, `./gradlew build`
fails with `Could not find a valid Docker environment`
(`DockerClientProviderStrategy`).

Add to your shell profile (`~/.zshrc`) once:

    export DOCKER_HOST="unix://$HOME/.colima/default/docker.sock"
    export TESTCONTAINERS_DOCKER_SOCKET_OVERRIDE=/var/run/docker.sock

Then `source ~/.zshrc`. Start Colima before running the build (it does not start
automatically after a reboot):

    colima start

## Build & test

    ./gradlew clean build

Runs unit tests and Testcontainers-backed integration tests (each spins up a
throwaway PostgreSQL container).

## Run locally

Start a PostgreSQL matching the defaults in `application.yaml`:

    docker run --name mneme-pg \
      -e POSTGRES_DB=mneme -e POSTGRES_USER=mneme -e POSTGRES_PASSWORD=mneme \
      -p 5432:5432 -d postgres:16-alpine

Run the app (the datasource password comes from the environment, not the repo):

    DB_PASSWORD=mneme ./gradlew bootRun

Health probe: `GET /actuator/health`

## API documentation

With the app running (dev profile), the OpenAPI contract and interactive UI are served:

- OpenAPI JSON: `http://localhost:8080/v3/api-docs`
- Swagger UI: `http://localhost:8080/swagger-ui.html`

Both are disabled under the `prod` profile.

## Authentication

Every endpoint except `/actuator/health` requires a static per-user bearer token:

    Authorization: Bearer <token>

Tokens are seeded via configuration (`app.auth.tokens`), supplied from the environment
in real deployments — never committed.

## Docker image

    docker build -t mneme-core:local .

Multi-stage, layered build (dependencies cached separately from application code).
CI (GitHub Actions) builds and publishes the image to `ghcr.io` on merge to
`develop`/`master`.