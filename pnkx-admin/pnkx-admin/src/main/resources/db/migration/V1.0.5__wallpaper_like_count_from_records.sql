-- V1.0.5 壁纸点赞数纠偏
-- 说明：
--   旧版客户端展示 px_wallpaper.like_count，历史数据可能与 px_like_record 中
--   当前有效的壁纸点赞记录不一致。这里把冗余字段统一重算为 type=2 的记录数，
--   使未更新到新版 mapper 的客户端也不会继续展示历史虚高点赞数。

UPDATE px_wallpaper w
LEFT JOIN (
    SELECT r.item_id, COUNT(1) AS cnt
    FROM px_like_record r
    WHERE r.type = '2'
    GROUP BY r.item_id
) lr ON lr.item_id = w.id
SET w.like_count = COALESCE(lr.cnt, 0);
