# ADR 0003 — OIDC rather than SAML for authenticating the mobile clients

- **Status:** Accepted, pending configuration by the university
- **Date:** 2026-09-01
- **Deciders:** Brian Vargas (K-Forge), with Gabriel Cruz Parra (IT Directorate, FUKL)

## Context

KApp's clients are native Android and iOS applications that call KApp's own API. The university
runs Microsoft 365, so Microsoft Entra ID already exists as its identity provider, and a student's
`@konradlorenz.edu.co` account is the natural credential.

Entra ID publishes an application in a tenant as an *enterprise application* regardless of protocol,
and supports both SAML 2.0 and OpenID Connect. Asked which to use, the IT Directorate's first answer
was SAML — the protocol most institutional integrations there already use, Moodle among them — and,
when pressed, confirmed there is **no technical limitation** preventing OIDC, only that SAML is more
familiar to operate.

So the choice is genuinely open, and it has to be made before the mobile clients implement sign-in.

## Decision

**Use OpenID Connect with the Authorization Code flow and PKCE**, through Microsoft's MSAL libraries
for Android and iOS.

## Rationale

SAML was designed for browser-based single sign-on to web applications. Three consequences make it a
poor fit here:

1. **It needs a browser.** A native client has to embed a web view to complete a SAML exchange.
   That is a worse experience, and embedded web views for authentication are discouraged by
   [RFC 8252](https://datatracker.ietf.org/doc/html/rfc8252) precisely because the user cannot
   verify what they are typing their password into.

2. **A SAML assertion is not an API credential.** It is an XML document asserting an identity to a
   service provider. It cannot be presented as a bearer token on calls to KApp's API, so the
   assertion would have to be exchanged for something else — machinery that exists only to bridge a
   protocol mismatch.

3. **It is not what Microsoft recommends for this case.** MSAL implements OIDC with PKCE for native
   clients, and it is the documented path. Choosing SAML means leaving the supported path and
   maintaining the difference ourselves.

OIDC returns a signed JWT that KApp's services already know how to validate — see
[ADR 0002](0002-rs256-with-a-published-jwks.md). The token from Entra and the token KApp issues
today have the same shape and are verified the same way, against a published JWKS.

## Alternatives considered

**SAML 2.0.** Preferred by the IT Directorate for operational familiarity, and that is a real cost
worth respecting: whoever operates it afterwards matters more than whoever builds it. Rejected on
the technical grounds above, with the understanding that if institutional policy later requires
SAML, KApp will implement it.

**Keep local credentials permanently.** Rejected. It means storing passwords for university members
when the university already authenticates them, and it cannot prove an account is still active.

## Consequences

**Positive**

- The supported, documented native flow. No embedded web view, no assertion exchange.
- KApp stops storing passwords for university members, removing a class of risk entirely.
- Sign-in proves the account is *currently* active, which e-mail verification cannot.
- Registration, e-mail verification and invitation codes become unnecessary for members. They remain
  for guests, who by definition have no university account.

**Negative**

- Adds a dependency on the university's tenant. If the registration is not granted, members cannot
  sign in through Entra and the local path stays in use.
- OIDC is likely the first of its kind at this institution, so the operations side is less
  rehearsed than SAML. K-Forge documenting the setup is part of the cost of this decision.

**Mitigation, already built.** Authentication sits behind `IdentityProviderPort` in `auth-service`,
with `LocalCredentialsAdapter` implementing it today. `AuthenticatedIdentity` carries only
`(subject, email, roles, emailVerified)`, each of which has an Entra equivalent — `oid`,
`preferred_username`, app-role assignment, implicit. No password hash and no storage type appears in
the port's signature.

That seam is what keeps this decision cheap to reverse: switching to Entra means writing a second
adapter, and being forced onto SAML later means writing a different one. Neither touches the domain,
and neither touches the four services that merely validate tokens.

## Follow-up

The university defers configuration until there is something concrete to register. The productive
next step is therefore not to keep discussing the protocol but to arrive with the exact parameters:
application name, redirect URIs, requested scopes (`openid`, `profile`, `email`, `User.Read`) and
the platform type. The objection raised was about operational effort, so the answer is to remove
the effort.

`Mail.Send` on the same registration is worth requesting at the same time — it covers guest e-mail
verification through Microsoft Graph and avoids standing up an SMTP relay separately.
