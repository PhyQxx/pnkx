export function createLatestLoader({ fetch, onStart, onData, onError, onSettled }) {
    let revision = 0;
    return {
        invalidate() {
            revision++;
        },
        async run(input) {
            const current = ++revision;
            onStart();
            try {
                const result = await fetch(input);
                if (current === revision) onData(result);
            } catch (error) {
                if (current === revision) onError(error);
            } finally {
                if (current === revision) onSettled();
            }
        }
    };
}
export function parsePinned(value, allowed) {
    try {
        const items = typeof value === 'string' ? JSON.parse(value) : value;
        return Array.isArray(items) ? [...new Set(items.filter((key) => Object.hasOwn(allowed, key)))].slice(0, 6) : [];
    } catch {
        return [];
    }
}
