/**
 * 友链
 */
export interface Link {
  /**
   * 友链id
   */
  id?: number
  /**
   * 友链颜色
   */
  color?: string
  /**
   * 友链名称
   */
  title: string
  /**
   *友链头像
   */
  img: string
  /**
   * 友链地址
   */
  url: string
  /**
   * 友链介绍
   */
  remark: string
  /**
   * 友链状态
   */
  status: string
  /**
   * 友链邮箱
   */
  email: string
}
