/**
 * 文章
 */
export interface Article {
  /**
   * 文章id
   */
  id: number
  /**
   * 是否置顶 (0否 1是)
   */
  isTop: number
  /**
   * 文章缩略图
   */
  cover: string
  /**
   * 文章标题
   */
  title: string
  /**
   * 文章内容
   */
  content: string
  /**
   * 文章内容（富文本格式）
   */
  richText: string
  /**
   * 文章标签
   */
  tag: string
  /**
   * 文章类型
   */
  type: number
  /**
   * 文章类型名称
   */
  typeName: string
  /**
   * 浏览量
   */
  visitsNumber: number
  /**
   * 点赞量
   */
  likeNumber: number
  /**
   * 留言数
   */
  leaveMessageNumber: number
  /**
   * 发表时间
   */
  createTime: string
  /**
   * 上一篇文章
   */
  lastArticle: Article
  /**
   * 下一篇文章
   */
  nextArticle: Article
  /**
   * 更新时间
   */
  updateTime: string
  /**
   * 作者昵称
   */
  nickName: string
}

/**
 * 文章搜索
 */
export interface SearchArticle {
  /**
   * 文章id
   */
  id: number
  /**
   * 文章标题
   */
  title: string
  /**
   * 文章内容
   */
  content: string
}
