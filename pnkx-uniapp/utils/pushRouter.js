const routes = {
  todo: sourceId => sourceId ? `/pages_life/todo/edit?id=${sourceId}&fromNotification=1` : '/pages_life/todo/index',
  commemoration: sourceId => sourceId ? `/pages_life/commemorationDay/add?id=${sourceId}&fromNotification=1` : '/pages_life/commemorationDay/index',
  subscription: sourceId => `/pages_life/subscription/index${sourceId ? `?highlight=${sourceId}` : ''}`,
  menstruation: () => '/pages_life/menstruationAssistant/index',
  budget: () => '/pages_life/bookkeeping/budget/index'
}

export function parsePushPayload(message) {
  const raw = message && message.data ? message.data.payload : null
  if (!raw) return null
  try {
    const payload = typeof raw === 'string' ? JSON.parse(raw) : raw
    if (!payload || !['life_reminder', 'budget_alert'].includes(payload.type)) return null
    return payload
  } catch (e) {
    return null
  }
}

export function resolvePushRoute(payload) {
  if (!payload || !routes[payload.sourceType]) return '/pages/index/index'
  return routes[payload.sourceType](payload.sourceId)
}

