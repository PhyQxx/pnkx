export const analyticsEvents = ['page_view', 'page_duration', 'search_open', 'search_submit', 'search_success', 'search_fail', 'search_click', 'comment_submit', 'comment_success', 'comment_fail', 'ai_open', 'ai_submit', 'ai_success', 'ai_fail', 'ai_stop'] as const
export type AnalyticsEventName = typeof analyticsEvents[number]
export function analyticsPage(path: string) {
  const fixed: Record<string, string> = { '/': 'home', '/type': 'category', '/archives': 'archives', '/video': 'video', '/album': 'album', '/message': 'message', '/link': 'link', '/share': 'share', '/login': 'login', '/user': 'user' }
  path = path.split(/[?#]/)[0].replace(/\/$/, '') || '/'
  const match = path.match(/^\/(post|type|videos)\/([0-9]{1,20})$/)
  return match ? { pageKey: ({ post: 'post_detail', type: 'category_detail', videos: 'video_detail' } as Record<string, string>)[match[1]], contentId: match[2] } : { pageKey: fixed[path] || 'other', contentId: null }
}
export interface AnalyticsOptions { operationId?: string }
export type AnalyticsTrack = (name: AnalyticsEventName, options?: AnalyticsOptions) => void
