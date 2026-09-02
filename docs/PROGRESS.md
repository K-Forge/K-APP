# Implementation Status

> Read this before proposing large changes. Last updated: August 2026.

KApp is in the thesis pre-proposal phase (_anteproyecto_). The scope was deliberately narrowed in
August 2026: because the university's academic data is not available, the product cannot depend on
it. Accounts are created from scratch, and the MVP covers **users, campus map, a customisable
preloaded schedule and a customisable preloaded career semaforo**. Everything else is deferred.

The backend runs on the lead developer's machine until university hardware exists. There is no
production deployment.

---

## Summary

| Area | Status | Tests | Notes |
|---|---|---|---|
| Platform foundation | Complete | — | Boot 3.5, MongoDB, RS256/JWKS, Mongock, Testcontainers |
| API contract | Complete | — | Five OpenAPI 3.1 specs, linted in CI, served as mocks |
| **Auth: sign-in and registration** | **Merged** | **71** | Institutional and guest registration, verification, invitation codes, `IdentityProviderPort` |
| **User profiles** | **Merged** | **97** | Profiles, internal upsert, accent-insensitive indexed search |
| **Campus map** | **Merged** | **36** | Buildings, spaces, guest-readable, plus an offline pin editor |
| Catalogue and semaforo | In progress | — | Seed and progress tracking |
| Timetables | In progress | — | Enrolments, meetings, overlap detection |
| Admin and developer portal | In progress | — | Angular, runs from a compose `dev` profile |
| Android (Kotlin) | Not started | — | The product. Unblocked by the mocks |
| iOS (Swift) | Not started | — | The product. Unblocked by the mocks |
| Deployment | Not started | — | Runs locally; university hardware pending |

**214 integration tests**, from a repository that had none three days ago. Every service asserts its
full role-by-endpoint authorization matrix with one assertion per case, including every combination
that must be refused — those are the ones that matter.

---

## What exists

**Platform.** Spring Boot 3.5.16 and Spring Cloud 2025.0.3, on the 3.x line deliberately: Mongock
publishes no Boot 4 artifact and Spring Cloud 2025.1.x targets Boot 4. All versions are centralised
in the parent POM so parallel branches never edit it.

**Security.** Every service is an OAuth2 resource server validating RS256 against auth-service's
JWKS. This is what closes S1: identity comes from a signed token, not a header the gateway sets,
so reaching a service port directly gains nothing. Service ports are unpublished; only the gateway
is reachable. RS256 rather than a shared secret because a symmetric key given to seven services is
seven places that can mint an administrator token — and because Entra ID signs RS256 with a JWKS,
making that migration a change of property value.

**Contract first.** `docs/api/*.openapi.yaml` are hand-written and served by Prism containers, so
the mobile team works without waiting for the backend. CI lints them on every push.

**Tests.** 214 integration tests on Testcontainers, from a repository that had none. Beyond the
authorization matrices, they have already earned their keep by catching real defects:

- In `auth-service`, the role check ran before the `try` block, so an invitation code carrying a
  rejected role consumed its slot permanently — the `release()` in the `catch` never ran. A student
  would have burned an invitation on a registration that failed.
- A shared static Testcontainers instance was being stopped by the first test class to finish, while
  sibling classes still depended on it. That is the kind of failure that looks random.

**Two findings worth carrying forward.** MongoDB 7 reports `IXSCAN` for an unanchored,
case-insensitive regex while walking the index over its full unbounded key range — the same cost as
a collection scan under a reassuring name. Any assertion about query plans must check narrowed
bounds and documents examined, not the stage name. And a security regression guard must not claim a
path a service might legitimately want, or it collides with the real chain and stops the context
from starting.

---

## Security findings

Tracked in `docs/SECURITY-AUDIT.md`.

| ID | Severity | Status |
|---|---|---|
| S1 | Critical | Resolved. Per-service token validation; service ports unpublished |
| S2 | High | Resolved in the merged services. Every endpoint carries an explicit rule, asserted per role in tests |
| S3 | Moderate | Resolved. CORS allow-list replaces the wildcard |
| S4 | Moderate | Resolved. Eureka discovery locator off; `/internal/**` has no route |
| S5 | Moderate | Resolved. Actuator exposes health and info only, without details |
| S6 | Low | Resolved. Debug logging off by default |
| S7 | Low | Open. Refresh tokens and revocation are deferred; tokens expire in one hour |
| H1 | High | Open. Two development credentials remain readable in git history |

---

## Next

1. Registration and verification. **Blocked on an SMTP relay** — without e-mail there is no
   verification and therefore no registration, which is the front door of the product. The
   verification step sits behind a flag so the rest of the work is not blocked meanwhile.
2. Academic catalogue and curricula, seeded from the published Ingenieria de Sistemas pensum
   (~51 courses, 9 levels, 4 areas, full prerequisite graph).
3. Student progress and prerequisite-aware eligibility.
4. Timetables.
5. Campus map. **The floor plan images are the longest-lead item**: obtaining them is human
   latency, not engineering, and it is the likeliest thing to slip. Development proceeds against
   placeholder plans.
6. Mobile clients.

---

## Deferred work

Everything below is known and deliberately postponed. Recorded so it reads as a decision rather
than an oversight.

### Product

| Item | Note |
|---|---|
| `course-service`, `assignment-service` | Frozen. Still in the tree, out of the reactor, compose and CI. They target PostgreSQL/JPA |
| Enrollment, assignments, grading | Out of MVP scope |
| The 17 services in `MICROSERVICES-IDEAS.md` | Out of MVP scope |
| Web client | Frozen. It was a prototype of the mobile layout, not a product surface |

### Engineering

| Item | Note |
|---|---|
| Refresh tokens, logout, revocation | Tokens simply expire (S7) |
| Distributed rate limiting | The gateway limits credential endpoints in memory. Correct for one instance; **revisit before running a second** |
| Spring Cloud Config Server | Configuration is per-service environment variables |
| Distributed tracing | No Zipkin |
| Database per service | Already one database per service, on one MongoDB instance |
| `app/database/init.sql` | Legacy PostgreSQL schema, reference only |

### Deployment

| Item | Note |
|---|---|
| University hardware | Needs 8 GB RAM minimum, 16 GB recommended, 4 vCPU |
| TLS | Required, not optional: iOS blocks plaintext HTTP and Android has since API 28 |
| Multi-architecture images | Built on Apple Silicon; a typical x86 server needs `docker buildx` |
| Backups | Neon did this invisibly. On-premise needs `mongodump` on a schedule, copied off the host |
| Entra ID | Needs an application registration from the university |

### Repository governance

| Item | Note |
|---|---|
| Branch protection | Ruleset requires linear history and allows merge commits, which are mutually exclusive. Fix: squash |
| `CODEOWNERS` | Missing. Ownership must be the team: GitHub does not accept a code owner approving their own pull request |
| Secret scanning, push protection | Disabled |
| Dependabot | Disabled, no `.github/dependabot.yml` |
| Social preview | `portfolio-cover.png` is already 1200x630 but must be uploaded through the GitHub UI |
| Credential rotation (H1) | Two development credentials readable in git history |
