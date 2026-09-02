# Architecture Decision Records

One file per decision that would be expensive to reverse or that someone will otherwise ask about
again in six months. Each records the context, what was decided, what else was considered, and the
consequences — including the bad ones.

They are append-only. A decision that turns out wrong gets a new record superseding the old one, and
the old one stays: the reasoning that led somewhere wrong is worth as much as the reasoning that led
somewhere right.

| # | Decision | Status |
|---|---|---|
| [0001](0001-mongodb-over-postgresql.md) | MongoDB rather than PostgreSQL | Implemented |
| [0002](0002-rs256-with-a-published-jwks.md) | RS256 with a published JWKS, verified by every service | Implemented |
| [0003](0003-oidc-over-saml-for-mobile-authentication.md) | OIDC rather than SAML for the mobile clients | Accepted, pending university configuration |

## Still to record

Decisions already taken whose reasoning currently lives only in commit messages and
`docs/INTEGRATION-NOTES.md`:

- Seven services, with `auth` and `user` kept separate because authentication is the seam an
  external identity provider replaces
- Contract-first OpenAPI with Prism mocks, so client and server work proceed in parallel
- `semaphore-service` owning the academic catalogue, because the curriculum is what the progress
  view displays
- Freezing `course-service` and `assignment-service` rather than deleting or migrating them
