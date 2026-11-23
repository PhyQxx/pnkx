/**
 * 图片压缩引擎（三端条件编译适配，与 UI 解耦）
 *
 * 分端实现：
 * - 微信小程序：wx.compressImage（maxEdge 时按压缩宽高缩放，否则按 quality）
 * - App：uni.compressImage（仅质量压缩；App 端不支持指定目标宽高）
 * - H5：canvas 重绘 + toBlob（createImageBitmap 处理 EXIF 方向，不支持时降级）
 *
 * 统一返回 { tempFilePath, size }，size 为压缩后字节数（拿不到时为 0，由 UI 显示 '—'）
 */

/** 超过该大小（字节）不压缩：25MB */
export const MAX_FILE_SIZE = 25 * 1024 * 1024

/**
 * 大小格式化
 * @param {number} bytes 字节数
 * @returns {string} 如 1.2MB / 356KB
 */
export function formatSize(bytes) {
  if (!(bytes > 0)) return '—'
  if (bytes >= 1024 * 1024) {
    return (bytes / 1024 / 1024).toFixed(1) + 'MB'
  }
  return Math.round(bytes / 1024) + 'KB'
}

/**
 * 压缩率徽标文本
 * @param {number} before 原始字节
 * @param {number} after 压缩后字节
 * @returns {string} 如 -62%，无有效值时返回 ''
 */
export function ratioText(before, after) {
  if (!(before > 0) || !(after > 0)) return ''
  const percent = Math.round((1 - after / before) * 100)
  return percent > 0 ? '-' + percent + '%' : '未缩小'
}

/**
 * 是否应跳过压缩
 * @param {string} fileName 文件名
 * @param {number} fileSize 文件字节（0 表示未知，不按大小拦截）
 * @returns {string} 跳过原因，空串表示不跳过
 */
export function skipReason(fileName, fileSize) {
  if (/\.gif$/i.test(fileName || '')) {
    return 'GIF 动图不支持压缩'
  }
  if (fileSize > MAX_FILE_SIZE) {
    return '文件超过 25MB'
  }
  return ''
}

/**
 * 读取图片信息（宽高）
 * @param {string} filePath 图片路径
 * @returns {Promise<{width:number, height:number}>}
 */
function getImageInfo(filePath) {
  return new Promise((resolve, reject) => {
    uni.getImageInfo({
      src: filePath,
      success: resolve,
      fail: (err) => reject(new Error((err && err.errMsg) || 'getImageInfo fail'))
    })
  })
}

/**
 * 读取文件字节数
 * @param {string} filePath 文件路径
 * @returns {Promise<number>} 失败返回 0
 */
function getFileSize(filePath) {
  return new Promise(resolve => {
    // #ifdef MP-WEIXIN
    try {
      wx.getFileSystemManager().getFileInfo({
        filePath,
        success: (res) => resolve(res.size || 0),
        fail: () => resolve(0)
      })
    } catch (e) {
      resolve(0)
    }
    // #endif
    // #ifndef MP-WEIXIN
    uni.getFileInfo({
      filePath,
      success: (res) => resolve((res && res.size) || 0),
      fail: () => resolve(0)
    })
    // #endif
  })
}

/**
 * 回调 API 的 Promise 封装
 */
function invoke(options) {
  return new Promise((resolve, reject) => {
    options.success = resolve
    options.fail = (err) => reject(new Error((err && err.errMsg) || 'compressImage fail'))
    uni.compressImage(options)
  })
}

/**
 * 压缩单张图片
 * @param {Object} params
 * @param {string} params.filePath 源图路径（chooseImage 的 tempFilePath）
 * @param {number} params.quality 质量 10-100（默认 70）
 * @param {number} params.maxEdge 目标最长边像素（0=保持原图尺寸；App 端忽略）
 * @returns {Promise<{tempFilePath:string, size:number}>}
 */
export async function compressImage({ filePath, quality = 70, maxEdge = 0 }) {
  // #ifdef MP-WEIXIN
  {
    const params = { src: filePath, quality }
    if (maxEdge > 0) {
      const info = await getImageInfo(filePath)
      const scale = Math.min(1, maxEdge / Math.max(info.width, info.height))
      if (scale < 1) {
        params.compressedWidth = Math.round(info.width * scale)
        params.compressedHeight = Math.round(info.height * scale)
      }
    }
    const res = await new Promise((resolve, reject) => {
      wx.compressImage({
        ...params,
        success: resolve,
        fail: (err) => reject(new Error((err && err.errMsg) || 'compressImage fail'))
      })
    })
    const size = await getFileSize(res.tempFilePath)
    return { tempFilePath: res.tempFilePath, size }
  }
  // #endif

  // #ifdef APP-PLUS
  {
    // App 端 compressImage 仅支持质量压缩
    const res = await invoke({ src: filePath, quality })
    const size = await getFileSize(res.tempFilePath)
    return { tempFilePath: res.tempFilePath, size }
  }
  // #endif

  // #ifdef H5
  return await compressH5(filePath, quality, maxEdge)
  // #endif
}

/**
 * H5 压缩：canvas 重绘 + toBlob
 * PNG 输入保持 PNG（toBlob 对 png 忽略 quality，压缩来自 maxEdge 缩放）
 */
async function compressH5(filePath, quality, maxEdge) {
  const response = await fetch(filePath)
  const blob = await response.blob()
  const bitmap = await loadBitmapH5(blob)
  const scale = maxEdge > 0
    ? Math.min(1, maxEdge / Math.max(bitmap.width, bitmap.height))
    : 1
  const width = Math.max(1, Math.round(bitmap.width * scale))
  const height = Math.max(1, Math.round(bitmap.height * scale))

  const canvas = document.createElement('canvas')
  canvas.width = width
  canvas.height = height
  const ctx = canvas.getContext('2d')
  // png 转 jpeg 时铺白底避免黑背景
  const keepPng = blob.type === 'image/png'
  if (!keepPng) {
    ctx.fillStyle = '#FFFFFF'
    ctx.fillRect(0, 0, width, height)
  }
  ctx.drawImage(bitmap, 0, 0, width, height)
  if (bitmap.close) bitmap.close()

  const outBlob = await new Promise((resolve, reject) => {
    canvas.toBlob(
      b => b ? resolve(b) : reject(new Error('canvas toBlob fail')),
      keepPng ? 'image/png' : 'image/jpeg',
      quality / 100
    )
  })
  const tempFilePath = URL.createObjectURL(outBlob)
  return { tempFilePath, size: outBlob.size }
}

/**
 * H5 加载位图：优先 createImageBitmap 修正 EXIF 方向，不支持时降级 Image 元素
 */
async function loadBitmapH5(blob) {
  if (typeof createImageBitmap === 'function') {
    try {
      return await createImageBitmap(blob, { imageOrientation: 'from-image' })
    } catch (e) {
      // 部分浏览器不支持 imageOrientation 参数，回退基础调用
      try {
        return await createImageBitmap(blob)
      } catch (e2) {
        // 继续走 Image 元素降级
      }
    }
  }
  const objectUrl = URL.createObjectURL(blob)
  try {
    const img = await new Promise((resolve, reject) => {
      const el = new Image()
      el.onload = () => resolve(el)
      el.onerror = () => reject(new Error('image load fail'))
      el.src = objectUrl
    })
    return img
  } finally {
    URL.revokeObjectURL(objectUrl)
  }
}

/**
 * H5 触发浏览器下载（保存兜底）
 * @param {string} objectUrl blob URL
 * @param {string} fileName 下载文件名
 */
export function downloadH5(objectUrl, fileName) {
  const link = document.createElement('a')
  link.href = objectUrl
  link.download = fileName || 'compressed.jpg'
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
}
