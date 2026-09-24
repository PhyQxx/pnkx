/** Incremental SSE parser that preserves partial lines across network chunks. */
export function createSseParser(onData) {
  let buffer = ''
  let dataLines = []
  let done = false

  function dispatch() {
    if (!dataLines.length) return
    const data = dataLines.join('\n')
    dataLines = []
    if (data === '[DONE]') {
      done = true
      return
    }
    if (!done && data) onData(data)
  }

  function consumeLine(line) {
    if (line.endsWith('\r')) line = line.slice(0, -1)
    if (line === '') {
      dispatch()
      return
    }
    if (line.startsWith('data:')) {
      const value = line.slice(5)
      dataLines.push(value.startsWith(' ') ? value.slice(1) : value)
    }
  }

  return {
    push(chunk) {
      if (done || !chunk) return
      buffer += chunk
      let newline
      while ((newline = buffer.indexOf('\n')) >= 0) {
        consumeLine(buffer.slice(0, newline))
        buffer = buffer.slice(newline + 1)
      }
    },
    finish() {
      if (done) return
      if (buffer) consumeLine(buffer)
      buffer = ''
      dispatch()
    },
    isDone() { return done }
  }
}

