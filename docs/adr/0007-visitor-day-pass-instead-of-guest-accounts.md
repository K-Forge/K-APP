# ADR 0007 — A visitor day pass instead of guest accounts

- **Status:** Accepted and implemented
- **Date:** 2026-09-05

## Context

`POST /auth/register/guest` accepted **any** e-mail address, with no invitation code, and created a
real credential plus a real profile carrying `ROLE_GUEST`. It existed so a visitor — an applicant, a
relative at an event, somebody attending a talk — could see the campus map.

A visitor is on campus for an afternoon. An account is a thing that outlives them: a row in
`credentials`, a row in `users`, an unverified e-mail address, and nothing that ever removes any of
it. The endpoint was also completely open, so it accumulated whatever anyone typed.

## Decision

Reception issues a **one-day pass**. The visitor redeems it by presenting an identity document and
receives a token valid for **24 hours** that opens the campus map and nothing else. **No account is
created.** Open guest registration is removed.

## Rationale

**A token is the right shape for a visit.** It expires on its own; there is nothing to clean up,
nothing to verify, and nothing left behind. The pass is also a record of who was in the building,
which reception has an actual use for and an open registration form never provided.

**The token is an ordinary `ROLE_GUEST` token, not a new role.** `ROLE_GUEST` is already refused by
`semaphore-service` and `schedule-service` and allowed by `map-service`, with tests asserting each.
Reusing it means "the pass opens the map and nothing else" **is** the authorization matrix the
services already enforce, rather than a second mechanism that could drift away from it. Its subject
is `visitor:<pass id>` and it carries no `email` claim, because there is no account behind it.

**Redemption is a single `findAndModify`** guarded on `redeemedAt` still being null, so the server
decides who wins when a code is presented twice at once. Read-then-write would let two visitors
redeem one code — rarely, under load, and invisibly, which is the worst way to discover a register
is wrong.

**Codes avoid `I`, `O`, `0` and `1`.** They are read aloud across a counter and typed by somebody
who has never seen them written down.

## Alternatives considered

**Keep guest accounts but require an invitation code.** Rejected: it still creates an account that
outlives the visit, and it makes reception mint invitation codes, which are a different thing with a
different lifecycle.

**An anonymous map with no token at all.** Simplest, and genuinely defensible for a map that is not
secret. Rejected because reception asked for traceability — knowing who was in the building — and an
anonymous map provides none.

**Store a hash of the document number rather than the number.** Considered seriously. Rejected
because it defeats the purpose: reception needs to *read* the register to answer "who was here", and
a hash answers only "was it this person", which requires already knowing the answer.

## Consequences — this changes the project's data profile

**KApp now stores personal data under Ley 1581 de 2012 (habeas data).** Before this it stored no
institutional or government-issued data at all. That brings obligations, and each is met
deliberately:

- **Purpose:** reception knowing who was in the building. Nothing else reads the register; it is
  exposed only under `/auth/admin/visitor-passes`, to `ROLE_ADMIN`.
- **Retention: 30 days**, enforced by a MongoDB **TTL index** on `purgeAt`. The database deletes the
  document itself. A scheduled task in the service was considered and rejected: it stops when the
  service is down, when somebody disables it, or when it throws — and personal data quietly
  outstaying its retention period is precisely the failure nobody notices until they are asked to
  prove it did not happen.
- **Minimisation:** the document number never reaches a log line. The register has a retention
  period; a log line has none.

**Dirección de TI has to be told, and has not been.** The technical summary shared with Gabriel Cruz
Parra says KApp stores no institutional records. That stopped being accurate the day this shipped.
Tracked as S12 in `SECURITY-AUDIT.md`.

**A redeemed pass cannot be deleted on request** — the API answers `409`. That row is no longer a
pass, it is the record of a visit, and a register reception could quietly edit is not a register. It
leaves on its own after 30 days.
