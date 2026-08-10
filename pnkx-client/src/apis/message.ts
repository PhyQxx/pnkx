import type { Message, MessageForm } from '@/types/message'
import type { HttpOption } from '@/composables/useHttp'

enum Api {
  message = '/client/message/getMessageList',
  add = '/admin/message/addMessage'
}

/**
 * 查看留言列表
 * @returns 文章分类
 */
export function getMessageList(message?: any, option?: HttpOption<Message[]>) {
  return useHttp.get<Message[]>(Api.message, message, option)
}

/**
 * 添加留言
 * @param data 留言
 */
export function addMessage(data: MessageForm) {
  return useHttp.post(Api.add, data)
}
