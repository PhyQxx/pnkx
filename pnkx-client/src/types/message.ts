/**
 * 留言
 */
export interface Message {
  /**
   * 留言id
   */
  id: number
  /**
   * 昵称
   */
  nickName: string
  /**
   * 头像
   */
  avatar: string
  /**
   * 留言内容
   */
  content: string

  /**
   * 创建时间
   */
  createTime: string

  /**
   * 留言类型留言类型
   0：文章留言；
   1：留言板留言；
   2：相册留言；
   3：友链留言；
   4：照片留言；
   5：笔记留言；
   6：通知公告留言；
   7：视频板块留言；
   8：视频弹幕；
   */
  messageBoard: string
}

/**
 * 留言表单
 */
export interface MessageForm {
  /**
   * 昵称
   */
  nickName: string
  /**
   * 头像
   */
  avatar: string
  /**
   * 留言内容
   */
  content: string
}
