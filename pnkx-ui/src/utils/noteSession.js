const fields = ['title', 'content', 'richText', 'folder', 'order', 'remark']
export const cloneNote = note => JSON.parse(JSON.stringify(note))
export const noteChanged = (a, b) => fields.some(key => a?.[key] !== b?.[key])

export function noteSession(server, draft) {
  return {
    saved: cloneNote(server), draft: cloneNote(draft || server),
    conflict: draft && draft.revision !== server.revision ? server : null,
    error: null, running: null, timer: null
  }
}

/** One writer per note. Edits arriving during a request are sent with its returned revision. */
export function saveSession(state, send, changed = () => {}) {
  if (state.running) return state.running
  if (state.conflict) return Promise.resolve(false)
  state.error = null
  state.running = (async () => {
    try {
      while (noteChanged(state.draft, state.saved)) {
        const snapshot = cloneNote(state.draft)
        const result = await send(snapshot)
        state.saved = cloneNote(result.data)
        state.draft.revision = result.data.revision
        changed(state)
      }
      return true
    } catch (error) {
      state.error = error
      state.conflict = error.current || null
      changed(state)
      return false
    } finally {
      state.running = null
    }
  })()
  // Also clear a synchronously completed no-op save.
  state.running.finally(() => { state.running = null })
  return state.running
}
