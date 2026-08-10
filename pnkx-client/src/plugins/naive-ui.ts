import { setup } from '@css-render/vue3-ssr'
import { defineNuxtPlugin } from '#app'

// Nuxt 3.12+ 改用 unhead 管理 SSR head，原先基于 ssrContext.renderMeta 的写法已失效，
// 导致 naive-ui 组件的 SSR 样式无法注入到 <head>，页面刷新时样式错乱/白屏。
// 这里改用 useServerHead，在服务端把 @css-render/vue3-ssr 收集到的样式注入到 head 中。
// 参考: https://github.com/07akioni/nuxtjs-naive-ui/issues/4
export default defineNuxtPlugin((nuxtApp) => {
  if (process.server) {
    const { collect } = setup(nuxtApp.vueApp)
    useServerHead({
      style: () => {
        const stylesString = collect()
        const stylesArray = stylesString
          .split(/<\/style>/g)
          .filter((style) => style)
        return stylesArray.map((styleString: string) => {
          const match = styleString.match(/<style cssr-id="([^"]*)">([\s\S]*)/)
          if (match) {
            const id = match[1]
            return { 'cssr-id': id, children: match[2] }
          }
          return {}
        })
      }
    })
  }
})
