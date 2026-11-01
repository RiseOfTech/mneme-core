## Purpose

Moves a card through the Leitner boxes from the outcome of each answer, so the server alone
decides when the card is next shown.

## ADDED Requirements

### Requirement: Configurable five-box schedule

The system SHALL use five boxes with one global, configurable interval per box, defaulting
to 1, 3, 7, 16 and 30 days. The service MUST refuse to start with any other number of
intervals.

#### Scenario: Wrong number of intervals
- **WHEN** the configuration does not hold exactly five intervals
- **THEN** the service fails to start

### Requirement: Initial review state

The system SHALL place a new card in box 1, due one first interval after creation.

#### Scenario: New card
- **WHEN** a card is created
- **THEN** its review state is box 1, due at creation time plus the first interval

### Requirement: Grade an answer

The system SHALL accept a grade of `CORRECT` or `INCORRECT` at `POST /v1/reviews/{cardId}` and
return the resulting box and due time. The client MUST NOT be able to set the box or due time
directly. Grades MUST NOT be stored, and grading MUST NOT change the card's content.

#### Scenario: Correct answer
- **WHEN** a card in a box below 5 is graded `CORRECT`
- **THEN** the response is `200` with the next box
- **AND** the due time is the answer time plus that box's interval

#### Scenario: Correct answer in the last box
- **WHEN** a card in box 5 is graded `CORRECT`
- **THEN** the card stays in box 5
- **AND** the due time is the answer time plus the last interval

#### Scenario: Incorrect answer
- **WHEN** a card in any box is graded `INCORRECT`
- **THEN** the card moves to box 1
- **AND** the due time is the answer time plus the first interval

#### Scenario: Missing or unrecognised grade
- **WHEN** the grade is absent or is not a known value
- **THEN** the response is `400` with code `VALIDATION_FAILED`

#### Scenario: Grading a missing or foreign card
- **WHEN** the caller grades a card that does not exist or belongs to another user
- **THEN** the response is `404` with code `REVIEW_NOT_FOUND`
- **AND** the two responses are byte-identical
