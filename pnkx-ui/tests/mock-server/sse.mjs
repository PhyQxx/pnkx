/**
 * SSE 流式脚本工具
 *
 * chunk 格式：{ data: '文本', event: '事件名(可选)', delay: 毫秒(可选) }
 * 前端 chatStreamParser 契约：
 *   - 纯 data: 行按帧拼接（\r\n 与 \n 均可，空行分帧）
 *   - event: operation-plan + data:{json} 携带 planId
 *   - data:[DONE] 结束
 */

/**
 * 把 chunks 编成可执行的发送计划（默认：文本分 3 段 + [DONE]）
 */
export function buildStreamScript(chunks) {
    if (Array.isArray(chunks) && chunks.length) {
        return chunks.map(c => ({ data: c.data ?? '', event: c.event || '', delay: c.delay ?? 40 }))
    }
    return [
        { data: '你好', delay: 30 },
        { data: '，这是', delay: 40 },
        { data: '模拟回复。', delay: 40 },
        { data: '[DONE]', delay: 0 }
    ]
}

/**
 * 逐帧写出 SSE（含人工延迟），返回 Promise
 * data 内含换行时按 SSE 规范拆成多个 data: 行（前端 parser 按 \n\n 分帧后拼回）
 */
export async function writeSseFrames(res, script) {
    for (const frame of script) {
        if (frame.delay) {
            await new Promise(r => setTimeout(r, frame.delay))
        }
        if (frame.event) {
            res.write(`event: ${frame.event}\n`)
        }
        const lines = String(frame.data).split('\n')
        for (const line of lines) {
            res.write(`data: ${line}\n`)
        }
        res.write('\n')
    }
}
