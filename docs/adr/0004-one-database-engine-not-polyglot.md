# ADR 0004 — One database engine, not polyglot persistence

- **Status:** Accepted and implemented
- **Date:** 2026-09-05

## Context

KApp is six services, each with its own database. The question came up directly: since the services
are independent, could each use whichever engine suits it — MongoDB for the map, PostgreSQL for the
schedule, MySQL somewhere else? And a sharper version of it: what happens the day `schedule-service`
has to be backed by the university's own timetable system instead of ours?

The question is a good one. "Database per service" is a real microservices pattern, and polyglot
persistence is how it is usually illustrated.

## Decision

**One engine — MongoDB — with one logical database and one account per service.** The seam that
protects against a future migration is the port, not the storage engine.

## Rationale

**The cost of a second engine is paid every day; the benefit arrives once.** A second engine means a
second driver, a second migration tool, a second backup and restore procedure, a second set of
failure modes to learn, a second thing to install on the university's hardware, and a second set of
containers on a laptop already running six JVMs. Six people are building this in their spare time
before November.

**No service's access pattern actually argues for a different engine.** Every one of them reads and
writes a document-shaped aggregate owned by a single writer: a student's timetable, a student's
progress, a building with its floors. There is no service here doing relational reporting, graph
traversal or full-text ranking at a scale that would justify the second engine on merit rather than
on principle.

**What makes a future migration cheap is the port.** The day `schedule-service` is backed by the
university's system, what changes is one adapter behind an interface — the same shape as
`IdentityProviderPort`, which already exists precisely so Entra ID can replace local credentials
without touching a caller. That would be equally true, and equally cheap, if the current storage
were PostgreSQL. Choosing engines per service buys nothing towards it.

**Isolation comes from credentials, not from separate engines.** Each service holds an account with
`readWrite` on exactly one database and no reach into any other — see
[ADR 0005](0005-per-service-database-credentials.md). That is the property the anteproyecto claims,
and it holds on one instance.

## Alternatives considered

**One engine per service, chosen for fit.** The textbook illustration of the pattern. Rejected on
the arithmetic above: the operational cost is continuous and the benefit is hypothetical.

**MongoDB plus Redis for caching and sessions.** Considered and rejected *for now*, not on
principle. Nothing in the MVP has a cache-shaped problem: the heaviest read is one student's
progress document, which is a single indexed lookup. Redis becomes the right answer when there is
distributed rate limiting to do — the gateway's limiter is in-memory today and correct only for one
instance — or when session revocation lands. Both are recorded as deferred in `PROGRESS.md`, and
either would justify adding it. Adding it before then would be a cache with nothing to cache.

**PostgreSQL for everything instead.** Already decided against in
[ADR 0001](0001-mongodb-over-postgresql.md), on different grounds. Nothing since has undermined it.

## Consequences

- One driver, one migration tool, one backup procedure, one thing to install on university hardware.
- A service that genuinely outgrows MongoDB has to be migrated deliberately rather than having
  simply been born elsewhere. That is a real cost, accepted knowingly.
- The claim "servicios independientes, con base de datos propia" in the anteproyecto stays true, and
  is enforced rather than merely observed.
