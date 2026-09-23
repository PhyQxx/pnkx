-- V1.0.6 壁纸点赞记录类型统一为 type=3
-- 说明：
--   壁纸点赞统一使用 px_like_record.type = '3'。
--   历史 px_like_record.type = '2' 迁移为 type = '3'，随后重算 px_wallpaper.like_count。

DELETE r2
FROM px_like_record r2
         INNER JOIN px_like_record r3
                    ON r3.item_id = r2.item_id
                        AND r3.create_by = r2.create_by
                        AND r3.type = '3'
WHERE r2.type = '2';

UPDATE px_like_record
SET type = '3',
    update_time = NOW()
WHERE type = '2';

UPDATE px_wallpaper w
LEFT JOIN (
    SELECT r.item_id, COUNT(1) AS cnt
    FROM px_like_record r
    WHERE r.type = '3'
    GROUP BY r.item_id
) lr ON lr.item_id = w.id
SET w.like_count = COALESCE(lr.cnt, 0);
