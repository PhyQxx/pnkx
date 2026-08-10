import type { PageQuery } from './index'

/**
 * 评论查询参数
 */
export interface CommentQuery extends PageQuery {
  /**
   * 父评论id
   */
  parentId?: number
  /**
   * 文章或固定项ID
   */
  articleId: number
  /**
   * 评论类型
   */
  messageBoard: string
}

/**
 * 评论
 */
export interface Comment {
  /**
   * 评论id
   */
  id: number
  /**
   * 父评论id
   */
  parentId?: number
  /**
   * 被评论id
   */
  replyId: number
  /**
   * 被评论用户id
   */
  replyUserId: number
  /**
   * 被评论用户昵称
   */
  replyNickName: string
  /**
   * 评论用户id
   */
  createBy: number
  /**
   * 昵称
   */
  nickName: string
  /**
   * 头像
   */
  avatar: string
  /**
   * 评论内容
   */
  content: string
  /**
   * 点赞数
   */
  likeNumber: number
  /**
   * 回复量
   */
  replyNumber: number
  /**
   * 回复列表
   */
  replyList: Comment[]
  /**
   * 评论时间
   */
  createTime: string
}

/**
 * 评论表单
 */
export interface CommentForm {
  /**
   * 类型id
   */
  articleId?: number
  /**
   * 评论类型 (1文章 2友链 3说说)
   */
  messageBoard: string
  /**
   * 父评论id
   */
  parentId?: number
  /**
   * 被回复评论id
   */
  replyId?: number
  /**
   * 被回复用户id
   */
  replyUserId?: number
  /**
   * 评论内容
   */
  content: string
}
