import type { AnalyticsTrack } from '@/utils/analytics'
export function useAnalytics() {
  const app = useNuxtApp()
  const track: AnalyticsTrack = (name, options) => {
    if (process.client) app.$analytics?.(name, options)
  }
  return { track }
}
