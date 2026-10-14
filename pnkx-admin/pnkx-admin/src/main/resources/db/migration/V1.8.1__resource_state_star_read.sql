-- ============================================================
-- V1.8.1  状态收一（§3.1）：剪藏 starFlag/readFlag 并入 px_resource_state
-- ============================================================
-- px_resource_state 已承载 inbox；补 starred / opened 两列后，
-- 剪藏收藏与已读不再写 px_web_article 旧列（旧列保留观察一版后删除）。
-- 壁纸"我的喜欢"已于 V1.4.5 收一进 px_like_record，无需迁移。
-- ============================================================

ALTER TABLE px_resource_state
  ADD COLUMN starred TINYINT NOT NULL DEFAULT 0,
  ADD COLUMN opened  TINYINT NOT NULL DEFAULT 0;

-- 存量回填（当前旧列为空，语句保持幂等）
INSERT IGNORE INTO px_resource_state(user_id, resource_type, resource_id, starred, opened)
SELECT create_by, 'webArticle', id,
       IF(star_flag = '1', 1, 0),
       IF(read_flag  = '1', 1, 0)
FROM px_web_article
WHERE del_flag = '0' AND (star_flag = '1' OR read_flag = '1');
