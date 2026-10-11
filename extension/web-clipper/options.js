import {login, getConfig, normalizeApiUrl} from './common.js';

const apiUrlEl = document.getElementById('apiUrl');
const usernameEl = document.getElementById('username');
const passwordEl = document.getElementById('password');
const resultEl = document.getElementById('result');

async function init() {
    const config = await getConfig();
    apiUrlEl.value = config.apiUrl || '';
    usernameEl.value = config.username || '';
    passwordEl.value = config.password || '';
}

document.getElementById('saveBtn').onclick = async () => {
    const apiUrl = normalizeApiUrl(apiUrlEl.value);
    if (!apiUrl || !usernameEl.value || !passwordEl.value) {
        showResult('请填写完整', 'err');
        return;
    }
    // 保存时清空旧 token，下次收藏自动用新配置重新登录
    await chrome.storage.local.set({
        apiUrl,
        username: usernameEl.value.trim(),
        password: passwordEl.value,
        token: null
    });
    showResult('已保存，可以开始使用了', 'ok');
};

document.getElementById('testBtn').onclick = async () => {
    const config = {
        apiUrl: normalizeApiUrl(apiUrlEl.value),
        username: usernameEl.value.trim(),
        password: passwordEl.value
    };
    if (!config.apiUrl || !config.username || !config.password) {
        showResult('请填写完整后再测试', 'err');
        return;
    }
    try {
        await login(config);
        showResult('连接成功，账号可用 ✓', 'ok');
    } catch (error) {
        showResult('连接失败：' + error.message, 'err');
    }
};

function showResult(message, type) {
    resultEl.className = 'result ' + type;
    resultEl.textContent = message;
}

init();
