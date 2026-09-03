# Runbook

How to start, stop and troubleshoot KApp locally. Organised by what you are trying to do.

Everything runs in Docker. You almost never need all of it at once — see [Profiles](#profiles).

---

## Prerequisites

| Tool           | Version | Notes                                        |
| -------------- | ------- | -------------------------------------------- |
| Docker Desktop | 4.x+    | Must be **running**, not just installed      |
| JDK            | 21      | Only to build or run tests outside Docker    |
| Maven          | —       | Use the bundled `./mvnw`; do not install one |
| pnpm           | latest  | Only to develop the admin portal itself      |

The repository pins the JDK with `.java-version` (jenv) in `app/backend/microservices/`. If `java`
is not found there, `jenv` is not picking it up:

```bash
jenv add /opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home
```

---

## Profiles

Seven JVMs plus MongoDB is roughly 5 GB. You rarely need that, and on a laptop also running Xcode
or Android Studio you actively do not want it.

| Profile    | What starts                           | RAM     | Use it when                                       |
| ---------- | ------------------------------------- | ------- | ------------------------------------------------- |
| `mock`     | 5 Prism mock servers                  | ~200 MB | Building a mobile screen against the API contract |
| `core`     | mongo, discovery, gateway, auth, user | ~2.5 GB | Working on sign-in or profiles                    |
| `academic` | core + semaphore, schedule            | ~3.5 GB | Working on curricula or timetables                |
| `map`      | core + map                            | ~3 GB   | Working on the campus map                         |
| `full`     | everything                            | ~5 GB   | End-to-end checks before a merge                  |
| `dev`      | the admin portal                      | ~50 MB  | Any time you want the web console                 |

Profiles combine. The portal on its own is not much use, so pair it with a backend:

```bash
docker compose --profile core --profile dev up -d
```

---

## Starting and stopping

All commands run from `app/backend/microservices/`.

```bash
cd app/backend/microservices
```

**Start**

```bash
docker compose --profile core --profile dev up -d      # the usual combination
docker compose --profile mock up -d                    # mobile work, no backend needed
docker compose --profile full --profile dev up -d      # everything
```

**Stop**

```bash
docker compose --profile full --profile dev down       # stop, keep the data
docker compose --profile full --profile dev down -v    # stop and WIPE the database
```

`down` only stops what the named profiles cover, so pass the same profiles you started with, or
just pass `full` and `dev` to catch everything.

**Rebuild after changing code**

```bash
docker compose --profile core build auth-service       # one service
docker compose --profile core up -d auth-service       # restart it

docker compose --profile full build                    # everything, slow
```

**See what is running**

```bash
docker compose ps
docker compose logs -f auth-service                    # follow one service
docker compose logs --tail=50 api-gateway
```

---

## What runs where

| URL                                   | What                                              |
| ------------------------------------- | ------------------------------------------------- |
| http://localhost:4300                 | **Admin and developer portal**                    |
| http://localhost:8080                 | API gateway — the only backend entry point        |
| http://localhost:8080/swagger-ui.html | Aggregated API documentation                      |
| http://localhost:27017                | MongoDB, for `mongosh` and Compass                |
| http://localhost:4010-4014            | Prism mocks: auth, user, semaphore, schedule, map |

**Ports 8081 to 8085 are deliberately unreachable.** The services are only addressable through the
gateway; being able to bypass it was security finding S1. If you need to reach one directly for
debugging, go through the container:

```bash
docker compose exec api-gateway wget -qO- http://auth-service:8081/auth/health
```

---

## Local accounts

Password for all of them: `KForge2026Dev!`

| E-mail                                                                            | Role      |
| --------------------------------------------------------------------------------- | --------- |
| `brian@konradlorenz.edu.co`                                                       | ADMIN     |
| `julian@` · `santiago@` · `diego@` · `ivan@` · `alejandro@` `konradlorenz.edu.co` | STUDENT   |
| `profesor@konradlorenz.edu.co`                                                    | PROFESSOR |
| `visitante@gmail.com`                                                             | GUEST     |

These live in **your** MongoDB. They are not shared, and they disappear with `down -v`.

**To recreate them**, register through the API with a seeded invitation code:

```bash
curl -X POST http://localhost:8080/auth/register \
  -H 'Content-Type: application/json' \
  -d '{"email":"you@konradlorenz.edu.co","password":"KForge2026Dev!",
       "firstName":"Your","lastName":"Name","invitationCode":"KL-20262-STUDENT",
       "studentCode":"506000000","programCode":"506"}'
```

Register no more than a handful at a time — the gateway allows 10 attempts per minute on the
credential endpoints and will answer 429.

`ROLE_ADMIN` is never granted by an invitation code, on purpose: otherwise anyone who could read
the repository could escalate. Promote an existing account instead:

```bash
docker compose exec -T mongo mongosh --quiet --eval '
  const email = "you@konradlorenz.edu.co";
  db.getSiblingDB("kapp_auth").credentials.updateOne({email}, {$set:{roles:["ROLE_ADMIN"]}});
  db.getSiblingDB("kapp_user").users.updateOne({email}, {$set:{role:"ROLE_ADMIN"}});'
```

---

## Tests

```bash
cd app/backend/microservices

./mvnw -B verify                              # everything, ~5 minutes
./mvnw -B -pl map-service -am verify          # one service
```

Tests use Testcontainers, so **Docker must be running** — they start their own MongoDB and do not
touch the one from compose.

Use `verify`, not `install`. `install` writes to the shared local Maven repository and races with
anyone else building at the same time.

---

## Inspecting the database

```bash
docker compose exec mongo mongosh
```

One database per service: `kapp_auth`, `kapp_user`, `kapp_semaphore`, `kapp_schedule`, `kapp_map`.

```javascript
use kapp_user
db.users.find().limit(5)
db.users.countDocuments({role: "ROLE_STUDENT"})
```

---

## Troubleshooting

Every entry here is a failure we actually hit.

### "Network Error (0)" in the portal, no status code

CORS. The browser blocked the request before sending it, which is why there is no HTTP status —
if the backend were down you would see a timeout or a 502 instead.

The gateway allows only listed origins. Check yours is one of them:

```bash
curl -i -X OPTIONS http://localhost:8080/auth/login \
  -H "Origin: http://localhost:4300" \
  -H "Access-Control-Request-Method: POST"
```

A 200 with `Access-Control-Allow-Origin` is fine; a 403 means your origin is missing. Add it to
`kapp.cors.allowed-origins` in `api-gateway/src/main/resources/application.yml`, then rebuild the
gateway.

### The first request after starting returns 504

Expected, once. Services fetch the signing keys from auth-service lazily — deliberately, because
fetching them eagerly would deadlock startup — so the first authenticated request pays for that
fetch plus JVM warm-up and can exceed the gateway's 10-second timeout. Retry; it will be fast.

### 429 Too Many Requests on login or register

The rate limiter, working. Ten attempts per minute per client on the credential endpoints. Wait a
minute.

### A service shows `unhealthy` but seems to work

Read its logs before assuming the service is broken — the health probe may be failing on something
optional:

```bash
docker compose logs --tail=30 auth-service | grep -i "health\|error"
```

auth-service used to report unhealthy purely because no SMTP server exists. That specific case is
fixed, but the pattern recurs.

### Tests fail with "Could not find a valid Docker environment"

Docker Desktop is not running. Start it and wait for the whale icon to settle.

### A service 404s on endpoints you know exist

The container is running an older image than the one you just built. `docker compose up -d`
starts whatever image exists **at that moment**, so kicking off a build and bringing the stack
up before it finishes leaves containers on the previous build — and a service still carrying
only its Phase 0 skeleton answers 404 for every real endpoint.

Check what the running jar actually contains:

```bash
docker compose exec map-service sh -c 'unzip -l /app/app.jar | grep -c "kapp/map"'
```

A handful of classes means the skeleton; several dozen means the real service. Fix by running
`up -d` again once the build has finished — Compose recreates only the containers whose image
changed.

### Everything is slow, the fan is loud

You are probably running `full` when you need `core`. Check with `docker compose ps` and restart
with a narrower profile.

### Starting fresh

```bash
docker compose --profile full --profile dev down -v    # wipes the database
docker compose --profile core --profile dev up -d
```

The seed data — curricula, buildings, spaces, invitation codes — is reloaded automatically by
Mongock on startup. User accounts are not; register them again.

---

## Legacy scripts

`scripts/start-microservices.sh` and `scripts/start-frontend.sh` predate the containers and refer
to services that no longer exist. Use Docker Compose. They are kept only until someone confirms
nothing depends on them.
