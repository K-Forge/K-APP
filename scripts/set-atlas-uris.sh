#!/usr/bin/env bash
#
# Points the five services at an Atlas cluster, or back at the local container.
#
#   scripts/set-atlas-uris.sh            # ask for the host and the five passwords
#   scripts/set-atlas-uris.sh --local    # go back to the local MongoDB
#
# Assembling five connection strings by hand is where every Atlas mistake in
# docs/ATLAS-SETUP.md comes from, and two of the three fail with "Authentication failed",
# which reads exactly like a wrong password. This composes them instead:
#
#   - no authSource. Atlas keeps every user in `admin` regardless of which database it can
#     reach, so pinning it to the service's own database fails.
#   - no replicaSet. The SRV record already carries it; passing it as well is an error.
#   - the password is percent-encoded, so a generated one containing @ : / ? # or % works
#     instead of silently truncating the string at the wrong character.
#
# Passwords are typed, never passed as arguments: an argument lands in your shell history.
set -euo pipefail

cd "$(dirname "$0")/../app/backend/microservices"

ENV_FILE=".env"
SERVICES=(auth user semaphore schedule map)

if [ ! -f "$ENV_FILE" ]; then
  echo "No .env here. Generate one first:" >&2
  echo "  ../../../scripts/generate-dev-secrets.sh > .env" >&2
  exit 1
fi

if ! command -v python3 >/dev/null 2>&1; then
  echo "python3 is needed to percent-encode the passwords." >&2
  exit 1
fi

# Replaces KEY=... in .env, or appends it when the key is absent.
set_var() {
  local key="$1" value="$2"
  if grep -q "^${key}=" "$ENV_FILE"; then
    python3 - "$ENV_FILE" "$key" "$value" <<'PY'
import sys, pathlib
path, key, value = sys.argv[1], sys.argv[2], sys.argv[3]
p = pathlib.Path(path)
lines = p.read_text().split("\n")
# The value is written verbatim rather than through a regex replacement: a password can
# contain any character, and sed would treat several of them as syntax.
# Single-quoted: the value contains '&', and a shell sourcing this file would
# otherwise read it as a background job and leave the variable empty.
out = [f"{key}='{value}'" if line.startswith(key + "=") else line for line in lines]
p.write_text("\n".join(out))
PY
  else
    printf "%s='%s'\n" "$key" "$value" >> "$ENV_FILE"
  fi
}

backup() {
  cp "$ENV_FILE" "${ENV_FILE}.bak"
  echo "Previous .env saved as .env.bak"
}

# ── Back to local ───────────────────────────────────────────────────────────────────
if [ "${1:-}" = "--local" ]; then
  backup
  # shellcheck disable=SC1090
  set -a; . "./$ENV_FILE"; set +a

  for svc in "${SERVICES[@]}"; do
    pw_var="MONGO_$(printf '%s' "$svc" | tr '[:lower:]' '[:upper:]')_PASSWORD"
    pw="${!pw_var:-}"
    if [ -z "$pw" ]; then
      echo "$pw_var is empty; regenerate .env with generate-dev-secrets.sh" >&2
      exit 1
    fi
    encoded=$(python3 -c 'import sys,urllib.parse;print(urllib.parse.quote(sys.argv[1], safe=""))' "$pw")
    # Local DOES take both: the container is a single-node replica set, and the user lives
    # inside the database it owns, so that database is its authSource.
    set_var "MONGO_$(printf '%s' "$svc" | tr '[:lower:]' '[:upper:]')_URI" \
      "mongodb://kapp_${svc}_user:${encoded}@mongo:27017/kapp_${svc}?replicaSet=rs0&authSource=kapp_${svc}"
  done

  echo
  echo "Pointing at the local container. Start it with:"
  echo "  docker compose --profile full --profile dev up -d"
  exit 0
fi

# ── Atlas ───────────────────────────────────────────────────────────────────────────
echo "Pointing the five services at an Atlas cluster."
echo "Walkthrough, if you have not created it yet: docs/ATLAS-SETUP.md"
echo

printf 'Cluster host (or paste a whole connection string): '
read -r raw
[ -n "$raw" ] || { echo "Nothing entered." >&2; exit 1; }

# Accept a pasted connection string as well as a bare host - it is what Atlas's "Connect"
# button puts on the clipboard, and retyping it by hand is one more chance to get it wrong.
HOST=$(python3 - "$raw" <<'PY'
import re, sys
raw = sys.argv[1].strip()
m = re.search(r'@([^/?]+)', raw)
host = m.group(1) if m else re.sub(r'^mongodb(\+srv)?://', '', raw).split('/')[0].split('?')[0]
print(host.strip())
PY
)

case "$HOST" in
  *.mongodb.net) ;;
  *) echo "Warning: '$HOST' does not look like an Atlas host (expected something.mongodb.net)." ;;
esac
echo "Using host: $HOST"
echo

declare -a ENCODED
for svc in "${SERVICES[@]}"; do
  printf 'Password for kapp_%s_user: ' "$svc"
  read -rs pw
  echo
  [ -n "$pw" ] || { echo "Empty password for kapp_${svc}_user." >&2; exit 1; }
  ENCODED+=("$(python3 -c 'import sys,urllib.parse;print(urllib.parse.quote(sys.argv[1], safe=""))' "$pw")")
done

backup
i=0
for svc in "${SERVICES[@]}"; do
  upper=$(printf '%s' "$svc" | tr '[:lower:]' '[:upper:]')
  set_var "MONGO_${upper}_URI" \
    "mongodb+srv://kapp_${svc}_user:${ENCODED[$i]}@${HOST}/kapp_${svc}?retryWrites=true&w=majority"
  printf '  MONGO_%s_URI -> mongodb+srv://kapp_%s_user:********@%s/kapp_%s\n' \
    "$upper" "$svc" "$HOST" "$svc"
  i=$((i + 1))
done

cat <<'EOF'

Written. The MONGO_*_PASSWORD and MONGO_ROOT_* values are untouched on purpose: they
provision the LOCAL container and have nothing to do with Atlas. You want them working for
the days you are offline - `scripts/set-atlas-uris.sh --local` switches back.

Start the stack without a local database:

  cd app/backend/microservices
  docker compose --profile full --profile dev down
  docker compose --profile cloud --profile dev up -d

Then, once, for everybody:

  scripts/create-dev-accounts.sh

If a service reports "Authentication failed", the password is wrong - the two things that
usually cause that message are not possible here, because this script never writes them.
EOF
