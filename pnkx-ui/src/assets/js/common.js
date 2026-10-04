/**
 * 颜色数组
 */
export const colorArray = [
    '#5A8DEE',
    '#CD594B',
    '#F8CE5E',
    '#4B9E65',
];

/**
 * 博客url（全项目博客前台地址唯一出口，勿在其他文件硬编码域名）
 * @type {string}
 */
export const BLOG_URL = 'https://pnkx.top/';

/**
 * FTP 图床对外地址（全项目唯一出口）
 * @type {string}
 */
export const FTP_SITE_URL = 'https://ftp.pnkx.top:8';

/**
 * 图片的文件类型
 */
export const IMAGE_TYPE = ['image/jpeg', '.jpg', '.jpeg', '.gif', '.bmp', '.webp'];

/**
 * websocket消息类型
 * @type {{}}
 */
export const WEBSOCKET_MESSAGE_TYPE = {
    LOGIN: 'login',
    LOG_OUT: 'log_out',
    CHAT_MESSAGE: 'chat_message'
}
