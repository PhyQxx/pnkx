// @ts-ignore markdown-it does not ship declarations for its browser bundle.
import MarkdownIt from 'markdown-it/dist/markdown-it.js'
// @ts-ignore
import hljs from 'highlight.js/lib/common'
// @ts-ignore
import iterator from 'markdown-it-for-inline'
// @ts-ignore
import container from 'markdown-it-container'
export default defineNuxtPlugin(() => {
  return {
    provide: {
      markdownIt: (data: string) => {
        const md = new MarkdownIt({
          html: true,
          linkify: true,
          typographer: true,
          highlight: function (str, lang) {
            // 当前时间加随机数生成唯一的id标识
            const codeIndex = parseInt(Date.now().toString()) + Math.floor(Math.random() * 10000000)
            // 复制功能主要使用的是 clipboard.js
            let html = `<button class="copy-btn" type="button" data-clipboard-action="copy" data-clipboard-target="#copy${codeIndex}">复制</button>`
            const linesLength = str.split(/\n/).length - 1
            // 生成行号
            let linesNum = '<span aria-hidden="true" class="line-numbers-rows">'
            for (let index = 0; index < linesLength; index++) {
              linesNum = linesNum + '<span></span>'
            }
            linesNum += '</span>'
            if (lang && hljs.getLanguage(lang)) {
              try {
                // highlight.js 高亮代码
                // const preCode = hljs.highlight(lang, str, true).value
                const preCode = hljs.highlight(str, {language: lang, ignoreIllegals: true}).value
                html = html + preCode
                if (linesLength) {
                  html += '<b class="name">' + lang + '</b>'
                }
                // 将代码包裹在 textarea 中，由于防止textarea渲染出现问题，这里将 "<" 用 "&lt;" 代替，不影响复制功能
                return `<pre class="hljs"><code>${html}</code>${linesNum}</pre><textarea style="position: absolute;top: -9999px;left: -9999px;z-index: -9999;" id="copy${codeIndex}">${str.replace(
                  /<\/textarea>/g,
                  '&lt;/textarea>'
                )}</textarea>`
              } catch (error) {
                console.error(error)
              }
            }

            const preCode = md.utils.escapeHtml(str)
            html = html + preCode
            return `<pre class="hljs"><code>${html}</code>${linesNum}</pre><textarea style="position: absolute;top: -9999px;left: -9999px;z-index: -9999;" id="copy${codeIndex}">${str.replace(
              /<\/textarea>/g,
              '&lt;/textarea>'
            )}</textarea>`
          }
        })
        // a标签 新窗口打开
        md.use(iterator, 'url_new_win', 'link_open', function (tokens: any, idx: number) {
          const aIndex = tokens[idx].attrIndex('target')
          if (aIndex < 0) {
            tokens[idx].attrPush(['target', '_blank'])
          } else {
            tokens[idx].attrs[aIndex][1] = '_blank'
          }
        })
        // 居中处理
        md.use(container, 'center', {
          validate: function(params: string) {
            return params.trim().match(/^center$/);
          },
          render: function (tokens: any, idx: number) {
            tokens[idx].info.trim().match(/^center$/);
            if (tokens[idx].nesting === 1) {
              // 打开容器标签
              return '<div style="text-align:center">';
            } else {
              // 关闭容器标签
              return '</div>\n';
            }
          }
        });
        // 文字处理
        md.use((md) => {
          md.renderer.rules.text = (tokens, idx) => {
            const token = tokens[idx];
            return textToHtml(token.content);
          };
        });
        // 渲染成html
        return md.render(data)
      },
      markdownItSearch: (data: string) => {
        const md = new MarkdownIt({
          html: true
        }).disable(['link', 'image'])
        return md.renderInline(data)
      },
      markdownItContent: (data: string) => {
        const md = new MarkdownIt()
        // 去除markdown标签
        return md
          .render(data)
          .replace(/<\/?[^>]*>/g, '')
          .replace(/[|]*\n/, '')
          .replace(/&npsp;/gi, '')
          .replace('::: center', '')
          .replace(':::', '')
      }
    }
  }
})

/**
 * 文字样式处理
 * @param content
 */
function textToHtml(content: string) {
  // 背景色
  if (content.startsWith('!!!#') && content.endsWith('!!!')) {
    const color = content.slice(3, 10);
    const text = content.slice(10, -3);
    if (color.match(/^#[0-9A-Fa-f]{6}$/)) {
      return `<span style="background-color: ${color};">${textToHtml(text)}</span>`;
    }
  } else if (content.startsWith('!!#') && content.endsWith('!!')) {
    const color = content.slice(2, 9);
    const text = content.slice(9, -2);
    if (color.match(/^#[0-9A-Fa-f]{6}$/)) {
      return `<span style="color: ${color};">${textToHtml(text)}</span>`;
    }
  } else if (content.startsWith('/') && content.endsWith('/')) {
    const text = content.slice(1, -1);
    return `<span style="text-decoration: underline;">${textToHtml(text)}</span>`;
  }
  return content;
}
