// A missing payload (for example an HTML fallback from a proxy) is not an empty list.
export function readPanelList(response) {
    if (!response || !Array.isArray(response.data)) {
        throw new Error('列表响应格式异常，请重试');
    }
    return response.data;
}
