/**
 * 比较版本号
 * @param {string} v1
 * @param {string} v2
 * @returns {number} 1 if v1 > v2, -1 if v1 < v2, 0 if v1 == v2
 */
export function compareVersion(v1, v2) {
  const arr1 = v1.split('.');
  const arr2 = v2.split('.');
  const len = Math.max(arr1.length, arr2.length);

  while (arr1.length < len) {
    arr1.push('0');
  }
  while (arr2.length < len) {
    arr2.push('0');
  }

  for (let i = 0; i < len; i++) {
    const num1 = parseInt(arr1[i]);
    const num2 = parseInt(arr2[i]);

    if (num1 > num2) {
      return 1;
    } else if (num1 < num2) {
      return -1;
    }
  }

  return 0;
}

import manifest from '@/manifest.json'

/**
 * 获取App版本号
 * 优先从manifest.json获取（支持wgt热更新后的版本）
 * @returns {string} 版本号
 */
export function getAppVersion() {
  return manifest.versionName || '';
}
