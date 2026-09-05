# Moving the development databases to Atlas

Step by step, in the order the Atlas interface asks for it. About twenty minutes.

The goal is one free cluster holding the same five databases the local stack has, with the same
rule: **one account per service, able to read and write exactly one database.** After this, the six
of us share one development database and nobody has to run MongoDB on their machine.

> **What I cannot do for you.** Every step below needs your Atlas account. I am not going to handle
> your credentials or an API key with permissions over your organisation, so the cluster and its
> users are yours to create. Everything on this side is already written and waiting for the
> connection strings — step 6 is the only place your work meets mine.

---

## 1. Account and project

1. Sign up or sign in at <https://cloud.mongodb.com>.
2. Create an organisation if you have none — call it **K-Forge**.
3. Inside it, create a project named **KApp Dev**.

One project per environment. When there is a production cluster it goes in its own project, so a
mistake made in development cannot reach it: users, network rules and alerts are all per project.

## 2. The cluster

1. **Create** → choose the **M0** tier. It is free and does not expire.
2. Provider **AWS**, region **N. Virginia (us-east-1)** — the lowest latency to Bogotá among the
   regions M0 offers. Take **São Paulo (sa-east-1)** instead if it appears in the list.
3. Name it **kapp-dev**.
4. Create. It takes three to five minutes to provision.

M0 is a three-node replica set, which is what the local container imitates. Transactions work the
same, so nothing in KApp behaves differently against it.

**Skip the "Quickstart" wizard's user and IP prompts** if it shows them; the next two steps do the
same thing properly.

## 3. The five database users

**Security → Database Access → Add New Database User**, five times.

For each one: **Password** authentication, then under *Database User Privileges* choose
**Specific Privileges** — not "Read and write to any database", which is exactly what we are
avoiding — and add one privilege:

| Username                | Role        | Database         | Collection    |
| ----------------------- | ----------- | ---------------- | ------------- |
| `kapp_auth_user`        | `readWrite` | `kapp_auth`      | *(leave blank)* |
| `kapp_user_user`        | `readWrite` | `kapp_user`      | *(leave blank)* |
| `kapp_semaphore_user`   | `readWrite` | `kapp_semaphore` | *(leave blank)* |
| `kapp_schedule_user`    | `readWrite` | `kapp_schedule`  | *(leave blank)* |
| `kapp_map_user`         | `readWrite` | `kapp_map`       | *(leave blank)* |

Use **Autogenerate Secure Password** and copy each one as you go — Atlas shows it once. Paste them
straight into `.env` (step 6); do not put them in a chat, an issue or a commit.

You do not need to create the databases first. The first write creates them, and Mongock does that
on startup.

> **Atlas users live in `admin`, not in their own database.** This is the one place Atlas differs
> from our local container: every Atlas user authenticates against `admin` regardless of which
> database it can reach. So an Atlas connection string carries **no `authSource` parameter at all**.
> Leaving `authSource=kapp_auth` in there makes authentication fail with a confusing
> "Authentication failed" that looks like a wrong password. The isolation is unaffected — it comes
> from the privilege, not from where the user is stored.

## 4. Network access

**Security → Network Access → Add IP Address**.

For a development cluster holding disposable data, **Allow Access from Anywhere** (`0.0.0.0/0`) is
the practical choice: there are six of us, on home connections, campus Wi-Fi and phone tethering,
and dynamic addresses would mean somebody is locked out every week.

That is a decision made knowingly, not an oversight. What makes it acceptable is that the cluster
holds nothing real — no institutional records, no personal data, no production credentials — and
that the only way in is still a per-service password. It stops being acceptable the moment real
data lands there, and a production cluster must use an IP list or VPC peering instead.

## 5. The connection strings

**Database → kapp-dev → Connect → Drivers → Java 5.2 or later.** Copy the string. It looks like:

```
mongodb+srv://<db_username>:<db_password>@kapp-dev.xxxxx.mongodb.net/?retryWrites=true&w=majority&appName=kapp-dev
```

Build five from it — one per service — by putting in the username, the password, and the database
name after the host:

```
mongodb+srv://kapp_auth_user:PASSWORD@kapp-dev.xxxxx.mongodb.net/kapp_auth?retryWrites=true&w=majority
```

Three things to keep right:

- **No `replicaSet` parameter.** The SRV record carries it; passing it as well is an error.
- **No `authSource`.** See the note in step 3.
- **Percent-encode the password** if Atlas generated one containing `@ : / ? # [ ] %`. Regenerating
  until you get a purely alphanumeric one is easier and just as strong.

## 6. Point the stack at it

In `app/backend/microservices/.env`, replace the five URI values:

```
MONGO_AUTH_URI=mongodb+srv://kapp_auth_user:...@kapp-dev.xxxxx.mongodb.net/kapp_auth?retryWrites=true&w=majority
MONGO_USER_URI=mongodb+srv://kapp_user_user:...@kapp-dev.xxxxx.mongodb.net/kapp_user?retryWrites=true&w=majority
MONGO_SEMAPHORE_URI=mongodb+srv://kapp_semaphore_user:...@kapp-dev.xxxxx.mongodb.net/kapp_semaphore?retryWrites=true&w=majority
MONGO_SCHEDULE_URI=mongodb+srv://kapp_schedule_user:...@kapp-dev.xxxxx.mongodb.net/kapp_schedule?retryWrites=true&w=majority
MONGO_MAP_URI=mongodb+srv://kapp_map_user:...@kapp-dev.xxxxx.mongodb.net/kapp_map?retryWrites=true&w=majority
```

Leave the `MONGO_*_PASSWORD` and `MONGO_ROOT_*` values alone. They provision the **local**
container and are unrelated to Atlas; you still want them working for the days you run offline.

Then start the stack without a local database:

```bash
cd app/backend/microservices
docker compose --profile full --profile dev down
docker compose --profile cloud --profile dev up -d
```

`cloud` starts every service **except** MongoDB.

## 7. Check it worked

```bash
docker compose ps
docker compose logs --tail=30 auth-service | grep -i mongo
```

All services healthy, and no `MongoSecurityException`. Then create the team's accounts against the
cloud database, once, for everybody:

```bash
scripts/create-dev-accounts.sh
```

And confirm the seed data arrived — Mongock runs against Atlas the same way it runs locally:

```bash
curl -s http://localhost:8080/api/catalog/programs \
  -H "Authorization: Bearer <a token from /auth/login>" | head
```

## 8. Tell the others

Each teammate needs the same five lines in their own `.env`. Send the file privately — one person,
one message — not in the group chat. They then run:

```bash
docker compose --profile cloud --profile dev up -d
```

and have a working backend without installing MongoDB.

---

## What does not change

**The tests keep using Testcontainers.** They start their own MongoDB on the machine running them,
deliberately: pointing them at a shared cluster would make them slow and intermittently red, and one
person's run would wipe another's data halfway through someone else's. `./mvnw -B verify` needs
Docker and no Atlas.

**The local profiles keep working.** `--profile core`, `--profile full` and the rest still start a
local MongoDB with its own credentials. Atlas is an option, not a replacement — on a plane or with
bad Wi-Fi, the local stack is the one that works.

---

## If it does not connect

| What you see | What it is |
| --- | --- |
| `Authentication failed` | Almost always `authSource=` left in the string. Remove it — Atlas users live in `admin`. |
| `Timed out ... no primary` | Your IP is not on the Network Access list, or the entry is still "pending". |
| `not authorized on kapp_auth` | The user was created with a privilege on the wrong database, or with a built-in role instead of Specific Privileges. |
| `Unable to look up TXT record` | A network that blocks SRV DNS lookups — some campus and captive-portal Wi-Fi does. Use the non-SRV `mongodb://` string Atlas also offers. |
| Everything hangs on startup | The `cloud` profile was not used and Compose is waiting on a local `mongo` container that is not running. |
