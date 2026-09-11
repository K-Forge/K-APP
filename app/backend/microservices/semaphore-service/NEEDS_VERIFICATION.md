# Needs verification

Findings from finishing `semaphore-service` that a human with access to the real
Ingeniería de Sistemas Reforma 2018 plan (or to the published `docs/api/*.yaml`
contracts) needs to resolve. Nothing below was silently patched over: the code and the
seed match what is written here, discrepancies included.

## 1. The seeded pensum 1015 does not sum to its own declared totals

`pensum-1015.json` was reconstructed from a printed diagram, not read from SINU. The
change unit that loads it (`V002_SeedIngenieriaDeSistemas`) and `PensumSeedTotalsTest`
both say so and both keep the *declared* header (`totalCredits: 142`, `totalHours: 194`)
exactly as printed, deliberately not recomputed from the 48 seeded items - see the
`Pensum` and `PensumArea` javadoc for why. The gap between what was printed and
what the 48 reconstructed items actually add up to is the finding below.

**Only 48 items could be reconstructed**, against the "roughly 51" the plan is described
as having in `docs/api/semaphore.openapi.yaml`. The 3 or so missing items are unknown,
not guessed at - adding placeholder rows to force a count match would be inventing data.

### Per level: computed vs. declared

| Level | Items | Credits (computed) | Credits (declared) | Weekly hours (computed) | Weekly hours (declared) |
|------:|------:|--------------------:|--------------------:|--------------------------:|--------------------------:|
| 1 | 6 | 16 | 16 | **22** | **21** ⚠️ |
| 2 | 5 | 16 | 16 | 22 | 22 |
| 3 | 5 | **15** | **16** ⚠️ | 21 | 21 |
| 4 | 5 | **15** | **16** ⚠️ | 23 | 23 |
| 5 | 5 | **15** | **16** ⚠️ | **20** | **21** ⚠️ |
| 6 | 6 | **18** | **16** ⚠️ | **23** | **18** ⚠️ |
| 7 | 6 | **18** | **16** ⚠️ | **25** | **19** ⚠️ |
| 8 | 6 | **17** | **16** ⚠️ | **21** | **20** ⚠️ |
| 9 | 4 | 14 | 14 | **20** | **29** ⚠️ |
| **Total** | **48** | **144** | **142** ⚠️ | **197** | **194** ⚠️ |

Only level 2 has no mismatch at all. `PensumSeedTotalsTest` pins the *computed*
column above as a regression baseline (one test per level, plus the two grand totals),
so a future edit that moves a course between levels fails loudly instead of silently
changing these numbers again.

### Per knowledge area: computed vs. declared

Given for the same reason - a wrong level assignment usually also means a course landed
under the wrong area subtotal, which can help triangulate which item(s) actually moved.

| Area | Items | Credits (computed) | Credits (declared) | Weekly hours (computed) | Weekly hours (declared) |
|------|------:|--------------------:|--------------------:|--------------------------:|--------------------------:|
| CB (Ciencias Básicas) | 12 | **38** | **36** ⚠️ | **49** | **44** ⚠️ |
| BIS (Básicas de Ing. de Sistemas) | 9 | **27** | **28** ⚠️ | 40 | 40 |
| ISA (Ing. de Sistemas Aplicada) | 22 | **67** | **66** ⚠️ | **91** | **92** ⚠️ |
| SI (Sociedad e Interculturalidad) | 5 | 12 | 12 | **17** | **18** ⚠️ |

### What to do with this

A student or advisor with the real printed pensum should be able to spot, in minutes,
which course(s) are at the wrong level - the per-level and per-area gaps above are small
(1-7 credits/hours each) and consistent with a handful of courses being one level off,
not a wholesale re-derivation. **Do not "fix" this by editing the declared header or the
area totals to match the computed items** - fix it by moving the misplaced course(s) to
their real level in `pensum-1015.json`, then update the hardcoded expectations in
`PensumSeedTotalsTest` to match. If the totals still don't reconcile after that,
report the new numbers here rather than forcing agreement.

### SINU codes

Only 10 of the 48 items carry a confirmed institutional SINU code (`sinuCode` set and
equal to `code`): `10011, 20015, 10024, 20028, 20037, 17080, 46018, 48022, 59035, 56201`.
The other 32 fixed courses use a generated `IS-*` slug as their `code` with `sinuCode:
null`, and the 6 elective slots have no code at all. Nobody should treat an `IS-*` code
as an institutional one; that is exactly what the nullable `sinuCode` field next to it is
for.

## 2. `userId` type contradicts across the two published contracts

`docs/api/semaphore.openapi.yaml` types `StudentProgress.userId` and the
`GET /api/semaphore/{userId}` path parameter as `integer`/`format: int64`. But the value
that actually flows through the system is the JWT `sub` claim, which `auth-service`
mints as `user-service`'s own profile id - a string (`docs/api/user.openapi.yaml` types
`UserProfile.id` as a UUID string). A UUID does not fit in an `int64`, and treating the
subject as a number anywhere would require parsing a value that is not numeric.

This service implements `userId` as `String` everywhere - `StudentProgress.userId`,
`StudentProgressDto.userId`, `StudentProgressWithReconciliationDto.userId`, and the
`{userId}` path variable on `GET /api/semaphore/{userId}` - matching the *user* contract
and runtime reality rather than the *semaphore* contract's declared type. This is a
deliberate deviation from "match it exactly," recorded here because fixing it properly
means editing `docs/api/semaphore.openapi.yaml` (out of scope for this worktree, and a
breaking change for any client that already reads `userId` as a number) rather than
silently working around it in code.

**Action needed:** whoever owns the OpenAPI contracts should change
`docs/api/semaphore.openapi.yaml`'s `userId` to a string type, consistent with
`docs/api/user.openapi.yaml`.

## 3. Two design decisions for an underspecified edge case

Not bugs, but worth a second pair of eyes since the spec is silent on both:

- **`ProgressSummaryDto.currentLevel` once nothing is left unfinished.** The spec defines
  it as "the lowest level that still holds an unfinished item," which has no answer once
  every item is `PASSED`. This implementation falls back to `pensum.levels()` (the
  plan's last level) rather than `0` or the stored `StudentProgress.currentLevel`. See
  `StudentProgressService.getSummary`.
- **`StudentProgress.currentLevel` vs. the derived summary field of the same name are
  two different things.** The stored field is seeded once, at lazy creation, from
  `user-service`'s `academic.currentLevel` (defaulting to `1` if absent) and is not
  touched again by anything in this service. `ProgressSummaryDto.currentLevel` is
  recomputed from the semáforo on every call. They are expected to disagree once a
  student is partway through a level without every prior item settled - that is not a
  bug in either one.
