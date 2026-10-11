/**
 * pnkx 网址收藏插件 - 共享逻辑
 * 认证流程：无 token 或 401 时用账号密码调 POST /login 换取 token（pnkx 登录无验证码），
 * 之后携带 Authorization: Bearer <token> 调 POST /client/webArticle 静默收藏。
 */

const CONFIG_KEYS = ['apiUrl', 'username', 'password', 'token'];

function normalizeApiUrl(url) {
    return (url || '').trim().replace(/\/+$/, '');
}

async function getConfig() {
    const config = await chrome.storage.local.get(CONFIG_KEYS);
    config.apiUrl = normalizeApiUrl(config.apiUrl);
    return config;
}

function isConfigured(config) {
    return !!(config.apiUrl && config.username && config.password);
}

/**
 * 登录换取 token。pnkx 登录响应：{code: 200, token: "..."}
 */
async function login(config) {
    const response = await fetch(config.apiUrl + '/login', {
        method: 'POST',
        headers: {'Content-Type': 'application/json'},
        body: JSON.stringify({userName: config.username, password: config.password})
    });
    const result = await response.json().catch(() => null);
    if (!result || result.code !== 200 || !result.token) {
        throw new Error(result && result.msg ? result.msg : '登录失败（检查账号密码）');
    }
    await chrome.storage.local.set({token: result.token});
    return result.token;
}

/**
 * 确保有可用 token（无则登录）
 */
async function ensureToken(config) {
    if (!config.token) {
        return login(config);
    }
    return config.token;
}

/**
 * 收藏网址。401 时自动重登录一次再重试。
 */
export async function saveArticle(url, remark) {
    const config = await getConfig();
    if (!isConfigured(config)) {
        throw new Error('NOT_CONFIGURED');
    }
    let token = await ensureToken(config);
    const doSave = async (authToken) => fetch(config.apiUrl + '/client/webArticle', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
            'Authorization': 'Bearer ' + authToken
        },
        body: JSON.stringify({url, remark: remark || ''})
    });

    let response = await doSave(token);
    if (response.status === 401) {
        // token 过期：重新登录后重试一次
        token = await login(config);
        response = await doSave(token);
    }
    const result = await response.json().catch(() => null);
    if (!result || result.code !== 200) {
        throw new Error(result && result.msg ? result.msg : '保存失败（HTTP ' + response.status + '）');
    }
    return result.data;
}

/**
 * 快捷键静默收藏：通知栏提示结果
 */
export async function quickSaveWithNotify(tab) {
    if (!tab || !tab.url || !/^https?:/i.test(tab.url)) {
        notify('收藏失败', '当前页面不是网页');
        return;
    }
    try {
        await saveArticle(tab.url, tab.title || '');
        notify('已收藏', (tab.title || tab.url).slice(0, 60));
    } catch (error) {
        const message = error.message === 'NOT_CONFIGURED' ? '请先在插件选项中配置服务器与账号' : error.message;
        notify('收藏失败', message);
    }
}

function notify(title, message) {
    chrome.notifications.create({
        type: 'basic',
        iconUrl: 'icons/icon128.png',
        title,
        message
    });
}
