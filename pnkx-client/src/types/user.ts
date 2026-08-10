/**
 * 登录信息
 */
export interface LoginForm {
  /**
   * 用户名
   */
  userName: string
  /**
   * 密码
   */
  password: string
}

/**
 * 用户信息
 */
export interface UserForm {
  /**
   * 用户名
   */
  userName: string
  /**
   * 密码
   */
  password: string
}

/**
 * 用户登录信息
 */
export interface UserInfo {
  /**
   * 用户id
   */
  userId: number
  /**
   * 头像
   */
  avatar: string
  /**
   * 昵称
   */
  nickName: string
  /**
   * 用户名
   */
  userName: string
  /**
   * 邮箱
   */
  email: string
  /**
   * 个人网站
   */
  webSite: string
  /**
   * 个人简介
   */
  intro: string
  /**
   * 登录方式
   */
  loginType: number
  /**
   * 文章点赞集合
   */
  articleLikeSet: number[]
  /**
   * 评论点赞集合
   */
  commentLikeSet: number[]
}

/**
 * 用户信息
 */
export interface UserInfoForm {
  /**
   * 昵称
   */
  nickName: string
  /**
   * 个人网站
   */
  webSite: string
  /**
   * 个人简介
   */
  intro: string
}

/**
 * 邮箱
 */
export interface EmailForm {
  /**
   * 邮箱号
   */
  email: string
  /**
   * 验证码
   */
  code: string
}
