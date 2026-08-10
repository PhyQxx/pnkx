#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
壁纸数据同步器：对比「本地文件夹」与「数据库现状」，生成增量并导入。

混合智能层级：
  - 系列有子文件夹 → 三层（根文件夹 → 子文件夹 → 壁纸）
    子文件夹里的图归各子文件夹；根目录散落的图归「默认」子文件夹（如有）
  - 系列图片全在根目录 → 两层（根文件夹 → 壁纸）

用法：
  python3 sync_wallpaper.py              # 检测+导入所有系列
  python3 sync_wallpaper.py --dry-run    # 仅检测，不导入
  python3 sync_wallpaper.py --no-import  # 仅生成 SQL，不导入
  python3 sync_wallpaper.py 火影 海贼王   # 只处理指定系列（关键词匹配）
"""
import os
import sys
import random
import pymysql

# ---- 系列配置 ----
SERIES = [
    {"key": "fire",       "name": "火影插画",       "base": "/Users/peihaoyu/Pictures/裴浩宇/火影插画",
     "url_prefix": "https://ftp.pnkx.top:8/ftp/我的图片/火影插画",       "parent_name": "火影忍者"},
    {"key": "onepiece",   "name": "海贼王插画",     "base": "/Users/peihaoyu/Pictures/裴浩宇/海贼王插画",
     "url_prefix": "https://ftp.pnkx.top:8/ftp/我的图片/海贼王插画",     "parent_name": "海贼王"},
    {"key": "lol",        "name": "英雄联盟",       "base": "/Users/peihaoyu/Pictures/裴浩宇/英雄联盟插画",
     "url_prefix": "https://ftp.pnkx.top:8/ftp/我的图片/英雄联盟插画",   "parent_name": "英雄联盟"},
    {"key": "qin",        "name": "秦时明月",       "base": "/Users/peihaoyu/Pictures/裴浩宇/秦时明月",
     "url_prefix": "https://ftp.pnkx.top:8/ftp/我的图片/秦时明月",       "parent_name": "秦时明月"},
    {"key": "yuan",       "name": "缘之空",         "base": "/Users/peihaoyu/Pictures/裴浩宇/缘之空",
     "url_prefix": "https://ftp.pnkx.top:8/ftp/我的图片/缘之空",         "parent_name": "缘之空"},
    {"key": "shen",       "name": "神雕侠侣",       "base": "/Users/peihaoyu/Pictures/裴浩宇/神雕侠侣插画",
     "url_prefix": "https://ftp.pnkx.top:8/ftp/我的图片/神雕侠侣插画",   "parent_name": "神雕侠侣"},
    {"key": "dnf",        "name": "地下城与勇士",   "base": "/Users/peihaoyu/Pictures/裴浩宇/地下城与勇士插画",
     "url_prefix": "https://ftp.pnkx.top:8/ftp/我的图片/地下城与勇士插画", "parent_name": "地下城与勇士"},
    {"key": "dpcq",       "name": "斗破苍穹",       "base": "/Users/peihaoyu/Pictures/裴浩宇/斗破苍穹插画",
     "url_prefix": "https://ftp.pnkx.top:8/ftp/我的图片/斗破苍穹插画",   "parent_name": "斗破苍穹"},
    {"key": "qnv",        "name": "倩女幽魂",       "base": "/Users/peihaoyu/Pictures/裴浩宇/倩女幽魂",
     "url_prefix": "https://ftp.pnkx.top:8/ftp/我的图片/倩女幽魂",       "parent_name": "倩女幽魂"},
    {"key": "tlb",        "name": "天龙八部",       "base": "/Users/peihaoyu/Pictures/裴浩宇/天龙八部插画",
     "url_prefix": "https://ftp.pnkx.top:8/ftp/我的图片/天龙八部插画",   "parent_name": "天龙八部"},
    {"key": "sg",         "name": "三国",           "base": "/Users/peihaoyu/Pictures/裴浩宇/三国插画",
     "url_prefix": "https://ftp.pnkx.top:8/ftp/我的图片/三国插画",       "parent_name": "三国"},
    {"key": "dl1",        "name": "斗罗大陆",       "base": "/Users/peihaoyu/Pictures/裴浩宇/斗罗大陆",
     "url_prefix": "https://ftp.pnkx.top:8/ftp/我的图片/斗罗大陆",       "parent_name": "斗罗大陆"},
    {"key": "dl2",        "name": "斗罗大陆-绝世唐门", "base": "/Users/peihaoyu/Pictures/裴浩宇/斗罗大陆-绝世唐门",
     "url_prefix": "https://ftp.pnkx.top:8/ftp/我的图片/斗罗大陆-绝世唐门", "parent_name": "斗罗大陆·绝世唐门"},
    {"key": "ldj",        "name": "鹿鼎记",         "base": "/Users/peihaoyu/Pictures/裴浩宇/鹿鼎记插画",
     "url_prefix": "https://ftp.pnkx.top:8/ftp/我的图片/鹿鼎记插画",     "parent_name": "鹿鼎记"},
    {"key": "ashtly",     "name": "爱上她的理由",   "base": "/Users/peihaoyu/Pictures/裴浩宇/爱上她的理由",
     "url_prefix": "https://ftp.pnkx.top:8/ftp/我的图片/爱上她的理由",   "parent_name": "爱上她的理由"},
]
EXCLUDE = {"NSFW"}
IMG_EXT = (".png", ".jpg", ".jpeg", ".webp")
LIKE_MIN, LIKE_MAX = 100, 300

# ---- DB ----
DB = dict(host="mysql.pnkx.top", port=13306, user="root",
          password="123456", database="pnkx", charset="utf8mb4",
          autocommit=True)


def esc(s):
    return s.replace("\\", "\\\\").replace("'", "''")


def clean_name(fname):
    """文件名 -> 壁纸名：去扩展名"""
    return os.path.splitext(fname)[0]


def thumb_url(url):
    return url.replace("/ftp/", "/ftp/thumbnail/", 1)


def list_images(path):
    return sorted(f for f in os.listdir(path)
                  if f.lower().endswith(IMG_EXT))


def scan_series(series):
    """
    扫描系列本地目录，返回 (root_imgs, subfolders)
      root_imgs: [img, ...]  根目录直接放的图片
      subfolders: [(folder_name, [img,...]), ...]  子文件夹
    """
    base = series["base"]
    if not os.path.isdir(base):
        return None, None
    root_imgs = list_images(base)
    subfolders = []
    for d in sorted(os.listdir(base)):
        full = os.path.join(base, d)
        if not os.path.isdir(full) or d in EXCLUDE:
            continue
        imgs = list_images(full)
        if imgs:
            subfolders.append((d, imgs))
    return root_imgs, subfolders


def db_connect():
    return pymysql.connect(**DB)


def get_or_create_root(conn, name):
    """获取或创建顶层根文件夹(parent_id=0)，返回 id"""
    cur = conn.cursor()
    cur.execute("SELECT id FROM px_wallpaper_folder WHERE name=%s AND parent_id=0", (name,))
    r = cur.fetchone()
    if r:
        return r[0]
    cur.execute("SELECT `order` FROM px_wallpaper_folder WHERE parent_id=0")
    orders = []
    for ro in cur.fetchall():
        try:
            orders.append(int(ro[0]))
        except (ValueError, TypeError):
            pass
    next_order = (max(orders) + 1) if orders else 0
    cur.execute(
        "INSERT INTO px_wallpaper_folder(name, cover, parent_id, `order`, del_flag, "
        "create_by, create_time, update_by, update_time) "
        "VALUES (%s, NULL, 0, %s, 0, '1', NOW(), '1', NOW())",
        (name, str(next_order)))
    return cur.lastrowid


def fetch_db_state(conn, parent_id):
    """返回 (folder_names:set, wallpaper_urls:set)"""
    cur = conn.cursor()
    cur.execute("SELECT name FROM px_wallpaper_folder WHERE parent_id=%s", (parent_id,))
    folder_names = {r[0] for r in cur.fetchall()}
    cur.execute("SELECT url FROM px_wallpaper")
    urls = {r[0] for r in cur.fetchall()}
    return folder_names, urls


def next_order_for(conn, parent_id):
    cur = conn.cursor()
    cur.execute("SELECT `order` FROM px_wallpaper_folder WHERE parent_id=%s", (parent_id,))
    orders = []
    for r in cur.fetchall():
        try:
            orders.append(int(r[0]))
        except (ValueError, TypeError):
            pass
    return (max(orders) + 1) if orders else 0


def gen_folder_sql(name, cover, parent_id, order):
    return (
        "INSERT INTO `pnkx`.`px_wallpaper_folder`"
        "(`name`, `cover`, `parent_id`, `order`, `del_flag`, "
        "`create_by`, `create_time`, `update_by`, `update_time`)"
        " SELECT '%s', '%s', %d, '%d', 0, '1', NOW(), '1', NOW()"
        " WHERE NOT EXISTS ("
        "SELECT 1 FROM `pnkx`.`px_wallpaper_folder` "
        "WHERE `name` = '%s' AND `parent_id` = %d);"
        % (esc(name), esc(cover), parent_id, order, esc(name), parent_id))


def gen_wallpaper_sql(name, url, thumbnail, like_count, folder_name, parent_id, order):
    folder_sub = ("(SELECT `id` FROM `pnkx`.`px_wallpaper_folder` "
                  "WHERE `name` = '%s' AND `parent_id` = %d LIMIT 1)"
                  % (esc(folder_name), parent_id))
    return (
        "INSERT INTO `pnkx`.`px_wallpaper`"
        "(`name`, `url`, `thumbnail`, `folder`, `like_count`, `width`, `height`, "
        "`order`, `del_flag`, `version`, `create_by`, `create_time`, "
        "`update_by`, `update_time`, `remark`)"
        " SELECT '%s', '%s', '%s', %s, %d, NULL, NULL, '%d', 0, NULL, '1', NOW(), '1', NOW(), NULL"
        " WHERE NOT EXISTS ("
        "SELECT 1 FROM `pnkx`.`px_wallpaper` WHERE `url` = '%s');"
        % (esc(name), esc(url), esc(thumbnail), folder_sub, like_count, order, esc(url)))


def gen_wallpaper_sql_by_id(name, url, thumbnail, like_count, folder_id, order):
    """folder 字段直接用 id（用于两层结构，壁纸直接挂根文件夹）"""
    return (
        "INSERT INTO `pnkx`.`px_wallpaper`"
        "(`name`, `url`, `thumbnail`, `folder`, `like_count`, `width`, `height`, "
        "`order`, `del_flag`, `version`, `create_by`, `create_time`, "
        "`update_by`, `update_time`, `remark`)"
        " SELECT '%s', '%s', '%s', %d, %d, NULL, NULL, '%d', 0, NULL, '1', NOW(), '1', NOW(), NULL"
        " WHERE NOT EXISTS ("
        "SELECT 1 FROM `pnkx`.`px_wallpaper` WHERE `url` = '%s');"
        % (esc(name), esc(url), esc(thumbnail), folder_id, like_count, order, esc(url)))


def exec_stmts(conn, stmts):
    cur = conn.cursor()
    folder_ok = wall_ok = fails = 0
    for sql in stmts:
        is_folder = sql.startswith("INSERT INTO `pnkx`.`px_wallpaper_folder`")
        try:
            cur.execute(sql)
            affected = cur.rowcount
            if is_folder:
                folder_ok += affected
            else:
                wall_ok += affected
        except Exception as e:
            fails += 1
            print("    [失败] %s" % e)
    return folder_ok, wall_ok, fails


def process_series(series, conn, do_import):
    print("\n=== %s ===" % series["name"])
    root_imgs, subfolders = scan_series(series)
    if root_imgs is None:
        print("  [跳过] 本地目录不存在: %s" % series["base"])
        return 0, 0

    has_subs = len(subfolders) > 0
    total_local = len(root_imgs) + sum(len(i) for _, i in subfolders)
    if total_local == 0:
        print("  [跳过] 无图片")
        return 0, 0

    root_id = get_or_create_root(conn, series["parent_name"])

    if has_subs:
        print("  结构: 三层（%d 个子文件夹 + %d 张根目录散图）"
              % (len(subfolders), len(root_imgs)))
        db_folders, db_urls = fetch_db_state(conn, root_id)
        stmts = []
        order = next_order_for(conn, root_id)

        default_folder = "默认"
        if root_imgs:
            if default_folder not in db_folders:
                cover = "%s/%s" % (series["url_prefix"], root_imgs[0])
                stmts.append(gen_folder_sql(default_folder, cover, root_id, order))
                order += 1
            for idx, img in enumerate(root_imgs):
                url = "%s/%s" % (series["url_prefix"], img)
                if url not in db_urls:
                    like = random.randint(LIKE_MIN, LIKE_MAX)
                    stmts.append(gen_wallpaper_sql(clean_name(img), url, thumb_url(url),
                                                   like, default_folder, root_id, idx))
        for fname, imgs in subfolders:
            if fname not in db_folders:
                cover = "%s/%s/%s" % (series["url_prefix"], fname, imgs[0])
                stmts.append(gen_folder_sql(fname, cover, root_id, order))
                order += 1
            for idx, img in enumerate(imgs):
                url = "%s/%s/%s" % (series["url_prefix"], fname, img)
                if url not in db_urls:
                    like = random.randint(LIKE_MIN, LIKE_MAX)
                    stmts.append(gen_wallpaper_sql(clean_name(img), url, thumb_url(url),
                                                   like, fname, root_id, idx))
    else:
        print("  结构: 两层（%d 张图直接挂根文件夹）" % len(root_imgs))
        _, db_urls = fetch_db_state(conn, root_id)
        stmts = []
        for idx, img in enumerate(root_imgs):
            url = "%s/%s" % (series["url_prefix"], img)
            if url not in db_urls:
                like = random.randint(LIKE_MIN, LIKE_MAX)
                stmts.append(gen_wallpaper_sql_by_id(clean_name(img), url, thumb_url(url),
                                                     like, root_id, idx))

    n_folder = sum(1 for s in stmts if s.startswith("INSERT INTO `pnkx`.`px_wallpaper_folder`"))
    n_wall = len(stmts) - n_folder
    print("  差异: 新增文件夹 %d, 新增壁纸 %d" % (n_folder, n_wall))

    if n_folder == 0 and n_wall == 0:
        print("  数据库已是最新。")
        return 0, 0

    if do_import:
        fok, wok, fails = exec_stmts(conn, stmts)
        print("  导入: 文件夹 +%d, 壁纸 +%d, 失败 %d" % (fok, wok, fails))
        return fok, wok
    else:
        out_file = "diff_%s.sql" % series["key"]
        with open(out_file, "w", encoding="utf-8") as f:
            f.write("\n".join(stmts))
        print("  SQL 已写入: %s" % out_file)
        return n_folder, n_wall


def main():
    args = [a for a in sys.argv[1:] if not a.startswith("-")]
    dry_run = "--dry-run" in sys.argv
    no_import = "--no-import" in sys.argv
    do_import = not (dry_run or no_import)

    series_list = SERIES
    if args:
        series_list = [s for s in SERIES
                       if any(k in s["name"] or k in s["parent_name"] for k in args)]
        if not series_list:
            sys.exit("没有匹配的系列，可用关键词: " +
                     ", ".join(s["name"] for s in SERIES))

    mode = "dry-run（仅检测）" if dry_run else ("仅生成 SQL" if no_import else "检测 + 导入")
    print("模式: %s" % mode)
    print("系列: %s" % ", ".join(s["name"] for s in series_list))

    conn = db_connect()
    try:
        total_f = total_w = 0
        for series in series_list:
            fok, wok = process_series(series, conn, do_import)
            total_f += fok
            total_w += wok
        print("\n=== 汇总 ===")
        print("  新增文件夹: %d, 新增壁纸: %d" % (total_f, total_w))
    finally:
        conn.close()


if __name__ == "__main__":
    main()
