# Mneme — Product overview & MVP scope

**Mneme** is a vocabulary-learning app built around personal mnemonic associations.
The base unit is a card: word + translation + association, written by the user; the
image is optional. This document is the product-level scope; the MVP is implemented in
the **mneme-core** service.

Language note: the **Word** is in the target language (English); the **Translation**
and **Association** are in the learner's native language (Russian).

Concept example: `surround` → translation "окружать" (to surround) → association
*"сир, нас окружают"* (knights shouting СИР / "sire" + раунд / "round" = a circle).
The human invents the association — that is the whole point of the product.

---

## 1. Overview and goal

- **What:** a tool for memorizing words through associations, with an individual
  repetition process.
- **Audience:** a small number of users in a trusted circle (a family).
- **Dual goal:**
    1. a working product for daily use by the author and family;
    2. a playground to sharpen architecture (Clean Architecture, ports & adapters,
       distributed-systems thinking, deploy/k8s).

---

## 2. How to use this document (instructions for the AI agent)

- This file is the **single source of truth** for scope and product decisions.
- Code-writing principles live in the **engineering standards** (global `AGENTS.md`)
  and stack/conventions in the project `AGENTS.md`. They are not duplicated here.
- Workflow: **PROJECT.md → ARCHITECTURE.md (separate step) → break into task specs
  (docs/specs) → implement per task.**
- Rules:
    - Do not implement anything from the **"Out of MVP scope"** section.
    - Do not reintroduce rejected alternatives from section 11 "on your own initiative".
    - When something is ambiguous — **ask, don't assume**.
    - Treat decisions as fixed until explicitly revised in this file.

---

## 3. Glossary (ubiquitous language)

| Term | Meaning |
|---|---|
| **User** | A user. Owns their own cards and their own progress. |
| **Card** | The unit of study. Complete once Word + Translation + Association exist. |
| **Word** | The foreign word being learned (target language, e.g. English). |
| **Translation** | The word's translation (learner's native language). |
| **Association** | A mnemonic association. Free text in the learner's native language. **Always written by the human.** |
| **Image** | Optional card image. In MVP — manual only (upload/URL). |
| **Visibility** | Card visibility: `Private` (default) or `Public`. |
| **ReviewState** | A card's repetition state for a user. |
| **Leitner Box** | The box number in the Leitner system. Determines the review interval. |
| **Review Session** | A repetition session: cards whose due date has arrived. |
| **Grade** | Answer grade. **Binary:** `Correct` / `Incorrect`. |

*(Phase 2 will add: `ImagePrompt`, `PromptRefinement`, `GeneratedImage`, `AI Service`.
**These terms are not used in MVP code — do not introduce classes, fields, or tables for them.**)*

---

## 4. Domain concept

- A card **is already a finished product** the moment Word, Translation, and
  Association are filled in. The image is optional enrichment.
- **The association is always written by the human.** The goal is to build a personal
  neural connection in a specific user's head. The system **never** generates learning
  content.
- **The learning path is individual:** each user has their own cards and their own
  progress. There is no shared (group) study.
- A card belongs to exactly one owner (**siloed**).

---

## 5. MVP scope (mneme-core)

- **User identity** and simple authentication.
- **Card CRUD:** create, read, update, delete.
  Fields: Word, Translation, Association, optional manually attached Image
  (upload or URL), Visibility flag.
- **Leitner-based repetition:** a card has a Box and a next-due date; the Review
  Session shows cards whose date has arrived; binary Grade;
  `Correct` → promotion to the next box, `Incorrect` (in any box) → reset to the
  first. **In the last box, `Correct` keeps the card there** and it keeps being
  reviewed at the maximum interval — there is no separate "mastered" state in MVP.
- **Editing any card field does not touch ReviewState.** Resetting progress is a
  separate explicit action, and there is none in MVP.
- **Progress is per-user**, progress is not shared.
- **The Visibility flag is stored** (default `Private`), but **no logic based on it
  is implemented in MVP**: no sharing endpoints, no filtering queries by visibility.
  The field is groundwork for Phase 2.

---

## 6. Out of MVP scope (do NOT build)

- The AI image generation pipeline (prompt enrichment, confirmation, Kandinsky).
- The separate AI service (`mneme-ai`).
- Sharing, subscriptions, copying other users' public cards.
- Advanced SRS algorithms (SM-2, FSRS).
- A separate "dictionary / word queue for later" entity.
- Anything not explicitly listed in section 5.

---

## 7. Phase 2 (direction of development, do not implement now)

- **`mneme-ai` — AI Service as a separate bounded context (own repo).** Flow: card
  Association → enrichment into an expanded image prompt (**GigaChat**) → returned to
  the user for editing/confirmation → on confirmation, generation (**Kandinsky**) →
  the image is attached to the card **asynchronously**, the user is notified on
  completion (notification method — TBD, see §12). The card gains an image-generation
  state machine.
- **Phase 2 external services:** GigaChat — image-prompt enrichment; Kandinsky —
  image generation.
- **Clients:** `mneme-web` (web), `mneme-mobile` (mobile). Each owns its own DTOs
  generated from the `mneme-core` contract.
- **Sharing:** public cards are visible to others; a liked one can be copied into
  your own set (becomes your own card — everyone builds their own connection).
- **Advanced SRS** (SM-2 / FSRS) behind the same scheduler abstraction.

---

## 8. Non-functional requirements

- An app for a small trusted circle — **throughput is not a goal.**
  Do not optimize for high RPS.
- **Synchronous-path latency:** opening and going through cards must feel instant
  (target p95 ~100–200 ms). This dictates queries and indexes, not horizontal
  scaling.
- **Durability:** the card collection must not be lost. Backups are a real
  requirement.
- **Core independence:** the domain must work fully without any AI integration
  (image generation is optional and out of MVP).
- **Async readiness (for Phase 2):** AI work is asynchronous, best-effort,
  idempotent (safe to retry), with timeouts and a cost ceiling on external APIs.
  Card creation is never blocked by image generation.
- **Availability:** "works when a family member sits down to study." No HA or
  multi-region needed.

---

## 9. Technical constraints

- **Language / framework:** Java 21, Spring Boot 4.1.
- **Build:** Gradle (Kotlin DSL).
- **DB:** PostgreSQL.
- **Mandatory style:** Clean Architecture with explicit ports & adapters; the domain
  is independent of frameworks and vendors. Detailed design is produced in
  `ARCHITECTURE.md`, not here.

---

## 10. Architectural invariants (mandatory; detailed design is a separate step)

- Domain and application layers do not depend outward. Frameworks (Spring, JPA, HTTP)
  and external services are details at the edges.
- External systems sit behind **outbound ports** (interfaces declared by the inner
  layer); adapters implement them. In MVP there is one such system — Postgres.
- In MVP, `Image` is an **optional stored attribute of the card** (URL or uploaded
  file), set directly by the user. **There is no image-generation port in MVP.**
  Phase 2 introduces an outbound `ImageGenerator` port whose adapter calls `mneme-ai`;
  the core does not depend on it.
- The review-scheduling algorithm is a **domain Strategy** behind an interface,
  **not a port** (no external system behind it). The interface is designed
  **strictly for MVP needs (Leitner)**; generalizations for future algorithms only
  if needed now. YAGNI over speculative flexibility.
- Multi-user: `UserId` is part of the domain from the start and arrives in the
  application layer as an **explicit use-case parameter** (not extracted from
  `SecurityContext` inside the domain/use case). Extraction from the authn mechanism
  happens at the edge, in the REST adapter; the authentication mechanism itself is a
  replaceable detail.

---

## 11. Decisions made and rejected alternatives

**Siloed cards (each user has their own)**
- Why: an association is a personal neural connection; only the objective
  Word+Translation could be shared, which contradicts the premise and is not worth
  splitting the aggregate.
- Rejected: a shared common set of cards.

**Leitner for MVP**
- Why: simple, clean domain; the algorithm is behind an abstraction, so upgrading
  later is cheap.
- Rejected: SM-2 / FSRS at the start.

**Associations written by the human**
- Why: AI-generated associations kill the learning purpose; the quality of
  cross-lingual phonetic mnemonics from current LLMs is unreliable.
- Rejected: auto-generation of learning content.

**Image generation — optional and in Phase 2**
- Why: the biggest complexity multiplier (two external services, async pipeline,
  state machine) and not part of the learning loop.
- Rejected: image generation in MVP.

**AI work — a separate bounded context (`mneme-ai`)**
- Why: isolates slow and unreliable external calls from the fast synchronous core.
- Rejected: embedding AI logic into the core.

**Multi-repo, no shared model libraries**
- Why: hard service boundaries; enables learning deploy/k8s and polyglot services;
  contracts via OpenAPI/spec, each service owns its DTOs.
- Rejected: monorepo; shared domain-model library across services.

**Simple authentication for MVP**
- Why: a trusted family circle; identity is modeled now, authentication is hardened
  later.
- Rejected: full authentication (OAuth etc.) at the start.

---

## 12. Open questions for the architecture step

- **Leitner parameters:** number of boxes and per-box intervals (configurable,
  defaults TBD).
- **Persistence:** DB schema, aggregate boundaries, persistence library (JPA vs JDBC
  vs jOOQ), mapping (no JPA leakage into upper layers — per the code standards).
- **API surface:** shape of REST contracts (versioned under `/v1`), DTOs vs domain
  models at the edge.
- **Authentication mechanism:** Basic vs a static per-user token; user provisioning
  (seeded vs self-registration).
- **AI Service deployment (Phase 2):** separate process vs in-process async worker;
  transport (REST/messaging).
- **Image-ready notification method (Phase 2):** polling / SSE / WebSocket / other —
  TBD. Does not affect MVP architecture.
- **Backup/restore** for the durability requirement.