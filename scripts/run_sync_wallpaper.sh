#!/bin/bash
# 壁纸同步定时任务入口：加载数据库凭据，运行 sync_wallpaper.py 并记录日志。
# 供 launchd（top.pnkx.wallpaper-sync）每天 03:00 调用，也可手动执行。
set -u

DIR="$(cd "$(dirname "$0")" && pwd)"
ENV_FILE="$DIR/.wp_db.env"
LOG_DIR="$DIR/logs"
LOG_FILE="$LOG_DIR/wallpaper-sync.log"

mkdir -p "$LOG_DIR"
# 日志超过 5MB 时轮转，只保留上一份
if [ -f "$LOG_FILE" ] && [ "$(stat -f%z "$LOG_FILE")" -gt 5242880 ]; then
    mv "$LOG_FILE" "$LOG_FILE.1"
fi

rc=0
{
    echo ""
    echo "===== $(date '+%Y-%m-%d %H:%M:%S') 开始同步 ====="
    if [ ! -f "$ENV_FILE" ]; then
        echo "[错误] 缺少凭据文件: $ENV_FILE"
        exit 1
    fi
    # shellcheck disable=SC1090
    . "$ENV_FILE"
    /usr/bin/python3 "$DIR/sync_wallpaper.py" "$@"
    rc=$?
    echo "===== 结束，退出码 $rc ====="
} >> "$LOG_FILE" 2>&1

exit "$rc"
