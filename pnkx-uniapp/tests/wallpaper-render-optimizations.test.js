const fs = require('fs')
const path = require('path')
const assert = require('assert')

const root = path.resolve(__dirname, '..')
const indexVue = fs.readFileSync(path.join(root, 'pages_wallpaper/wallpaper/index.vue'), 'utf8')
const detailVue = fs.readFileSync(path.join(root, 'pages_wallpaper/wallpaper/detail.vue'), 'utf8')

function test(name, fn) {
  try {
    fn()
    console.log(`ok - ${name}`)
  } catch (error) {
    console.error(`not ok - ${name}`)
    console.error(error.message)
    process.exitCode = 1
  }
}

test('detail swiper only binds image src for the current item and neighbours', () => {
  assert.match(detailVue, /shouldRenderSwiperItem\s*\(/, 'detail.vue should expose a render-window guard')
  assert.match(detailVue, /getSwiperImageSrc\s*\(/, 'detail.vue should compute swiper image src lazily')
  assert.match(detailVue, /:src="getSwiperImageSrc\(wp,\s*idx\)"/, 'detail image src should go through the lazy src helper')
})

test('wallpaper index skips full refresh when returning from detail', () => {
  assert.match(indexVue, /skipNextShowRefresh:\s*false/, 'index.vue should track one-shot onShow refresh suppression')
  assert.match(indexVue, /this\.skipNextShowRefresh\s*=\s*true/, 'viewWallpaper should suppress the next onShow refresh')
  assert.match(indexVue, /if\s*\(this\.skipNextShowRefresh\)/, 'onShow should consume the suppression flag')
})

test('folder cover images use native lazy loading', () => {
  const folderImageMatch = indexVue.match(/<image[\s\S]*?class="cover-img"[\s\S]*?\/>/)
  assert(folderImageMatch, 'folder cover image markup should exist')
  assert.match(folderImageMatch[0], /:lazy-load="true"/, 'folder cover images should lazy load')
})
