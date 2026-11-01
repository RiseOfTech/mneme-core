## Purpose

Lets a user keep a private collection of study cards. A card is a word, its translation
and a personal association, with an optional image.

## ADDED Requirements

### Requirement: Create a card

The system SHALL create a card for the caller from a word, translation and association,
with an optional image URL and optional visibility defaulting to `PRIVATE`. Creating a card
SHALL also create its review state in the same atomic operation.

#### Scenario: Valid card
- **WHEN** an authenticated caller posts a valid card to `/v1/cards`
- **THEN** the response is `201` with the created card
- **AND** the card can be read back by its id

#### Scenario: Blank required field
- **WHEN** the caller posts a card with a blank `word`, `translation` or `association`
- **THEN** the response is `400` with code `VALIDATION_FAILED`

### Requirement: Read a card

The system SHALL return a card only to its owner. A card owned by another user MUST be
indistinguishable from a card that does not exist.

#### Scenario: Owner reads a card
- **WHEN** the owner requests `GET /v1/cards/{id}`
- **THEN** the response is `200` with the card

#### Scenario: Missing or foreign card
- **WHEN** the caller requests a card that does not exist or belongs to another user
- **THEN** the response is `404` with code `CARD_NOT_FOUND`
- **AND** the two responses are byte-identical

### Requirement: List cards

The system SHALL return all of the caller's cards and only those, newest first, with no
pagination, filtering or search.

#### Scenario: Caller has cards
- **WHEN** the caller requests `GET /v1/cards`
- **THEN** the response is `200` with their cards ordered by creation time, newest first
- **AND** no other user's card appears

#### Scenario: Caller has no cards
- **WHEN** a caller with no cards requests `GET /v1/cards`
- **THEN** the response is `200` with an empty array

### Requirement: Replace a card

The system SHALL treat `PUT /v1/cards/{id}` as a full replacement of the card's content.
An absent `imageUrl` MUST clear the image and an absent `visibility` MUST result in
`PRIVATE`. Replacing a card MUST NOT change its review state.

#### Scenario: Owner replaces a card
- **WHEN** the owner sends a valid replacement
- **THEN** the response is `200` with the updated card
- **AND** the card's box and due time are unchanged

#### Scenario: Optional fields omitted
- **WHEN** the replacement has no `imageUrl` and no `visibility`
- **THEN** the card has no image and its visibility is `PRIVATE`

#### Scenario: Blank required field on replace
- **WHEN** the replacement has a blank `word`, `translation` or `association`
- **THEN** the response is `400` with code `VALIDATION_FAILED`

#### Scenario: Replacing a missing or foreign card
- **WHEN** the caller replaces a card that does not exist or belongs to another user
- **THEN** the response is `404` with code `CARD_NOT_FOUND`
- **AND** the two responses are byte-identical

### Requirement: Delete a card

The system SHALL delete a card together with its review state in one atomic operation.

#### Scenario: Owner deletes a card
- **WHEN** the owner requests `DELETE /v1/cards/{id}`
- **THEN** the response is `204` with no body
- **AND** the card and its review state no longer exist

#### Scenario: Deleting a missing or foreign card
- **WHEN** the caller deletes a card that does not exist or belongs to another user
- **THEN** the response is `404` with code `CARD_NOT_FOUND`
- **AND** nothing is deleted
- **AND** the two responses are byte-identical

### Requirement: Visibility is stored but inert

The system SHALL store and return each card's visibility. No behaviour MUST depend on it.

#### Scenario: Listing ignores visibility
- **WHEN** a caller owns both private and public cards
- **THEN** `GET /v1/cards` returns all of them

### Requirement: Duplicate cards are permitted

The system SHALL accept several cards with the same word or translation for one owner.

#### Scenario: Same word twice
- **WHEN** a caller creates a second card with a word they already have
- **THEN** the response is `201`
