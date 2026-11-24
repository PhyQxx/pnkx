export const analyticsEvents = ['read_25', 'read_50', 'read_75', 'read_100', 'video_select', 'video_start', 'video_25', 'video_50', 'video_75', 'video_complete', 'video_error', 'page_view', 'page_duration', 'search_open', 'search_submit', 'search_success', 'search_fail', 'search_click', 'comment_submit', 'comment_success', 'comment_fail', 'ai_open', 'ai_submit', 'ai_success', 'ai_fail', 'ai_stop'] as const
export type AnalyticsEventName = typeof analyticsEvents[number]
export function analyticsPage(path: string) {
  const fixed: Record<string, string> = { '/': 'home', '/type': 'category', '/archives': 'archives', '/video': 'video', '/album': 'album', '/message': 'message', '/link': 'link', '/share': 'share', '/login': 'login', '/user': 'user' }
  path = path.split(/[?#]/)[0].replace(/\/$/, '') || '/'
  const match = path.match(/^\/(post|type|videos)\/([0-9]{1,20})$/)
  return match ? { pageKey: ({ post: 'post_detail', type: 'category_detail', videos: 'video_detail' } as Record<string, string>)[match[1]], contentId: match[2] } : { pageKey: fixed[path] || 'other', contentId: null }
}
export interface AnalyticsOptions { operationId?: string; targetId?: string }
export type AnalyticsTrack = (name: AnalyticsEventName, options?: AnalyticsOptions) => void

// Viewport-bottom position within the article, not proof of reading comprehension.
export function readingThresholds(top: number, height: number, viewportHeight: number) {
  if (height <= 0 || viewportHeight <= 0 || top >= viewportHeight) return []
  const percentage = Math.min(100, Math.max(0, (viewportHeight - top) / height * 100))
  return [25, 50, 75, 100].filter(value => percentage >= value)
}
export function playbackThresholds(current: number, duration: number) {
  if (!Number.isFinite(duration) || duration <= 0 || !Number.isFinite(current)) return []
  return [25, 50, 75].filter(value => current / duration * 100 >= value)
}
