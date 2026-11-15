/**
 * 业务 SVG 图标（源自 pnkx-uniapp staticPackage：记账分类 c-*、天气 w-*、心情 x-*、账户 a-* 等）
 *
 * 构建期将 src/mobile/assets/svg 下的图标收集为 URL 映射；
 * 页面用 <img :src="iconUrl('c-can')"> 引用，未命中时返回空串（img 隐藏）。
 */
const modules = import.meta.glob('../assets/svg/*.svg', { query: '?url', import: 'default', eager: true })

const iconMap = {}
Object.keys(modules).forEach(file => {
    const name = file.replace(/^.*\/([^/]+)\.svg$/, '$1')
    iconMap[name] = modules[file]
})

/** 全部图标名（图标选择器用） */
export const iconNames = Object.keys(iconMap).sort()

/** 按名称取图标 URL；支持带/不带 .svg 后缀 */
export function iconUrl(name) {
    if (!name) return ''
    return iconMap[String(name).replace(/\.svg$/, '')] || ''
}
