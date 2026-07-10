# Review session — spec

## Goal

Serve the cards that are due for review. This closes the study loop: a card can be
created, shown when its time comes, answered, and rescheduled.

This is the **first read that crosses both aggregates**: it filters by `Review.dueAt`
and returns `Card` content. It is assembled in the use case from the two repositories —
not by a database join, not by a read model. That decision, and the door left open to
revisit it, are recorded in ARCHITECTURE §4.

## In scope

### Endpoint

- `GET /v1/session/cards` → `200` with the cards whose `dueAt <= now`, for the
  authenticated user, ordered by `dueAt` ascending (most overdue first).
- `200 []` when nothing is due. An empty session is a valid state, not an error.
- No pagination, no limit. Family scale; throughput is explicitly not a goal
  (PROJECT.md §8). Adding a limit later is a decision to make when a real need appears
  (ARCHITECTURE §12).
- `401` without a valid bearer token (existing filter).

`session/` is a namespace reserved for future session-scoped reads. It is a
**read-only scenario resource**: it must never gain a write endpoint. Grading stays on
`POST /v1/reviews/{cardId}`, because grading writes a `Review`.

### Ports

- `ReviewRepository.findDue(UserId owner, Instant now)` → `List<Review>`, ordered by
  `dueAt` ascending. This is the query the mandatory `(owner_id, due_at)` index exists
  for (ARCHITECTURE §6, §11).
- `CardRepository.findAllByIdsAndOwner(Collection<CardId> ids, UserId owner)` →
  `List<Card>`. Ownership stays in the port contract even though the ids already come
  from owner-scoped reviews — every port expresses siloing the same way.

### Application

- `ReviewSessionView` — a record in `application`, one per due card:
  `(CardId cardId, Word word, Translation translation, Association association,
  Optional<Image> image, LeitnerBox box, Instant dueAt)`.

  It carries domain value objects, not primitives — mapping to wire types happens at
  the web edge, as everywhere else. It is neither an aggregate (no identity, never
  persisted) nor a DTO (not a JSON shape). It exists so the use case never returns raw
  aggregates, which is what makes a future move to a read model a change inside one
  method rather than a change to the whole stack.

  `visibility` is deliberately absent: nothing in the MVP keys off it.

- `GetReviewSessionUseCase` (interface, one method) → `DefaultGetReviewSessionUseCase`.
  `List<ReviewSessionView> get(UserId owner)`.

  Body: `reviewRepository.findDue(owner, clock.instant())`; if empty, return an empty
  list **without** calling `CardRepository` at all; otherwise collect the card ids,
  `cardRepository.findAllByIdsAndOwner(ids, owner)`, and zip the two lists into views
  **preserving the `dueAt` order of the reviews** — the card query's return order is
  not to be relied on.

  No `@Transactional` (two reads, no writes).

- If a review references a card that does not come back from the card repository, that
  is a broken invariant (they are created together in one transaction). Do **not**
  silently skip it and do not translate it to a client error — let it surface. Throw
  an unchecked exception; the catch-all handler turns it into `500`. A silently short
  session would hide data corruption.

### Web

- `SessionApi` (interface) → `SessionController`, `@RequestMapping("/v1/session")`,
  `GET /cards`, taking `@AuthenticatedUser UserId`.
- `SessionCardDto` — a record: `(UUID cardId, String word, String translation,
  String association, String imageUrl, int box, Instant dueAt)`. `imageUrl` is `null`
  when the card has no image. Domain types never appear in the DTO.

  The response contains the translation and the association up front. The client decides
  what to reveal and when — blurring, hints, and reveal order are UI concerns. The core
  serves data, not screens; per-screen shaping belongs in a BFF, outside this service
  (ARCHITECTURE §7).

## Out of scope

- No pagination, limit, or cursor.
- No "session state" on the server: no started/finished session, no progress counter,
  no skipping. The client walks the returned list.
- No write endpoints under `session/`.
- No hint levels, no partial reveal, no non-binary grades. `Grade` stays
  `CORRECT` / `INCORRECT` (PROJECT.md §3, §11).
- No card list/update/delete — still a later slice.
- No changes to `Card`, `Review`, `LeitnerStrategy`, or any table. No migration.
- Do not delete or rewrite files outside those this spec names.

## Acceptance criteria

1. `./gradlew clean build` is green.
2. A freshly created card is **not** in the session: its `dueAt` is `now + interval(1)`,
   which is in the future.
3. A card whose review is due (`dueAt <= now`) appears in the session, with its word,
   translation, association, image, box and dueAt.
4. Cards are ordered by `dueAt` ascending.
5. Another user's due cards never appear.
6. Nothing due → `200` with an empty JSON array, and `CardRepository` is not called.
7. Grading a due card `CORRECT` removes it from the session (its new `dueAt` moves into
   the future); grading `INCORRECT` also removes it (box 1's interval is still in the
   future). Verified end to end.
8. The domain source set still contains no `jakarta.persistence`, Spring, or web imports.

## Tests

- Use case, mocked ports and a fixed `Clock`: due reviews are zipped with their cards in
  `dueAt` order even when the card repository returns them in a different order; empty
  due list short-circuits and `CardRepository` receives zero interactions; a review whose
  card is missing raises rather than being skipped.
- Persistence integration (Testcontainers): `findDue` returns only reviews with
  `dueAt <= now`, only for the given owner, ordered ascending; `findAllByIdsAndOwner`
  excludes another owner's cards.
- Web (`@WebMvcTest`, use case mocked): the view maps to the DTO; empty list serialises
  as `[]`; `401` without a token; a card without an image serialises `imageUrl` as null.
- Endpoint integration (Testcontainers): create a card, confirm the session is empty;
  move the review's `dueAt` into the past (adjust the injected `Clock` or write the
  review directly through the port); confirm the card appears; grade it; confirm it
  disappears. This is acceptance criteria 2, 3 and 7 proven together.

## Notes

- Time comes from the injected `Clock`. The integration test needs to control "now" —
  prefer supplying a fixed `Clock` bean in the test context over sleeping or writing
  timestamps by hand, but writing a past `dueAt` through `ReviewRepository.save` is an
  acceptable alternative if it keeps the test simpler.
- Follow the interface and naming rules in the project `AGENTS.md`.
- Suggested order: ports → application (+ tests) → web (+ tests) → integration test.