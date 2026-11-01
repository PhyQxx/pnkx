// Descriptions enrich the server registry; registration status always comes from /ai/tools.
const rows = [
    ['assignment', '分工转派', '日程', '查询负责人、候选成员与记录，确认后转派待办或采购', '查询购物商品编号1的分工记录和可转派成员'],
    ['calendar_query', '日历查询', '日程', '按日期查看日历事项，最多90天；按天汇总不推断时段冲突', '查看明天的日历安排'],
    ['bookkeeping', '智能记账', '财务', '记录收入和支出，生成记账草稿', '今天午饭花了28元'],
    ['analysis', '账单分析', '财务', '查询消费记录、分析收支情况', '分析一下本月的餐饮支出'],
    ['todo', '待办管理', '日程', '查询、新增、修改、完成和删除待办', '明天下午提醒我取快递'],
    ['diary_write', '记录日记', '内容', '把生活和心情整理成日记', '记录今天的日记：下班后去散步，心情不错'],
    ['diary_analysis', '日记分析', '内容', '总结日记内容，回顾心情变化', '总结一下最近的日记'],
    ['note', '笔记管理', '内容', '创建、搜索和总结笔记', '查找关于旅行的笔记'],
    ['commemoration_day', '纪念日', '日程', '记录生日和重要纪念日', '添加每年11月20日的生日提醒'],
    ['shopping_list', '新增采购', '饮食与采购', '解析商品和数量，加入购物清单', '购物清单加上鸡蛋6个、牛奶2盒'],
    ['shopping_manage', '购物管理', '饮食与采购', '查询、认领、释放认领及更新采购状态', '查询购物清单里的牛奶'],
    ['recipe', '菜谱生成', '饮食与采购', '从网页或文字提取食材与做法，确认入库', 'https://www.xiachufang.com/recipe/100505867/ 生成到菜谱库'],
    ['meal_plan', '餐饮计划', '饮食与采购', '安排餐次，可关联已有菜谱生成采购计划', '安排明天晚餐吃皮蛋瘦肉粥，两人份'],
    ['life_catalog', '生活资料查询', '生活', '查询订阅、预算、阅读、菜谱和日历', '查一下菜谱库里有哪些粥'],
    ['life_reminder', '生活提醒', '生活', '查看纪念日、情侣卡券和生理期相关提醒', '最近有哪些纪念日'],
    ['life_report', '生活报告', '生活', '生成周报、月报和消费或心情总结', '生成本周生活周报'],
    ['content_publish', '内容转换', '内容', '阅读记录、笔记和日记整理成笔记或博客草稿', '把我的笔记整理成博客草稿'],
    ['web_clip', '网页收藏', '内容', '收藏链接，后台抓取正文与生成摘要', '收藏网页 https://example.com'],
    ['chat', '普通对话', '通用', '解答问题与日常交流', '帮我想几个周末活动']
];
export const toolMetadata = Object.fromEntries(rows.map(([name, title, category, summary, example]) => [name, { title, category, summary, example }]));
export const plannedTools = [
    ['assignment', '分工转派', '日程', '查询负责人和分工记录，转派任务', 'M2'],
    ['calendar_query', '日历查询', '日程', '汇总日期范围内的安排，检查时间冲突', 'M2'],
    ['subscription_manage', '订阅管理', '财务', '新增、修改和停用本系统订阅记录', 'M3'],
    ['budget_manage', '预算管理', '财务', '设置月度与分类预算，查询剩余额度', 'M3'],
    ['bookkeeping_manage', '账单管理', '财务', '纠正账单、分类和检查疑似重复', 'M3'],
    ['reminder_manage', '周期提醒', '日程', '创建、修改与暂停周期提醒', 'M3'],
    ['web_to_content', '网页转内容', '内容', '将收藏网页整理为笔记、菜谱或阅读记录', 'M4'],
    ['reading_manage', '阅读管理', '内容', '管理书单、进度和摘录', 'M4'],
    ['family_inventory', '家庭库存', '饮食与采购', '管理食材数量、单位和保质期', 'M4'],
    ['gift_plan', '礼物准备', '生活', '结合纪念日和预算规划礼物准备事项', 'M4'],
    ['life_search', '统一搜索', '通用', '跨笔记、日记、菜谱等模块搜索', 'M4']
].map(([name, title, category, summary, phase]) => ({ name, title, category, summary, phase, status: 'planned' }));
export function buildToolCatalog(registered) {
    const live = registered.map(tool => ({ ...toolMetadata[tool.name], ...tool, title: toolMetadata[tool.name]?.title || tool.name,
        category: toolMetadata[tool.name]?.category || '通用', summary: toolMetadata[tool.name]?.summary || tool.description, status: 'registered' }));
    const names = new Set(live.map(tool => tool.name));
    return [...live, ...plannedTools.filter(tool => !names.has(tool.name))];
}
