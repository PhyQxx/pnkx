#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Flyway V1.1.1 失败记录清理脚本。

用途：V1.1.1__reminder_engine.sql 首次执行因 sys_menu INSERT 漏列失败，
在 flyway_schema_history 留下 success=0 记录。修正 SQL 后需先清理该记录，
否则 Flyway 会报 "Migration ... failed" 拒绝再次执行。

使用：
    python3 scripts/fix_flyway_v111.py
或自定义连接参数：
    python3 scripts/fix_flyway_v111.py --host 127.0.0.1 --port 3306 --user root --password 123456 --db pnkx

前提：MySQL 已启动且可连接。
"""
import argparse
import sys

try:
    import pymysql
except ImportError:
    print('缺少 pymysql，请先执行: pip3 install pymysql', file=sys.stderr)
    sys.exit(1)


def main():
    p = argparse.ArgumentParser(description='清理 Flyway V1.1.1 失败记录')
    p.add_argument('--host', default='127.0.0.1')
    p.add_argument('--port', type=int, default=3306)
    p.add_argument('--user', default='root')
    p.add_argument('--password', default='123456')
    p.add_argument('--db', default='pnkx')
    args = p.parse_args()

    try:
        conn = pymysql.connect(host=args.host, port=args.port,
                               user=args.user, password=args.password,
                               database=args.db, charset='utf8mb4')
    except Exception as e:
        print(f'❌ 连接 MySQL 失败: {e}', file=sys.stderr)
        print('   请确认 MySQL 已启动，且连接参数正确。', file=sys.stderr)
        sys.exit(2)

    cur = conn.cursor()
    try:
        # 查看失败记录
        cur.execute("SELECT installed_rank, version, description, success "
                    "FROM flyway_schema_history WHERE version='1.1.1' AND success=0")
        failed = cur.fetchall()
        if not failed:
            print('✓ 未发现 V1.1.1 的失败记录，无需清理。可直接重启后端。')
            return
        print(f'发现 {len(failed)} 条 V1.1.1 失败记录:')
        for r in failed:
            print('  ', r)

        # 删除失败记录
        cur.execute("DELETE FROM flyway_schema_history WHERE version='1.1.1' AND success=0")
        conn.commit()
        print(f'✓ 已删除 {cur.rowcount} 条失败记录。可重启后端，Flyway 将重新执行已修正的迁移。')
    finally:
        cur.close()
        conn.close()


if __name__ == '__main__':
    main()
