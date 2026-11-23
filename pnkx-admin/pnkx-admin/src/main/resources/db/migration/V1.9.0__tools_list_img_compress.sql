-- ============================================================
-- V1.9.0 工具箱扩展 M3：sys.tools.list 增加图片压缩（共 9 个工具）
--   前端新增 pages_tools/img-compress（三端压缩适配）+ utils/tools/albumSaver.js、imageTools.js
--   注意：sys_config 有 Redis 缓存，后端重启时全量刷新
-- ============================================================

UPDATE `sys_config`
SET `config_value` = '[
  {
    "id":"mortgage",
    "name":"房贷计算器",
    "icon":"wallet",
    "color":"#6C9EFF",
    "desc":"商贷 / 公积金，等额本息、本金对比",
    "type":"page",
    "target":"/pages_tools/mortgage/index",
    "enabled":true,
    "group":"finance",
    "isNew":true
  },
  {
    "id":"tax",
    "name":"个税计算器",
    "icon":"compose",
    "color":"#FBBF24",
    "desc":"年度累计预扣，年终奖单独计税",
    "type":"page",
    "target":"/pages_tools/tax/index",
    "enabled":true,
    "group":"finance",
    "isNew":true
  },
  {
    "id":"exchange",
    "name":"汇率换算",
    "icon":"flag",
    "color":"#34D399",
    "desc":"实时汇率，配合记账外币场景",
    "type":"page",
    "target":"/pages_tools/exchange/index",
    "enabled":true,
    "group":"finance",
    "isNew":true
  },
  {
    "id":"date-calc",
    "name":"日期计算器",
    "icon":"calendar",
    "color":"#FB923C",
    "desc":"日期间隔、工作日、农历查询",
    "type":"page",
    "target":"/pages_tools/date-calc/index",
    "enabled":true,
    "group":"life",
    "isNew":true
  },
  {
    "id":"img-compress",
    "name":"图片压缩",
    "icon":"images",
    "color":"#60A5FA",
    "desc":"批量压缩，博客传图更省流量",
    "type":"page",
    "target":"/pages_tools/img-compress/index",
    "enabled":true,
    "group":"life",
    "isNew":true
  },
  {
    "id":"ai-pickup",
    "name":"AI 情话生成",
    "icon":"heart",
    "color":"#A78BFA",
    "desc":"土味情话、藏头诗、三行情书",
    "type":"page",
    "target":"/pages_tools/ai-pickup/index",
    "enabled":true,
    "group":"fun",
    "isNew":true
  },
  {
    "id":"what-to-eat",
    "name":"今天吃什么",
    "icon":"shop",
    "color":"#F472B6",
    "desc":"选择困难症救星，转一转决定",
    "type":"page",
    "target":"/pages_tools/what-to-eat/index",
    "enabled":true,
    "group":"fun",
    "isNew":true
  },
  {
    "id":"wallpaper",
    "name":"壁纸下载",
    "icon":"picture",
    "color":"#38BDF8",
    "desc":"精选高清壁纸，一键保存到相册",
    "type":"page",
    "target":"/pages_wallpaper/wallpaper/index",
    "enabled":true,
    "group":"life"
  },
  {
    "id":"blog",
    "name":"博客后台",
    "icon":"article",
    "color":"#60A5FA",
    "desc":"管理文章、照片与博客内容",
    "type":"web",
    "target":"https://pnkx.top",
    "enabled":true
  }
]',
    `remark` = '工具台首页展示的工具列表（JSON 数组），v2 增加 group/isNew 字段'
WHERE `config_key` = 'sys.tools.list';
