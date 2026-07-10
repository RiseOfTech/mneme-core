# Card: list, update, delete — spec

## Goal

Complete the MVP card CRUD. This closes `PROJECT.md` §5: after this slice every item in
the MVP scope exists.

This slice introduces no new architecture. Everything it needs — ports, adapters,
mappers, error handling, the `UserId` resolver — is already in place. Reuse the
established patterns; do not invent new ones.

## In scope

### Endpoints

- `GET /v1/cards` → `200` with all cards owned by the caller, ordered by `createdAt`
  descending (newest first). `200 []` when the user has no cards. No pagination, no
  filtering, no search — family scale (PROJECT.md §8).
- `PUT /v1/cards/{id}` → `200` with the updated card. **Full replacement**: the body
  carries every content field. An absent `imageUrl` means the card now has no image; an
  absent `visibility` defaults to `PRIVATE`, as on create.
- `DELETE /v1/cards/{id}` → `204 No Content`. Deleting a card also deletes its review.
- `404` on `PUT` or `DELETE` for a missing card **or a card owned by another user** —
  byte-identical bodies, as with `GET /v1/cards/{id}`.
- `401` without a valid bearer token.

### The visibility flag stays inert

`visibility` is stored and returned. **Nothing filters or branches on it.** `GET
/v1/cards` returns private and public cards alike, because they all belong to the
caller. Do not add a query parameter, a filter, or any visibility-dependent behaviour —
the field is groundwork for Phase 2 (PROJECT.md §5).

### Domain

- `Card` gains:

  ```
  Card update(Word word, Translation translation, Association association,
              Image image, Visibility visibility)
  ```

  Returns a **new** `Card` carrying the same `id` and `owner` and the supplied content.
  `Card` stays immutable; no setters. That the method takes neither `id` nor `owner` is
  the point: ownership and identity are not editable, and this is expressed by the
  signature, not by a comment.

  **Updating content must not touch review state.** `Card.update` knows nothing about
  `Review`; the update use case never loads one. This is the invariant from
  ARCHITECTURE §3, and this slice is where it would break if the aggregates were not
  genuinely separate.

- `CardRepository` gains:
    - `List<Card> findAllByOwner(UserId owner)` — ordered by `createdAt` descending.
    - `void deleteByIdAndOwner(CardId id, UserId owner)`.
- `ReviewRepository` gains `void deleteByCardIdAndOwner(CardId cardId, UserId owner)`.

  Every port method carries the owner. No unscoped variants.

### Application

- `ListCardsUseCase` (interface, one method) → `DefaultListCardsUseCase`.
  `List<Card> list(UserId owner)`. No `@Transactional` (single read).

- `UpdateCardCommand` — record in `application`: `(UserId owner, CardId cardId, Word,
  Translation, Association, Image image, Visibility visibility)`. Compact constructor
  null-checks the required fields and defaults `visibility` to `PRIVATE` when null,
  exactly as `CreateCardCommand` does. `image` stays nullable — absent means no image.

- `UpdateCardUseCase` (interface, one method) → `DefaultUpdateCardUseCase`.
  `Card update(UpdateCardCommand command)`. Body: `findByIdAndOwner`; empty →
  `CardNotFoundException`; `card.update(...)`; `save`. No `@Transactional` (single write).
  **Does not inject `ReviewRepository`.**

- `DeleteCardUseCase` (interface, one method) → `DefaultDeleteCardUseCase`.
  `void delete(UserId owner, CardId cardId)`. Annotated `@Transactional` — this is the
  **second** multi-write use case in the codebase.

  Body: `findByIdAndOwner` first; empty → `CardNotFoundException` (so deleting a
  nonexistent or foreign card is a `404`, not a silent success). Then delete the review,
  then the card, in that order — the foreign key `reviews.card_id → cards.id` forbids
  the reverse.

  **No `ON DELETE CASCADE`.** The link between the two aggregates is managed by the
  application layer, which creates them together and now deletes them together. The
  database does not own that relationship (ARCHITECTURE §3, §6). Do not add a cascade to
  the schema; no migration is needed for this slice.

  Deleting a card whose review is already absent must not fail — `deleteByCardIdAndOwner`
  on a missing row is a no-op, not an error.

### Web

- `CardApi` / `CardController` gain three methods. `CardResponseDto` is reused for list
  and update; no new response type.
- `UpdateCardRequestDto` — record, same fields and validation as
  `CreateCardRequestDto` (`@NotBlank` on word, translation, association; nullable
  `imageUrl`, nullable `VisibilityDto`). Do not reuse the create DTO: the two contracts
  are free to diverge later, and sharing a request type couples them.
- `DELETE` returns `204` with no body (`ResponseEntity<Void>` or `@ResponseStatus`).

No `ApiExceptionHandler` change: `CardNotFoundException` already maps to `404`.

## Out of scope

- No uniqueness constraint on `(owner, word)` or `(owner, word, translation)`. Duplicate
  cards are permitted in the MVP. This is a separate product decision, to be recorded in
  `PROJECT.md` before it is built. Do **not** add a unique index, a `409`, or a
  duplicate check.
- No pagination, sorting parameters, filtering, or search on `GET /v1/cards`.
- No `PATCH`. No partial updates.
- No progress reset. Updating a card never resets its review (PROJECT.md §5).
- No bulk operations, no soft delete, no archive.
- No changes to `Review`, `LeitnerStrategy`, the session, or grading.
- No migration. No schema change.
- Do not delete or rewrite files outside those this spec names.

## Acceptance criteria

1. `./gradlew clean build` is green.
2. `GET /v1/cards` returns only the caller's cards, newest first; another user's cards
   never appear; a user with no cards gets `200 []`.
3. `PUT /v1/cards/{id}` replaces every content field. Sending no `imageUrl` clears a
   previously set image. Sending no `visibility` results in `PRIVATE`.
4. **After `PUT`, the card's review is unchanged** — same box, same `dueAt`. Asserted
   end to end, not by inspection. This is the slice's most important test.
5. `PUT` and `DELETE` on a foreign card return `404`, byte-identical to a missing card.
6. `DELETE` removes both the card and its review; a subsequent `GET /v1/cards/{id}`
   returns `404` and the review row is gone.
7. `DELETE` on a missing or foreign card returns `404` and deletes nothing.
8. `DefaultUpdateCardUseCase` does not reference `ReviewRepository` — verified by a mock
   with zero interactions, and by `grep`.
9. The domain source set still contains no `jakarta.persistence`, Spring, or web imports.

## Tests

- Domain: `Card.update` returns a new instance with the new content, the same `id` and
  `owner`, and leaves the original unmodified.
- Use cases, mocked ports: list delegates and returns; update loads, calls
  `card.update`, saves, and never touches `ReviewRepository` (`verifyNoInteractions`);
  update on empty → `CardNotFoundException`; delete loads first, then deletes review
  then card, in that order (`InOrder` verification); delete on empty →
  `CardNotFoundException` and no deletion is attempted.
- Persistence integration (Testcontainers): `findAllByOwner` orders by `createdAt`
  descending and excludes another owner's cards; `deleteByIdAndOwner` does nothing for a
  foreign card; deleting a card whose review still exists fails at the FK, confirming
  the ordering in the use case is required (this test documents *why* the order matters).
- Endpoint integration (Testcontainers): create two cards, list them, update one and
  assert its review is untouched, delete it and assert both rows are gone and the list
  shrinks.

## Notes

- Follow the interface and naming rules in the project `AGENTS.md`.
- Suggested order: domain (`Card.update`, port methods) → application → persistence →
  web → integration. Compile after each layer.
- After this slice, `ARCHITECTURE.md` §4's escape hatch is worth re-reading: there are
  now two multi-write use cases. The threshold for introducing a `TransactionRunner`
  port was "three or four". Not yet — but note it.