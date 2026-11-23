import request from '@/utils/request'

/**
 * 工具使用打点（fire-and-forget，失败静默不影响跳转）
 * 放在主包 utils 下：工具箱主包页面（pages/tools.vue）openTool 时调用，
 * 微信分包规则不允许主包引用分包内 JS 模块，故不能放 pages_tools/api
 * 后端 POST /tools/use/log/record 匿名可写 + IP 限流，打点失败无感知
 * @param {{id:string, name:string}} tool 工具项（sys.tools.list 条目）
 */
export function recordToolUse(tool) {
  let source = 'mp'
  // #ifdef H5
  source = 'h5'
  // #endif
  // #ifdef APP-PLUS
  source = 'app'
  // #endif
  return request({
    url: '/tools/use/log/record',
    method: 'post',
    data: {
      toolId: tool && tool.id ? String(tool.id) : '',
      toolName: (tool && tool.name) || '',
      source
    }
  }).catch(() => {
    // 打点失败静默
  })
}
