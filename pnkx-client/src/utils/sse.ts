/** Incremental SSE decoder. Handles CRLF split across network chunks. */
export function createSseParser(onData: (data: string) => void) {
  let line = ''
  let skipLF = false
  let data: string[] = []
  function endLine() {
    if (!line) {
      if (data.length) onData(data.join('\n'))
      data = []
    } else if (line === 'data' || line.startsWith('data:')) {
      data.push(line.slice(5).replace(/^ /, ''))
    }
    line = ''
  }
  return (chunk: string) => {
    for (const char of chunk) {
      if (skipLF && char === '\n') { skipLF = false; continue }
      skipLF = false
      if (char === '\r' || char === '\n') {
        endLine()
        skipLF = char === '\r'
      } else {
        line += char
      }
    }
  }
}
