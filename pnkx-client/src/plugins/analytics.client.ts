import { API_BASE_URL } from '@/utils/apiBase'
import { analyticsPage, type AnalyticsTrack } from '@/utils/analytics'

export default defineNuxtPlugin((app) => {
  const config = useRuntimeConfig()
  if (String(config.public.analyticsEnabled) !== 'true') return { provide: { analytics: (() => {}) as AnalyticsTrack } }
  const router = useRouter()
  const { getToken, tokenPrefix } = useToken()
  const id = () => crypto.randomUUID()
  const read = (storage: Storage, key: string) => { try { return storage.getItem(key) } catch { return null } }
  const write = (storage: Storage, key: string, value: string) => { try { storage.setItem(key, value) } catch { /* private mode */ } }
  // Access to storage itself can throw in restricted browser contexts.
  let local: Storage | undefined, session: Storage | undefined
  try { local = window.localStorage; session = window.sessionStorage } catch { /* memory only */ }
  const visitorId = (local && read(local, 'pnkx.analytics.visitor')) || id()
  if (local) write(local, 'pnkx.analytics.visitor', visitorId)
  let sessionId = (session && read(session, 'pnkx.analytics.session')) || id()
  let lastActivity = Number(session && read(session, 'pnkx.analytics.activity')) || 0
  let pageViewId = '', path = '', page = analyticsPage('/'), duration = 0
  let lastTick = Date.now(), lastInteraction = Date.now(), disposed = false
  let queue: Record<string, unknown>[] = [], sending = false
  let referrerHost: string | null = null
  try { referrerHost = new URL(document.referrer).hostname } catch { /* direct visit */ }
  function saveSession() {
    if (!session) return
    write(session, 'pnkx.analytics.session', sessionId)
    write(session, 'pnkx.analytics.activity', String(lastActivity))
  }
  function enqueue(eventName: string, operationId?: string, durationMs = 0) {
    if (!pageViewId || disposed) return
    if (queue.length >= 100) return
    queue.push({ eventId: id(), visitorId, sessionId, pageViewId, ...page, eventName, operationId, durationMs, referrerHost, occurredAt: Date.now() })
  }
  function ensureSession() {
    const now = Date.now()
    if (now - lastActivity > 30 * 60 * 1000) {
      sessionId = id()
      if (pageViewId) { pageViewId = id(); enqueue('page_view') }
    }
    lastActivity = now
    saveSession()
  }
  async function flush() {
    if (sending || !queue.length) return
    sending = true
    const batch = queue.slice(0, 20)
    try {
      const headers: Record<string, string> = { 'Content-Type': 'application/json' }
      if (getToken()) headers.Authorization = tokenPrefix + getToken()
      const response = await fetch(`${API_BASE_URL}/client/analytics/events`, { method: 'POST', headers, body: JSON.stringify(batch), keepalive: true })
      const result = response.ok ? await response.json() : null
      if (result?.code === 200 || response.status === 400) queue.splice(0, batch.length)
    } catch { /* retain IDs for idempotent retry; never affect business UI */ }
    finally { sending = false }
  }
  function tick() {
    const now = Date.now(), elapsed = now - lastTick
    lastTick = now
    if (pageViewId && !document.hidden && now - lastInteraction <= 60000 && elapsed >= 0 && elapsed <= 10000) {
      ensureSession()
      duration += elapsed
    }
  }
  function reportDuration() {
    if (duration > 0) { enqueue('page_duration', undefined, Math.min(duration, 60000)); duration = 0 }
  }
  function visit() {
    const next = router.currentRoute.value.path
    if (pageViewId && path === next) return
    tick(); reportDuration()
    pageViewId = ''
    ensureSession()
    path = next; page = analyticsPage(next); pageViewId = id()
    lastTick = lastInteraction = Date.now()
    enqueue('page_view')
    void flush()
  }
  // Attribute async results to the page where the operation started.
  const operations = new Map<string, { pageViewId: string; sessionId: string; pageKey: string; contentId: string | null }>()
  const track: AnalyticsTrack = (name, options) => {
    ensureSession()
    const operationId = options?.operationId
    if (operationId && name.endsWith('_submit')) {
      if (operations.size >= 100) operations.delete(operations.keys().next().value!)
      operations.set(operationId, { pageViewId, sessionId, ...page })
    }
    const before = queue.length
    enqueue(name, operationId)
    if (operationId && queue.length > before) {
      const context = operations.get(operationId)
      if (context) Object.assign(queue[queue.length - 1], context)
      if (!name.endsWith('_submit')) operations.delete(operationId)
    }
  }
  const interact = () => { ensureSession(); lastInteraction = Date.now() }
  const hide = () => { tick(); reportDuration(); void flush(); lastTick = Date.now() }
  const restore = (event: PageTransitionEvent) => {
    if (event.persisted) { pageViewId = ''; visit() }
  }
  app.hook('app:mounted', visit)
  const removeRoute = router.afterEach((_to, _from, failure) => { if (!failure) visit() })
  const tickTimer = setInterval(tick, 5000)
  const sendTimer = setInterval(() => { reportDuration(); void flush() }, 15000)
  const activityEvents = ['pointerdown', 'keydown', 'scroll']
  activityEvents.forEach(name => window.addEventListener(name, interact, { passive: true }))
  document.addEventListener('visibilitychange', hide)
  window.addEventListener('pagehide', hide)
  window.addEventListener('pageshow', restore)
  if (import.meta.hot) import.meta.hot.dispose(() => {
    disposed = true; clearInterval(tickTimer); clearInterval(sendTimer); removeRoute()
    activityEvents.forEach(name => window.removeEventListener(name, interact))
    document.removeEventListener('visibilitychange', hide)
    window.removeEventListener('pagehide', hide); window.removeEventListener('pageshow', restore)
  })
  return { provide: { analytics: track } }
})
