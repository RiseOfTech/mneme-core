## Why

Behaviour was specified in nine one-off documents under `docs/specs/`, and later slices
silently overrode earlier ones, so nothing states what the system does today. OpenSpec
needs a baseline before changes can be expressed as deltas.

## What Changes

- Describe the behaviour of the running service as seven capabilities.
- No code, test or configuration changes. The baseline describes what exists, verified
  against code and tests; the old documents are only a hint.
- Known defects are recorded in `design.md`, not written down as requirements.

## Capabilities

### New Capabilities

- `card-management`: owning, creating, reading, replacing and deleting cards.
- `leitner-review`: grading an answer and scheduling the next review.
- `review-session`: serving the cards that are due.
- `authentication`: validating access tokens and identifying the caller.
- `api-error-contract`: the single shape of every error response.
- `cross-origin-access`: which browser origins may call the API.
- `api-documentation`: the published OpenAPI contract.

### Modified Capabilities

None. There are no existing specs.

## Impact

Adds `openspec/specs/` on archive. `docs/specs/` is retired separately.
