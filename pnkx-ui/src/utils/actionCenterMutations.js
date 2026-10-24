// One in-flight mutation per object; failed requests retain their exact payload for retry.
export function createActionMutations({ request, refresh, onBusy, onError, makeId, now = Date.now }) {
    const pending = new Set(),
        retries = new Map();
    let active = true,
        context = 0;
    return {
        contextChanged() {
            context++;
        },
        dispose() {
            active = false;
            retries.clear();
        },
        async run(key, signature, build) {
            if (!active || pending.has(key)) return false;
            const startedContext = context;
            pending.add(key);
            onBusy(key, true);
            onError('');
            const retryKey = `${key}:${signature}`;
            try {
                if (!retries.has(retryKey)) retries.set(retryKey, build({ id: makeId(), now: now() }));
                await request(retries.get(retryKey));
                retries.delete(retryKey);
                if (active && startedContext === context) await refresh();
                return true;
            } catch {
                if (active && startedContext === context)
                    onError('操作未确认成功，请重试或刷新核对；相同提醒操作重试不会重复执行。');
                return false;
            } finally {
                pending.delete(key);
                if (active) onBusy(key, false);
            }
        }
    };
}
