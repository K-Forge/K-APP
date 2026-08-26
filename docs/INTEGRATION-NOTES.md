# Integration notes

Working notes for merging the four parallel Phase 1 tracks back into `feature/mvp-backend`.
Delete this file once the merge is done and the items below are resolved.

---

## 1. The shared security chain backs off too easily — fix in `common` after the merge

`KappSecurityAutoConfiguration` declares its default `SecurityFilterChain` with
`@ConditionalOnMissingBean`. That means **any** chain a service declares silently disables the
shared one, including a chain that was only ever meant to cover one path.

Track B hit this in `user-service`: adding a chain for `/internal/**` disabled the default, and
every `/api/users/**` route would have been left unauthenticated with nothing failing loudly. They
worked around it by restating the default chain inside their own config. That workaround is
correct but it is a trap waiting for the next person, and it duplicates the rule in two places.

**The fix**, once all four tracks are merged and `common` is safe to edit:

- Give the shared chain `@Order(Ordered.LOWEST_PRECEDENCE)` and drop `@ConditionalOnMissingBean`,
  so it acts as a catch-all.
- Services add narrower chains with a `securityMatcher` and a higher precedence for the paths they
  own — `/internal/**`, the public auth endpoints — and nothing else.
- Then remove the restated default from `user-service` and check `auth-service`'s
  `AuthSecurityConfig` still covers every path rather than only the public ones.

Spring Security supports several `SecurityFilterChain` beans; the first whose matcher accepts the
request wins. That is the intended pattern and it removes the footgun entirely.

Add a test that a service declaring its own narrow chain still rejects an anonymous request to an
unrelated path. That is the failure mode this is guarding against, and it is invisible otherwise.

---

## 2. E-mail normalisation must agree across auth and user

`user-service` lowercases the e-mail before upserting on `POST /internal/users`, because a retry
differing only in capitalisation would otherwise slip past the unique index and create the exact
duplicate the upsert exists to prevent.

`auth-service` must normalise the same way, both before calling user-service and before writing the
credential. If the two disagree, the profile and the credential drift apart for anyone who types
their e-mail with a capital letter. Confirm both sides at merge time.

---

## 3. Search matches word prefixes, not infixes

`user-service` search folds accents at write time into an indexed `searchTokens` array and queries
it with anchored, flagless regexes, so the query can actually use the index. The consequence is
that `varg` finds Vargas but `unoz` does not.

This is the right trade for a type-ahead and it is worth keeping. Infix matching cannot be served
from an index, so if a real need for it appears, it needs a different mechanism (Atlas Search, or
a separate n-gram field), not a relaxed regex.

Note it in the API documentation so the mobile clients set expectations in the UI.

---

## 4. Contract fixes already applied

- `user.openapi.yaml`: `phone` was `maxLength: 15`, but E.164 permits 15 *digits* plus the leading
  `+`, so a fully qualified number reaches 16 characters. Raised to 16 and given an explicit
  `^\+[1-9]\d{1,14}$` pattern. Found by Track B.

---

## 5. Merge order

`common` is untouched by every track, so the four branches should merge cleanly. Merge in
dependency order and run the full reactor after each one rather than at the end, so a failure is
attributable:

```
feature/mvp-user  ->  feature/mvp-auth  ->  feature/mvp-semaphore  ->  feature/mvp-map
```

After all four: apply fix 1 above, re-run `./mvnw -B verify`, then bring up
`docker compose --profile full` and re-check the Phase 0 exit criteria, which are the only end to
end proof that the services still agree with each other.
