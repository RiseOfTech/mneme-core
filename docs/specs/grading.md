# Grading — spec

## Goal

Record a review answer and let the domain move the card through the Leitner boxes. This
activates `LeitnerStrategy.next`, which has been implemented but uncalled since the Card
slice, and closes half of the study loop: a card can now be created, answered, and
rescheduled.

The path must never load or write a `Card`. That is the structural guarantee behind
"editing content never touches review state" (ARCHITECTURE §3) — verify it holds.

## In scope

### Endpoint

- `POST /v1/reviews/{cardId}` with body `{ "grade": "CORRECT" }` or `"INCORRECT"`.
- `200` with the resulting review state: the new box number and the next due date.
  The client sends a **fact** (how the answer went); the server owns the algorithm. The
  client never writes `box` or `dueAt` directly.
- `404` when the card has no review **for this user** — i.e. the review does not exist,
  or belongs to someone else. The two cases must be indistinguishable, byte-identical,
  exactly as with `GET /v1/cards/{id}`.
- `400` on a missing or unrecognised grade value.
- `401` without a valid bearer token (existing filter, unchanged).

### Domain

- `Review` gains the behaviour that grading is an operation **of the aggregate**, not of
  the use case:

  ```
  Review grade(Grade grade, LeitnerStrategy strategy, Instant now)
  ```

  It asks the strategy for the next `ReviewState` and returns a new `Review` carrying
  it. `Review` stays immutable; no setters. The use case must not compute the new state
  itself — it orchestrates, it does not contain domain logic.

- `ReviewRepository` gains `Optional<Review> findByCardIdAndOwner(CardId, UserId)`,
  mirroring `CardRepository.findByIdAndOwner`. Ownership belongs in the port contract,
  so another user's review is never loaded.
  The existing `findByCardId` is **removed** — it can load a foreign review, and now
  that a real caller exists there is no reason to keep an unsafe variant. Update the
  persistence integration test accordingly.

### Application

- `GradeReviewCommand` — record in `application`: `(UserId owner, CardId cardId,
  Grade grade)`. All fields required; the compact constructor null-checks. No defaulting.
- `GradeReviewUseCase` (interface, one method) → `DefaultGradeReviewUseCase`.
  `Review grade(GradeReviewCommand command)`.
  Body: load via `findByCardIdAndOwner`; on empty throw `ReviewNotFoundException`;
  call `review.grade(command.grade(), strategy, clock.instant())`; save; return.
  **No `@Transactional`** — this is a single write, and the repository's own
  transactionality covers it (ARCHITECTURE §4).
- `ReviewNotFoundException` — alongside `CardNotFoundException`. Carries ids for the
  log message only; the message never reaches the client.

### Web

- `ReviewApi` (interface) → `ReviewController`, `@RequestMapping("/v1/reviews")`.
  `POST /{cardId}`, taking `@AuthenticatedUser UserId` and `@Valid @RequestBody`.
- `GradeDto` — web-local enum `CORRECT` / `INCORRECT`, with conversion to the domain
  `Grade`. The domain enum must not appear in a DTO (same rule as `VisibilityDto`).
- `GradeRequestDto(@NotNull GradeDto grade)`.
- `ReviewResponseDto(int box, Instant dueAt)` — a record. Do not expose `cardId` or
  `owner`; the caller already knows both.
- `ApiExceptionHandler` gains a handler for `ReviewNotFoundException` → `404`,
  new `ErrorCode.REVIEW_NOT_FOUND`. Same discipline as `CARD_NOT_FOUND`: fixed detail
  text, no ids in the body, `ProblemDetail` built by hand so Spring does not populate
  `instance` with the request path.

### Deliberately a 500, not a 404

If a `Card` exists but its `Review` does not, that is a broken invariant — the two are
written in one transaction at creation. Do not add a code path that translates it to a
client error. It will surface through the catch-all `500`, which is the correct, loud
signal. Do not "handle" it.

Note this is not the same as the `404` case above: a missing review for *this user*
means the card is missing or foreign, which the caller may not distinguish. The
impossible case is a review missing for a card the repository confirms exists — do not
add such a check.

## Out of scope

- No `/v1/session/cards` — the next slice.
- No card list, update, or delete.
- No grading history, no audit trail, no "how many times answered" counter. `Grade` is
  an incoming fact, never persisted.
- No progress reset endpoint (PROJECT.md §5 explicitly excludes it from the MVP).
- No changes to `Card`, `CardRepository`, `CardController`, or the cards table.
- Do not delete or rewrite files outside those this spec names.

## Acceptance criteria

1. `./gradlew clean build` is green.
2. `POST /v1/reviews/{cardId}` with `CORRECT` on a box-1 card returns `200`, box `2`,
   and `dueAt = now + the second configured interval`.
3. `CORRECT` on a box-5 card keeps it in box 5 at the maximum interval — there is no
   "mastered" state (PROJECT.md §5).
4. `INCORRECT` from any box returns box `1` with `dueAt = now + the first interval`.
5. Grading another user's card returns `404`, byte-identical to grading a card id that
   does not exist at all.
6. A malformed or absent `grade` returns `400` with `code: VALIDATION_FAILED`.
7. `grep` confirms no code path in the grading flow loads or saves a `Card`: neither
   `DefaultGradeReviewUseCase` nor `ReviewController` references `CardRepository`,
   `Card`, or `GetCardUseCase`.
8. The domain source set still contains no `jakarta.persistence`, Spring, or web imports.

## Tests

- Domain: `Review.grade` returns a new instance with the state the strategy produced;
  the original is unmodified; the strategy is called with the supplied `Instant`.
- Use case, mocked ports and a fixed `Clock`: loads via `findByCardIdAndOwner`; empty →
  `ReviewNotFoundException`; saves the review the aggregate returned; `CardRepository`
  is never touched (assert with a mock that is verified to have zero interactions).
- Persistence integration (Testcontainers): `findByCardIdAndOwner` returns empty for
  another owner's review.
- Web (`@WebMvcTest`, use case mocked): `200` with the new box and dueAt; `404` shape
  and byte-identity; `400` on a bad grade; `401` without a token.
- Endpoint integration (Testcontainers): create a card, grade it `CORRECT`, assert the
  persisted review is in box 2; grade `INCORRECT`, assert box 1. Then assert the card
  row is unchanged by both operations — this is acceptance criterion 7 proven at
  runtime rather than by inspection.

## Notes

- Follow the interface and naming rules in the project `AGENTS.md`.
- Time enters as a parameter, from the injected `Clock`. The domain never calls
  `Instant.now()`.
- Suggested order: domain (`Review.grade`, port method) → application → web → tests at
  each layer. Compile after each.