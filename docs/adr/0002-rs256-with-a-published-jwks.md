# ADR 0002 — RS256 with a published JWKS, verified independently by every service

- **Status:** Accepted and implemented
- **Date:** 2026-08-26

## Context

The previous architecture validated a JWT once, at the API gateway, and forwarded the caller's
identity to the services in an `X-User-Email` header. Services trusted that header without question.

`docs/SECURITY-AUDIT.md` recorded this as finding **S1, critical**: the services published their own
ports, so anyone able to reach one directly could set that header to any value and become any user,
including an administrator. The gateway was a formality, not a boundary.

The token itself was signed with HS512 using a symmetric secret shared across the services.

## Decision

Every service is an independent OAuth2 resource server. `auth-service` signs tokens with **RS256**
using an RSA private key it alone holds, and publishes the public half at
`/.well-known/jwks.json`. Every other service validates against that document via
`spring.security.oauth2.resourceserver.jwt.jwk-set-uri`.

Identity is read from the verified token — never from a header.

## Rationale

**Asymmetric over symmetric.** With HS512 the value that verifies a token is the value that signs
one. Distributing it to seven services creates seven places capable of minting an administrator
token, and one leaked configuration file compromises the whole system. With RS256 the other services
hold only a public key: they can verify and cannot issue.

**Verification at every service, not just the edge.** Once each service checks a signature it cannot
forge, reaching a service directly gains an attacker nothing, and S1 closes at the root rather than
being contained by network configuration that might later change. Service ports were unpublished as
well, but that is now defence in depth rather than the only defence.

**It is the shape Entra ID already speaks.** Entra signs RS256 and publishes a JWKS. Adopting the
same mechanism means [ADR 0003](0003-oidc-over-saml-for-mobile-authentication.md) is a change of
configuration rather than a redesign of how five services authenticate.

## Consequences

- The gateway no longer performs authentication at all. Its `JwtAuthenticationFilter` was deleted:
  keeping it would have meant two implementations of one rule, drifting apart, with a list of public
  paths maintained in a different file from the endpoints it governed.
- Services must use `jwk-set-uri`, **never** `issuer-uri`. The latter performs OIDC discovery while
  the bean is created, so every service would refuse to start unless `auth-service` were already
  up — a boot-order deadlock under `docker compose up` that surfaces as an opaque
  `IllegalArgumentException`.
- Signing keys must be provided by configuration in any shared environment. With none set,
  `auth-service` generates an ephemeral pair and warns: convenient on a laptop, and wrong anywhere
  else, since a restart invalidates every token already issued.
- Key rotation is possible without a flag day, because every token carries a `kid` and the JWKS can
  publish more than one key.

Proven by `AuthServiceIntegrationTest`, which verifies an issued token using only the published
public key, and asserts the JWKS never exposes the private exponent.
