/**
 * pnkx 网址收藏 - Service Worker
 * 快捷键（Ctrl/Cmd+Shift+S）静默收藏当前标签页
 */
import {quickSaveWithNotify} from './common.js';

chrome.commands.onCommand.addListener((command) => {
    if (command !== 'quick-save') {
        return;
    }
    chrome.tabs.query({active: true, currentWindow: true}).then(([tab]) => {
        quickSaveWithNotify(tab);
    });
});
