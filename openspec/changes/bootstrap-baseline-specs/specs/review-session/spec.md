## Purpose

Serves the cards a user should study now. The session is a read-only list; the client walks
it and grades each card through the review endpoint.

## ADDED Requirements

### Requirement: Serve due cards

The system SHALL return, at `GET /v1/session/cards`, every card of the caller whose due time
has arrived, ordered by due time ascending, with no limit or pagination. Each entry SHALL
carry the card id, word, translation, association, image URL, box and due time.

#### Scenario: Cards are due
- **WHEN** the caller has cards whose due time has arrived
- **THEN** the response is `200` with those cards, most overdue first

#### Scenario: Card has no image
- **WHEN** a due card has no image
- **THEN** its `imageUrl` is `null`

#### Scenario: Nothing is due
- **WHEN** none of the caller's cards are due
- **THEN** the response is `200` with an empty array

#### Scenario: Another user's cards
- **WHEN** another user has due cards
- **THEN** they never appear in the caller's session

#### Scenario: New card before its first interval
- **WHEN** a card has just been created
- **THEN** it is not in the session

### Requirement: Graded cards leave the session

The system SHALL exclude a card from the session once grading moves its due time into the
future.

#### Scenario: Card graded correct
- **WHEN** a due card is graded `CORRECT`
- **THEN** it no longer appears in the session

#### Scenario: Card graded incorrect
- **WHEN** a due card is graded `INCORRECT`
- **THEN** it no longer appears in the session

### Requirement: Corrupted data is reported

If a due review has no matching card, the system MUST fail the request rather than return a
shortened session.

#### Scenario: Review without a card
- **WHEN** a due review references a card that cannot be found
- **THEN** the response is `500` with code `INTERNAL_ERROR`
