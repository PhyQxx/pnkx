import type { Message } from '@/types/message'
/**
 * 视频
 */
export interface Video {
  /**
   * 视频id
   */
  id?: number
  /**
   * 标题
   */
  title: string
  /**
   * 封面
   */
  cover: string
  /**
   * 视频地址
   */
  url: string
  /**
   * 标签
   */
  label: string
  /**
   * 分类
   */
  type: string

  /**
   * 展示弹幕
   */
  showBarrage?: boolean
  /**
   * 弹幕
   */
  danmus: Message[]

  /**
   * 观看次数
   */
  visits?: number
  /**
   * 点赞数
   */
  videoLikeNumber?: number
  /**
   * 留言数
   */
  leaveMessageNumber?: number
  /**
   * 创建时间
   */
  createTime?: string
}
