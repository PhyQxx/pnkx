// Process readiness only; does not issue requests to the production backend.
export default defineEventHandler(() => ({ status: 'ok' }))
