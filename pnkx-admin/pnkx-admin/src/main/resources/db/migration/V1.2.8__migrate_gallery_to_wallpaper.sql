-- ============================================================
-- 旧图库（sys_file.type = 'tk'）迁移至壁纸管理
--
-- 目标：
--   1. 创建根文件夹「Pei你看雪」（已存在时复用）；
--   2. 将旧图库图片全部迁入该文件夹；
--   3. 按 URL 幂等去重，重复执行不会产生重复壁纸；
--   4. 保留 sys_file 原记录作为迁移审计和回滚来源。
--
-- 线上迁移前盘点：旧图库 497 条、URL 唯一 497 条，且与现有
-- px_wallpaper URL 无重合，因此首次执行预计新增 497 条壁纸。
-- ============================================================

INSERT INTO `px_wallpaper_folder`
    (`name`, `cover`, `parent_id`, `order`, `enabled`, `del_flag`, `create_by`, `create_time`, `remark`)
SELECT 'Pei你看雪',
       (SELECT COALESCE(NULLIF(sf.`thumbnail`, ''), sf.`url`)
        FROM `sys_file` sf
        WHERE sf.`type` = 'tk'
          AND sf.`url` IS NOT NULL
          AND TRIM(sf.`url`) <> ''
        ORDER BY sf.`id`
        LIMIT 1),
       0, '0', 1, 0, 'migration', NOW(), '由旧图库迁移创建'
WHERE NOT EXISTS (
    SELECT 1
    FROM `px_wallpaper_folder`
    WHERE `name` = 'Pei你看雪'
      AND `parent_id` = 0
      AND `del_flag` = 0
);

INSERT INTO `px_wallpaper`
    (`name`, `url`, `thumbnail`, `folder`, `like_count`, `del_flag`, `version`,
     `create_by`, `create_time`, `update_by`, `update_time`, `remark`)
SELECT LEFT(sf.`name`, 100),
       sf.`url`,
       NULLIF(sf.`thumbnail`, ''),
       target_folder.`id`,
       0,
       0,
       sf.`version`,
       COALESCE(NULLIF(sf.`create_by`, ''), 'migration'),
       COALESCE(sf.`create_time`, NOW()),
       sf.`update_by`,
       COALESCE(sf.`update_time`, sf.`create_time`, NOW()),
       LEFT(CONCAT_WS('；',
           NULLIF(sf.`remark`, ''),
           CONCAT('旧图库迁移，原sys_file.id=', sf.`id`),
           CONCAT('历史浏览=', COALESCE(sf.`browse`, 0)),
           CONCAT('历史点赞=', COALESCE(sf.`thumb`, 0))
       ), 500)
FROM `sys_file` sf
CROSS JOIN (
    SELECT `id`
    FROM `px_wallpaper_folder`
    WHERE `name` = 'Pei你看雪'
      AND `parent_id` = 0
      AND `del_flag` = 0
    ORDER BY `id`
    LIMIT 1
) target_folder
WHERE sf.`type` = 'tk'
  AND sf.`url` IS NOT NULL
  AND TRIM(sf.`url`) <> ''
  AND NOT EXISTS (
      SELECT 1
      FROM `px_wallpaper` wallpaper
      WHERE wallpaper.`del_flag` = 0
        AND wallpaper.`url` = sf.`url`
  );
