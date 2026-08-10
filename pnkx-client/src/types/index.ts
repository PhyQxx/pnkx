import type { UserInfo } from '~/types/user'

export interface PageListStore {
  home: string
  message: string
  link: string
  archive: string
  video: string
  share: string
  login: string
  user: string
  album: string
  type: string
}

/**
 * 分页返回接口
 */
export interface PageResult<T> {
  /**
   * 分页结果
   */
  rows: T
  /**
   * 总数
   */
  total: number
}
/**
 * 结果返回接口
 */
export interface Result<T> {
  /**
   * 状态码
   */
  code: number
  /**
   * 返回信息
   */
  msg: string
  /**
   * 返回数据
   */
  data: T
  /**
   * 分页结果
   */
  rows: T
  /**
   * 总数
   */
  total: number
  /**
   * 用户信息接口返回值
   */
  user: UserInfo
  /**
   * 头像上传返回
   */
  imgUrl: string
  /**
   * token
   */
  token: string
}

/**
 * 分页参数
 */
export interface PageQuery {
  /**
   * 搜索关键字
   */
  searchValue?: any
  /**
   * 当前页
   */
  pageNum: number
  /**
   * 每页大小
   */
  pageSize: number
  /**
   * 文章类型
   */
  type?: any
  /**
   * 排序字段
   */
  orderByColumn?: string
  /**
   * 正序、倒序
   */
  isAsc?: string
  /**
   * 随机
   */
  isRandom?: boolean
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
  /**
   * 验证码
   */
  code: string
}

export interface Record {
  /**
   * 聊天记录id
   */
  id: number
  /**
   * 用户id
   */
  userId: number
  /**
   * 用户昵称
   */
  nickName: string
  /**
   * 用户头像
   */
  avatar: string
  /**
   * 聊天内容
   */
  content: string
  /**
   * 用户登录ip
   */
  ipAddress: string
  /**
   * ip来源
   */
  ipSource: string
  /**
   * 创建时间
   */
  createTime: string
}
