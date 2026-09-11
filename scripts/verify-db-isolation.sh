#!/usr/bin/env bash
#
# Proves that the separation between the services' databases is enforced by MongoDB and
# not merely respected by the code.
#
#   scripts/verify-db-isolation.sh
#
# For each service account it checks two things: that the account can read
# and write its OWN database, and that it is refused on the other four. A credential that
# leaks out of one service must not open another service's data.
#
# Requires the stack's `mongo` container to be up and `.env` to hold the passwords that
# provisioned it. Run it after any change to mongo-init/rs-init.js.
set -euo pipefail

cd "$(dirname "$0")/../app/backend/microservices"

if [ ! -f .env ]; then
  echo "No .env here. Generate one with ../../../scripts/generate-dev-secrets.sh > .env" >&2
  exit 1
fi
set -a; . ./.env; set +a

ALL_DBS="kapp_auth kapp_user kapp_semaphore kapp_schedule kapp_map"

# Every try/catch below is at the TOP LEVEL of the mongosh program, including the ones
# inside the loop. mongosh does not rewrite the body of an ordinary function to await the
# driver's promises, so a `catch` written inside a function never fires and the error
# escapes instead. See the header of mongo-init/rs-init.js.
read -r -d '' PROBE <<'JS' || true
const OWN = process.env.OWN_DB;
const ALL = process.env.ALL_DBS.split(" ");
let failures = 0;

// Authenticating against the service's own database is the point of putting the user
// there: the connection string never mentions `admin`.
try {
  db.getSiblingDB(OWN).auth(process.env.SVC_USER, process.env.SVC_PW);
} catch (e) {
  print("  CANNOT AUTHENTICATE against " + OWN + " (" + (e.codeName || e.message) + ")");
  quit(1);
}

for (const name of ALL) {
  let allowed = false;
  let detail = "";
  try {
    db.getSiblingDB(name).isolation_probe.findOne();
    allowed = true;
  } catch (e) {
    detail = e.codeName || e.message;
  }

  if (name === OWN) {
    // readWrite, not read: the service has to be able to write its own data.
    let wrote = false;
    try {
      db.getSiblingDB(name).isolation_probe.insertOne({ at: new Date() });
      db.getSiblingDB(name).isolation_probe.drop();
      wrote = true;
    } catch (e) {
      detail = e.codeName || e.message;
    }
    if (allowed && wrote) {
      print("  allowed  " + name + "   (its own database, read and write)");
    } else {
      print("  FAILURE  " + name + "   should be readable and writable: " + detail);
      failures++;
    }
  } else {
    if (allowed) {
      print("  FAILURE  " + name + "   READ IT. The databases are not isolated.");
      failures++;
    } else {
      print("  refused  " + name + "   (" + detail + ")");
    }
  }
}

quit(failures === 0 ? 0 : 1);
JS

status=0
for svc in auth user semaphore schedule map; do
  own="kapp_${svc}"
  user="kapp_${svc}_user"
  pw_var="MONGO_$(printf '%s' "$svc" | tr '[:lower:]' '[:upper:]')_PASSWORD"
  pw="${!pw_var:-}"

  if [ -z "$pw" ]; then
    echo "${user}: $pw_var is not set in .env" >&2
    status=1
    continue
  fi

  echo "${user}:"
  # The password goes in as an environment variable, not on the command line: anything in
  # argv is visible to every process in the container via /proc.
  if ! docker compose exec -T \
      -e SVC_USER="$user" -e SVC_PW="$pw" -e OWN_DB="$own" -e ALL_DBS="$ALL_DBS" \
      mongo mongosh --quiet --eval "$PROBE"; then
    status=1
  fi
  echo
done

if [ "$status" -eq 0 ]; then
  echo "Every service reaches its own database and no other."
else
  echo "Isolation is NOT holding. See the failures above." >&2
fi
exit "$status"
