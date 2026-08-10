import type {Comment, CommentForm, CommentQuery} from '@/types/comment'
import type {HttpOption} from '@/composables/useHttp'

enum Api {
  list = '/client/message/getMessageList',
  add = '/admin/message/addMessage'
}

/**
 * 查看评论列表
 * @returns 评论列表
 */
export function getCommentList(params: CommentQuery, option?: HttpOption<Comment[]>) {
  return useHttp.get<Comment[]>(Api.list, params, option)
}

/**
 * 添加评论
 */
export function addComment(data: CommentForm) {
  return useHttp.post<null>(Api.add, data)
}
