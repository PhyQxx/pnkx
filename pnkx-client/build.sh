#!/bin/bash
set -euo pipefail
cd "$(dirname "$0")"
CONTAINER_NAME="pnkx-client"
BACKUP_NAME="${CONTAINER_NAME}-backup"

# Build before touching the running service.
docker build -t pnkx-client .
if docker container inspect "$BACKUP_NAME" >/dev/null 2>&1; then
  echo "备份容器已存在，请先检查上次部署状态：$BACKUP_NAME" >&2
  exit 1
fi
had_previous=false
was_running=false
if docker container inspect "$CONTAINER_NAME" >/dev/null 2>&1; then
  was_running=$(docker inspect --format '{{.State.Running}}' "$CONTAINER_NAME")
  docker rename "$CONTAINER_NAME" "$BACKUP_NAME"
  had_previous=true
fi
rollback() {
  trap - ERR
  docker rm -f "$CONTAINER_NAME" >/dev/null 2>&1 || true
  if [ "$had_previous" = true ]; then
    docker rename "$BACKUP_NAME" "$CONTAINER_NAME"
    if [ "$was_running" = true ]; then docker start "$CONTAINER_NAME"; fi
  fi
  echo "部署失败，已尝试恢复原容器" >&2
  exit 1
}
trap rollback ERR
if [ "$was_running" = true ]; then docker stop "$BACKUP_NAME"; fi
docker run --name "$CONTAINER_NAME" --restart unless-stopped -d -p 3001:3000 pnkx-client
healthy=false
for attempt in {1..30}; do
  if docker exec "$CONTAINER_NAME" node -e 'fetch("http://127.0.0.1:3000/api/health", {signal: AbortSignal.timeout(2000)}).then(r => process.exit(r.ok ? 0 : 1)).catch(() => process.exit(1))' >/dev/null 2>&1; then
    healthy=true
    break
  fi
  sleep 2
done
[ "$healthy" = true ] || rollback
trap - ERR
if [ "$had_previous" = true ]; then docker rm "$BACKUP_NAME"; fi
echo "部署成功，健康检查通过"
