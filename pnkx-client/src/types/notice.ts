/**
 * 通知
 */
export interface Notice {
  /**
   * 通知id
   */
  noticeId?: number
  /**
   * 标题
   */
  noticeTitle: string
  /**
   * 封面
   */
  cover: string
  /**
   * 内容md格式
   */
  contentMd: string
  /**
   * 公告内容
   */
  noticeContent: string
  /**
   * 公告类型
   */
  noticeType: string

  /**
   * 作者昵称
   */
  author: string
  /**
   * 通知标签
   */
  remark: string
  /**
   * 阅读数
   */
  read?: number
  /**
   * 留言数
   */
  leaveMessageNumber?: number
  /**
   * 创建时间
   */
  createTime?: string
}
