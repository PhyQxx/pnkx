export function createChatStreamParser(onText, onPlan) {
  let buffer = '', event = '', data = [];
  const dispatch = () => {
    const value = data.join('\n');
    if (event === 'operation-plan') {
      try {
        const id = JSON.parse(value).planId;
        if (typeof id === 'string' && /^[0-9a-f]{8}-[0-9a-f-]{27}$/i.test(id)) onPlan(id);
      } catch { /* malformed metadata must not become chat text */ }
    } else if ((!event || event === 'message') && value && value !== '[DONE]') onText(value);
    event = ''; data = [];
  };
  const line = value => {
    if (!value) return dispatch();
    if (value.startsWith('event:')) event = value.slice(6).trim();
    if (value.startsWith('data:')) data.push(value.slice(5).replace(/^ /, ''));
  };
  return {
    push(chunk) {
      buffer += chunk;
      let index;
      while ((index = buffer.indexOf('\n')) >= 0) {
        line(buffer.slice(0, index).replace(/\r$/, ''));
        buffer = buffer.slice(index + 1);
      }
    },
    finish() { if (buffer) line(buffer.replace(/\r$/, '')); buffer = ''; dispatch(); }
  };
}
