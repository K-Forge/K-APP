#!/usr/bin/env bash
# Rebuilds one or more compose services and restarts their containers.
#
# This exists because of a specific way to waste twenty minutes: `docker compose build X` and
# `docker compose up -d X` are two commands, and if the second one fails - a mistyped service
# name is enough, and the names here are `semaphore-service`, not `semaphore` - the build
# still succeeded, so there is no error on screen and the container keeps serving the OLD
# image. The fix you just made appears not to work, and you go looking for it in the code.
#
# So: fail on the first error, name the services exactly, and print what is actually being
# served at the end.
set -euo pipefail

cd "$(dirname "$0")/../app/backend/microservices"

if [ $# -eq 0 ]; then
  echo "usage: scripts/redeploy.sh <service> [service...]" >&2
  echo >&2
  echo "services:" >&2
  grep -E '^  [a-z][a-z-]*:' docker-compose.yml | tr -d ' :' | sed 's/^/  /' >&2
  exit 2
fi

PROFILES="--profile cloud --profile dev"

echo "building: $*"
docker compose $PROFILES build "$@"

echo "restarting: $*"
docker compose $PROFILES up -d "$@"

for service in "$@"; do
  container=$(docker compose $PROFILES ps -q "$service" 2>/dev/null | head -1)
  [ -z "$container" ] && { echo "  $service: no container?" >&2; continue; }
  name=$(docker inspect -f '{{.Name}}' "$container" | tr -d '/')

  # Wait only if the service actually declares a healthcheck; otherwise this loops forever.
  if [ "$(docker inspect -f '{{if .State.Health}}yes{{end}}' "$container")" = "yes" ]; then
    printf '  %s: waiting' "$name"
    for _ in $(seq 1 60); do
      status=$(docker inspect -f '{{.State.Health.Status}}' "$container")
      [ "$status" = "healthy" ] && break
      printf '.'
      sleep 3
    done
    echo " $(docker inspect -f '{{.State.Health.Status}}' "$container")"
  else
    echo "  $name: $(docker inspect -f '{{.State.Status}}' "$container") (no healthcheck)"
  fi

  echo "    image built $(docker inspect -f '{{.Created}}' "$(docker inspect -f '{{.Image}}' "$container")" | cut -c1-19)"
done
