-- ============================================================
-- V1.8.9 工具箱扩展 M3：日期计算器
--   1. sys_config 新增 sys.holiday.data（法定节假日 + 调休补班表，工作日口径依赖）
--      ⚠️ 2026 年放假安排为示例数据（锚定春节 2/17、端午 6/19、中秋 9/25 等农历锚点），
--      请以国务院办公厅当年公告为准，在后台「参数设置」中修正——每年更新数据无需发版
--   2. sys.tools.list 增加日期计算器（共 8 个工具）
-- ============================================================

INSERT INTO `sys_config`(`config_name`, `config_key`, `config_value`, `config_type`, `create_by`, `create_time`, `remark`)
SELECT '法定节假日与调休安排', 'sys.holiday.data',
       '{
  "2026": [
    {"date":"2026-01-01","type":"holiday","name":"元旦"},
    {"date":"2026-01-02","type":"holiday","name":"元旦"},
    {"date":"2026-01-03","type":"holiday","name":"元旦"},
    {"date":"2026-02-14","type":"workday","name":"春节调休补班"},
    {"date":"2026-02-16","type":"holiday","name":"春节"},
    {"date":"2026-02-17","type":"holiday","name":"春节"},
    {"date":"2026-02-18","type":"holiday","name":"春节"},
    {"date":"2026-02-19","type":"holiday","name":"春节"},
    {"date":"2026-02-20","type":"holiday","name":"春节"},
    {"date":"2026-02-21","type":"holiday","name":"春节"},
    {"date":"2026-02-22","type":"holiday","name":"春节"},
    {"date":"2026-02-28","type":"workday","name":"春节调休补班"},
    {"date":"2026-04-04","type":"holiday","name":"清明节"},
    {"date":"2026-04-05","type":"holiday","name":"清明节"},
    {"date":"2026-04-06","type":"holiday","name":"清明节"},
    {"date":"2026-04-26","type":"workday","name":"劳动节调休补班"},
    {"date":"2026-05-01","type":"holiday","name":"劳动节"},
    {"date":"2026-05-02","type":"holiday","name":"劳动节"},
    {"date":"2026-05-03","type":"holiday","name":"劳动节"},
    {"date":"2026-05-04","type":"holiday","name":"劳动节"},
    {"date":"2026-05-05","type":"holiday","name":"劳动节"},
    {"date":"2026-05-09","type":"workday","name":"劳动节调休补班"},
    {"date":"2026-06-19","type":"holiday","name":"端午节"},
    {"date":"2026-06-20","type":"holiday","name":"端午节"},
    {"date":"2026-06-21","type":"holiday","name":"端午节"},
    {"date":"2026-09-25","type":"holiday","name":"中秋节"},
    {"date":"2026-09-26","type":"holiday","name":"中秋节"},
    {"date":"2026-09-27","type":"holiday","name":"中秋节"},
    {"date":"2026-10-01","type":"holiday","name":"国庆节"},
    {"date":"2026-10-02","type":"holiday","name":"国庆节"},
    {"date":"2026-10-03","type":"holiday","name":"国庆节"},
    {"date":"2026-10-04","type":"holiday","name":"国庆节"},
    {"date":"2026-10-05","type":"holiday","name":"国庆节"},
    {"date":"2026-10-06","type":"holiday","name":"国庆节"},
    {"date":"2026-10-07","type":"holiday","name":"国庆节"},
    {"date":"2026-10-08","type":"holiday","name":"国庆节"},
    {"date":"2026-10-10","type":"workday","name":"国庆节调休补班"}
  ]
}',
       'miniprogram', 'admin', NOW(), '日期计算器工作日口径：holiday=法定假日休，workday=调休补班。示例数据，每年按国务院公告在后台更新'
WHERE NOT EXISTS (SELECT 1 FROM `sys_config` WHERE `config_key` = 'sys.holiday.data');

UPDATE `sys_config`
SET `config_value` = '[
  {"id":"mortgage","name":"房贷计算器","icon":"wallet","color":"#6C9EFF","desc":"商贷 / 公积金，等额本息、本金对比","type":"page","target":"/pages_tools/mortgage/index","enabled":true,"group":"finance","isNew":true},
  {"id":"tax","name":"个税计算器","icon":"compose","color":"#FBBF24","desc":"年度累计预扣，年终奖单独计税","type":"page","target":"/pages_tools/tax/index","enabled":true,"group":"finance","isNew":true},
  {"id":"exchange","name":"汇率换算","icon":"flag","color":"#34D399","desc":"实时汇率，配合记账外币场景","type":"page","target":"/pages_tools/exchange/index","enabled":true,"group":"finance","isNew":true},
  {"id":"date-calc","name":"日期计算器","icon":"calendar","color":"#FB923C","desc":"日期间隔、工作日、农历查询","type":"page","target":"/pages_tools/date-calc/index","enabled":true,"group":"life","isNew":true},
  {"id":"ai-pickup","name":"AI 情话生成","icon":"heart","color":"#A78BFA","desc":"土味情话、藏头诗、三行情书","type":"page","target":"/pages_tools/ai-pickup/index","enabled":true,"group":"fun","isNew":true},
  {"id":"what-to-eat","name":"今天吃什么","icon":"shop","color":"#F472B6","desc":"选择困难症救星，转一转决定","type":"page","target":"/pages_tools/what-to-eat/index","enabled":true,"group":"fun","isNew":true},
  {"id":"wallpaper","name":"壁纸下载","icon":"picture","color":"#38BDF8","desc":"精选高清壁纸，一键保存到相册","type":"page","target":"/pages_wallpaper/wallpaper/index","enabled":true,"group":"life"},
  {"id":"blog","name":"博客后台","icon":"article","color":"#60A5FA","desc":"管理文章、照片与博客内容","type":"web","target":"https://pnkx.top","enabled":true}
]',
    `remark` = '工具台首页展示的工具列表（JSON 数组），v2 增加 group/isNew 字段'
WHERE `config_key` = 'sys.tools.list';
