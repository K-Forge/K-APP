#!/usr/bin/env bash
#
# Creates the four KApp accounts the development team signs in with.
#
#   scripts/create-dev-accounts.sh              # create the missing ones
#   scripts/create-dev-accounts.sh --recreate   # delete and re-create all four
#
# The passwords are generated ON THIS MACHINE, written to
# `app/backend/microservices/.dev-accounts` (gitignored) and printed once. They are never
# sent through a chat, an issue or a commit: anything said in one of those stays in its
# history forever, and rotating the password later does not remove it from there.
#
# These are DEVELOPMENT accounts on a database that lives on one laptop. They hold
# ROLE_ADMIN because the team is still building the thing and everyone needs to reach
# everything - there is deliberately no separation between "admin" and "developer" yet.
# That has to change before KApp is reachable from outside a laptop; it is recorded in
# docs/SECURITY-AUDIT.md as a pre-production item.
#
# Requires the `core` profile (or wider) to be up, and .env to hold the Mongo root
# password that provisioned the volume.
set -euo pipefail

# Edit this list to match the team. The `.dev` in the address is deliberate: it keeps a
# development account from ever colliding with the person's real institutional address
# once KApp authenticates against Entra ID.
ACCOUNTS=(
  "brian.dev@konradlorenz.edu.co|Brian Steven|Vargas Clavijo|506900001"
  "ivan.dev@konradlorenz.edu.co|Ivan|Desarrollador|506900002"
  "alejandro.dev@konradlorenz.edu.co|Alejandro|Desarrollador|506900003"
  "santiago.dev@konradlorenz.edu.co|Santiago|Desarrollador|506900004"
)

GATEWAY="${KAPP_GATEWAY:-http://localhost:8080}"
INVITATION_CODE="${KAPP_INVITATION_CODE:-KL-20262-STUDENT}"
PROGRAM_CODE="506"

RECREATE=false
[ "${1:-}" = "--recreate" ] && RECREATE=true

cd "$(dirname "$0")/../app/backend/microservices"

if [ ! -f .env ]; then
  echo "No .env here. Generate one with ../../../scripts/generate-dev-secrets.sh > .env" >&2
  exit 1
fi
set -a; . ./.env; set +a

if [ -z "${MONGO_ROOT_PASSWORD:-}" ]; then
  echo "MONGO_ROOT_PASSWORD is empty in .env. Promotion to ROLE_ADMIN needs it." >&2
  exit 1
fi

if ! curl -fsS --max-time 5 "$GATEWAY/auth/health" >/dev/null 2>&1; then
  echo "The gateway is not answering at $GATEWAY." >&2
  echo "Start it with: docker compose --profile core up -d" >&2
  exit 1
fi

# 28 characters, no symbols. Length carries the entropy (~166 bits) and the alphabet
# avoids every character that would need escaping in JSON, in a shell or in a URL - which
# is where these passwords are about to travel.
password() {
  ( set +o pipefail; LC_ALL=C tr -dc 'A-Za-z0-9' < /dev/urandom | head -c 28 )
}

mongo_root() {
  docker compose exec -T -e ROOT_PW="$MONGO_ROOT_PASSWORD" mongo mongosh --quiet --eval "$1"
}

if [ "$RECREATE" = true ]; then
  emails=$(printf '%s\n' "${ACCOUNTS[@]}" | cut -d'|' -f1 | paste -sd',' -)
  echo "Deleting the four accounts so they can be created again."
  mongo_root '
    db.getSiblingDB("admin").auth("'"${MONGO_ROOT_USER:-kapp_root}"'", process.env.ROOT_PW);
    const emails = "'"$emails"'".split(",");
    const a = db.getSiblingDB("kapp_auth").credentials.deleteMany({ email: { $in: emails } });
    const u = db.getSiblingDB("kapp_user").users.deleteMany({ email: { $in: emails } });
    print("  removed " + a.deletedCount + " credentials and " + u.deletedCount + " profiles");
  '
  echo
fi

OUT=".dev-accounts"
: > "$OUT"
chmod 600 "$OUT"
{
  echo "# KApp development accounts, created $(date -u +%Y-%m-%dT%H:%M:%SZ)"
  echo "# Local development only. Gitignored on purpose. Rotate before KApp is reachable"
  echo "# from outside a laptop."
  echo
} >> "$OUT"

created=0
skipped=0

for entry in "${ACCOUNTS[@]}"; do
  IFS='|' read -r email first last student_code <<< "$entry"
  pw="$(password)"

  body=$(printf '{"email":"%s","password":"%s","firstName":"%s","lastName":"%s","invitationCode":"%s","studentCode":"%s","programCode":"%s"}' \
    "$email" "$pw" "$first" "$last" "$INVITATION_CODE" "$student_code" "$PROGRAM_CODE")

  response=$(curl -sS -o /dev/null -w '%{http_code}' --max-time 15 \
    -X POST "$GATEWAY/auth/register" \
    -H 'Content-Type: application/json' \
    -d "$body" || echo "000")

  case "$response" in
    201)
      printf '  created   %s\n' "$email"
      printf '%s\n  %s\n\n' "$email" "$pw" >> "$OUT"
      created=$((created + 1))
      ;;
    409)
      printf '  exists    %s   (run with --recreate to reset its password)\n' "$email"
      printf '%s\n  (already existed; password unchanged and not known to this script)\n\n' "$email" >> "$OUT"
      skipped=$((skipped + 1))
      ;;
    429)
      echo "  RATE LIMITED after $created accounts. The gateway allows 10 credential" >&2
      echo "  attempts per minute. Wait a minute and run this again." >&2
      exit 1
      ;;
    *)
      echo "  FAILED    $email   HTTP $response" >&2
      exit 1
      ;;
  esac
done

# ROLE_ADMIN is never granted by an invitation code - the codes ship in the repository, so
# a code that granted admin would let anyone who can read the repo escalate. Promotion
# happens here instead, directly against the two collections that hold the role.
if [ "$created" -gt 0 ] || [ "$RECREATE" = true ]; then
  echo
  echo "Promoting them to ROLE_ADMIN:"
  emails=$(printf '%s\n' "${ACCOUNTS[@]}" | cut -d'|' -f1 | paste -sd',' -)
  mongo_root '
    db.getSiblingDB("admin").auth("'"${MONGO_ROOT_USER:-kapp_root}"'", process.env.ROOT_PW);
    const emails = "'"$emails"'".split(",");
    const a = db.getSiblingDB("kapp_auth").credentials.updateMany(
      { email: { $in: emails } }, { $set: { roles: ["ROLE_ADMIN"] } });
    const u = db.getSiblingDB("kapp_user").users.updateMany(
      { email: { $in: emails } }, { $set: { role: "ROLE_ADMIN" } });
    print("  " + a.modifiedCount + " credentials and " + u.modifiedCount + " profiles now ROLE_ADMIN");
  '
fi

echo
echo "$created created, $skipped already existed."
echo "Passwords are in app/backend/microservices/$OUT (mode 600, gitignored)."
echo "Sign in at http://localhost:4300 or through $GATEWAY/auth/login."
if [ "$created" -gt 0 ]; then
  echo
  echo "Give each teammate their own line from that file over a private channel - not the"
  echo "group chat, and not a commit."
fi
