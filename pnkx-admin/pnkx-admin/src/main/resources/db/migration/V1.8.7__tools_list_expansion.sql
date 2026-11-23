-- ============================================================
-- V1.8.7 工具箱扩展：sys.tools.list 增加新工具
--   新增：房贷计算器 / 个税计算器 / 今天吃什么（pages_tools 分包）
--   结构升级：增加 group（分组）与 isNew（角标）字段，旧数据向后兼容
--   壁纸跳转路径迁移：pages_life/wallpaper/* → pages_wallpaper/wallpaper/*
--   注意：sys_config 有 Redis 缓存（sys_config:{key}），后端重启时
--   SysConfigServiceImpl#init 会全量刷新缓存，无需手动清理
-- ============================================================

UPDATE `sys_config`
SET `config_value` = '[
  {"id":"mortgage","name":"房贷计算器","icon":"wallet","color":"#6C9EFF","desc":"商贷 / 公积金，等额本息、本金对比","type":"page","target":"/pages_tools/mortgage/index","enabled":true,"group":"finance","isNew":true},
  {"id":"tax","name":"个税计算器","icon":"compose","color":"#FBBF24","desc":"年度累计预扣，年终奖单独计税","type":"page","target":"/pages_tools/tax/index","enabled":true,"group":"finance","isNew":true},
  {"id":"what-to-eat","name":"今天吃什么","icon":"shop","color":"#F472B6","desc":"选择困难症救星，转一转决定","type":"page","target":"/pages_tools/what-to-eat/index","enabled":true,"group":"fun","isNew":true},
  {"id":"wallpaper","name":"壁纸下载","icon":"picture","color":"#38BDF8","desc":"精选高清壁纸，一键保存到相册","type":"page","target":"/pages_wallpaper/wallpaper/index","enabled":true,"group":"life"},
  {"id":"blog","name":"博客后台","icon":"article","color":"#60A5FA","desc":"管理文章、照片与博客内容","type":"web","target":"https://pnkx.top","enabled":true}
]',
    `remark` = '工具台首页展示的工具列表（JSON 数组），v2 增加 group/isNew 字段'
WHERE `config_key` = 'sys.tools.list';
