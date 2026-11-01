## Context

Code and tests are the source of truth for this baseline. Implementation detail stays in
ARCHITECTURE.md and is not repeated in the specs.

## Decisions

**One change, not nine.** Replaying the old documents as historical changes would invent a
history that never happened and restate behaviour that was later replaced.

**Defects are not requirements.** Where the code contradicts an earlier intent, the
baseline stays silent on it instead of making the defect normative. Each one below is
fixed by its own change, which adds the requirement.

**New cards are specified as they behave now**, first due after the first interval. This
contradicts ARCHITECTURE.md section 4 and is corrected by `show-new-cards-immediately`.

## Known deviations

Observed against the running application on 2026-11-01.

| Request | Actual | Expected | Follow-up |
|---|---|---|---|
| `GET /swagger-ui.html` without a token | `401` | Reachable outside `prod` | `fix-swagger-ui-public-path` |
| Path id that is not a UUID, on any `/v1` route | `500` | `4xx` | Not scheduled |
| `POST /v1/cards` with an unsupported `Content-Type` | `500` | `415` | Not scheduled |
| Blank `imageUrl` on create or update | `500` | `400` | Not scheduled |

The old `jwt-auth` document required a proper `4xx` for a bad `Content-Type`; only the
`405` case was implemented and tested.

An access token whose `sub` is not a UUID was found by reading the code, which should
answer `500` instead of `401`. This was not observed at runtime.

Image upload is promised by PROJECT.md section 5, but only an image URL exists, so the
baseline specifies URL only.

## Risks / Trade-offs

Scenarios without a test are listed in `tasks.md`. They are true by inspection only and
can regress silently.
