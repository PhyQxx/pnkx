import {saveArticle, getConfig, isConfigured} from './common.js';

const titleEl = document.getElementById('pageTitle');
const urlEl = document.getElementById('pageUrl');
const remarkCard = document.getElementById('remarkCard');
const configCard = document.getElementById('configCard');
const saveBtn = document.getElementById('saveBtn');
const statusEl = document.getElementById('status');

let currentTab = null;

document.getElementById('openOptions').onclick = () => chrome.runtime.openOptionsPage();
document.getElementById('goOptions').onclick = () => chrome.runtime.openOptionsPage();
saveBtn.onclick = handleSave;

async function init() {
    const [tab] = await chrome.tabs.query({active: true, currentWindow: true});
    currentTab = tab;
    titleEl.textContent = tab.title || tab.url || '(无标题)';
    urlEl.textContent = tab.url || '';

    const config = await getConfig();
    if (isConfigured(config)) {
        remarkCard.style.display = '';
        if (/^https?:/i.test(tab.url || '')) {
            saveBtn.disabled = false;
        } else {
            saveBtn.disabled = true;
            showStatus('当前页面不是网页，无法收藏', 'err');
        }
    } else {
        configCard.style.display = '';
    }
}

async function handleSave() {
    if (!currentTab || !currentTab.url) return;
    saveBtn.disabled = true;
    saveBtn.textContent = '保存中…';
    try {
        await saveArticle(currentTab.url, document.getElementById('remark').value.trim());
        showStatus('已收藏！正文抓取与 AI 整理在后台进行', 'ok');
        setTimeout(() => window.close(), 1200);
    } catch (error) {
        if (error.message === 'NOT_CONFIGURED') {
            showStatus('请先完成配置', 'err');
            configCard.style.display = '';
        } else {
            showStatus('收藏失败：' + error.message, 'err');
        }
        saveBtn.disabled = false;
        saveBtn.textContent = '收 藏';
    }
}

function showStatus(message, type) {
    statusEl.className = 'status ' + type;
    statusEl.textContent = message;
}

init();
