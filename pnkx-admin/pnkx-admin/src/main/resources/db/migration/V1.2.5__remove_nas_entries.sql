-- 移除所有群晖 NAS 快捷入口。
-- 页面组件已经从 pnkx-ui 删除；这里禁用历史菜单以及挂在其下方的按钮权限，
-- 防止动态路由继续加载不存在的 Vue 组件。

UPDATE `sys_menu`
SET `visible` = '1',
    `status` = '1',
    `remark` = CONCAT(COALESCE(`remark`, ''), ' [NAS入口已移除]')
WHERE `component` IN (
    'px/nas',
    'px/life/nas/drive',
    'px/life/nas/mail',
    'px/life/album/index',
    'px/life/note/new',
    'px/life/diary/new'
)
   OR `menu_name` IN ('NAS', 'NAS管理', '群晖NAS', '群晖相册', '群晖笔记', '群晖日历');

UPDATE `sys_menu` child
JOIN `sys_menu` parent ON child.`parent_id` = parent.`menu_id`
SET child.`visible` = '1', child.`status` = '1'
WHERE parent.`component` IN (
    'px/nas',
    'px/life/nas/drive',
    'px/life/nas/mail',
    'px/life/album/index',
    'px/life/note/new',
    'px/life/diary/new'
)
   OR parent.`menu_name` IN ('NAS', 'NAS管理', '群晖NAS', '群晖相册', '群晖笔记', '群晖日历');
