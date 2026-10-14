#!/usr/bin/env bash
# Run against a disposable MySQL container. No existing database/config is read.
set -euo pipefail
repo_dir="$(cd "$(dirname "$0")/.." && pwd)"
container_id=""
cleanup() {
  if [[ -n "$container_id" ]]; then docker rm -f "$container_id" >/dev/null 2>&1 || true; fi
}
trap cleanup EXIT INT TERM
container_id="$(docker run -d --rm -e MYSQL_ALLOW_EMPTY_PASSWORD=yes -p 127.0.0.1::3306 mysql:8)"
ready=false
for ((i=0; i<90; i++)); do
  if docker exec "$container_id" mysqladmin --protocol=tcp -h127.0.0.1 -uroot ping --silent >/dev/null 2>&1; then
    ready=true
    break
  fi
  sleep 1
done
if [[ "$ready" != true ]]; then
  docker logs "$container_id" >&2
  exit 1
fi
mysql_port="$(docker port "$container_id" 3306/tcp | sed 's/.*://')"
cd "$repo_dir/pnkx-admin"
mvn -pl pnkx-life -am -Dtest=NoteIsolationIntegrationTest,ResourceSearchIntegrationTest \
  -Dsurefire.failIfNoSpecifiedTests=false \
  "-Dnote.test.mysqlUrl=jdbc:mysql://127.0.0.1:${mysql_port}/" test
