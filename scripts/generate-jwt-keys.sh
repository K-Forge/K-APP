#!/usr/bin/env bash
# Gives the local stack a signing key that survives a restart.
#
# Without KAPP_JWT_PRIVATE_KEY / KAPP_JWT_PUBLIC_KEY, auth-service generates an RSA pair at
# startup and warns that it is doing so. That is fine in principle and miserable in practice:
# every `docker compose up -d auth-service` invalidates every token already issued, so the admin
# portal answers 401 until you sign in again, and so does anything a teammate had open. Now that
# the databases are shared and the stack gets restarted for every backend change, that is a
# restart tax nobody should pay.
#
# The pair this writes is a DEVELOPMENT key. It lands in .env, which is gitignored, and it must
# never be the key that signs a token anybody outside this laptop would trust.
set -euo pipefail

cd "$(dirname "$0")/../app/backend/microservices"

if [ ! -f .env ]; then
  echo "No .env here. Generate one first: ../../../scripts/generate-dev-secrets.sh > .env" >&2
  exit 1
fi

if grep -q '^KAPP_JWT_PRIVATE_KEY=..' .env; then
  echo ".env already carries a signing key. Delete both KAPP_JWT_* lines to replace it."
  echo "Replacing it invalidates every token already issued, so everyone signs in again."
  exit 0
fi

tmp=$(mktemp -d)
trap 'rm -rf "$tmp"' EXIT

openssl genpkey -algorithm RSA -pkeyopt rsa_keygen_bits:2048 -out "$tmp/private.pem" 2>/dev/null
openssl rsa -in "$tmp/private.pem" -pubout -out "$tmp/public.pem" 2>/dev/null

# auth-service expects base64 of the whole PEM, newlines and armour included - RsaKeyProvider
# base64-decodes the value and then strips the BEGIN/END lines. Single-quoted in .env for the
# same reason every other value there is: an unquoted value with punctuation in it is a shell
# hazard, and `. .env` silently produced an empty variable the last time one was not quoted.
{
  echo
  echo "# RSA signing key for JWTs. Written by scripts/generate-jwt-keys.sh."
  echo "# Development only. Without these, auth-service generates a new pair on every start"
  echo "# and every token issued before the restart stops being accepted."
  echo "KAPP_JWT_PRIVATE_KEY='$(base64 < "$tmp/private.pem" | tr -d '\n')'"
  echo "KAPP_JWT_PUBLIC_KEY='$(base64 < "$tmp/public.pem" | tr -d '\n')'"
} >> .env

echo "Wrote KAPP_JWT_PRIVATE_KEY and KAPP_JWT_PUBLIC_KEY to app/backend/microservices/.env"
echo "Restart auth-service once more for it to pick them up; after that, restarts stop"
echo "logging everybody out."
