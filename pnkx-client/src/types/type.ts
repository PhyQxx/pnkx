/**
 * 分类
 */
export interface Type {
  /**
   * 分类id
   */
  typeCode: number
  /**
   * 分类名
   */
  typeName: string
  /**
   * 文章数量
   */
  articleNumber: number
}

/**
 * 分类VO
 */
export interface TypeVO {
  /**
   * 分类id
   */
  id: number
  /**
   * 分类名
   */
  typeName: string
}
