import { createDiscreteApi } from 'naive-ui'
import type { FetchResponse, SearchParameters } from 'ofetch'
import type { UseFetchOptions } from '#app'
import type { Result } from '@/types'

type UrlType = string | Request | Ref<string | Request> | (() => string | Request)

export type HttpOption<T> = UseFetchOptions<Result<T>>

const { message } = createDiscreteApi(['message'])

const baseURL = import.meta.env.VITE_APP_BASE_URL as string

function handleError<T>(response: FetchResponse<Result<T>> & FetchResponse<ResponseType>) {
  const err = (text: string) => {
    // @ts-ignore
    console.log(response['[Symbol(Response internals)]'].url, response?._data?.msg ?? text)
    message.error(response?._data?.msg ?? text)
  }
  if (!response._data) {
    err('请求超时，服务器无响应！')
    return
  }

  const handleMap: { [key: number]: () => void } = {
    404: () => err('服务器资源不存在'),
    500: () => err('服务器内部错误'),
    403: () => err('没有权限访问该资源'),
    401: () => {
      err('登录状态已过期，需要重新登录')
      // TODO 跳转实际登录页
      // navigateTo('/')
    }
  }
  handleMap[response.status] ? handleMap[response.status]() : err('未知错误！')
}
// get方法传递数组形式参数
function paramsSerializer(params?: SearchParameters) {
  if (!params) return

  const query = useCloneDeep(params)
  Object.entries(query).forEach(([key, val]) => {
    if (typeof val === 'object' && Array.isArray(val) && val !== null) {
      query[`${key}[]`] = toRaw(val).map((v: any) => JSON.stringify(v))
      delete query[key]
    }
  })
  return query
}
function fetch<T>(url: UrlType, option: any) {
  return useFetch<Result<T>>(url, {
    // 请求拦截器
    onRequest({ options }) {
      // get方法传递数组形式参数
      options.params = paramsSerializer(options.params)
      // 添加baseURL,从环境变量里面取
      if (!baseURL) {
        options.baseURL = 'https://admin.pnkx.top:8/prod-api';
      } else {
        // 本地需要登录的用这个
        options.baseURL = baseURL
      }
      options.headers = new Headers(options.headers)
      const { tokenPrefix, getToken } = useToken()
      // 携带token
      if (getToken()) {
        options.headers.set('Authorization', tokenPrefix + getToken())
      }
    },
    // 响应拦截
    onResponse({ response }) {
      if (response.status !== 200) {
        message.error(response.statusText)
        handleError<T>(response)
        return Promise.reject(response._data)
      }
      // 在这里判断错误
      if (response._data.code === 401) {
        // token过期
        navigateTo('/login')
        return Promise.reject(response._data)
      } else if (response._data.code !== 200) {
        message.error(response._data.msg)
        handleError<T>(response)
        return Promise.reject(response._data)
      }
      // 成功返回
      return response._data
    },
    // 错误处理
    onResponseError({ response }) {
      handleError<T>(response)
      return Promise.reject(response?._data ?? null)
    },
    // 合并参数
    ...option
  })
}

// 自动导出
export const useHttp = {
  get: <T>(url: UrlType, params?: any, option?: HttpOption<T>) => {
    return fetch<T>(url, { method: 'get', params, ...option })
  },

  post: <T>(url: UrlType, body?: any, option?: HttpOption<T>) => {
    return fetch<T>(url, { method: 'post', body, ...option })
  },

  put: <T>(url: UrlType, body?: any, option?: HttpOption<T>) => {
    return fetch<T>(url, { method: 'put', body, ...option })
  },

  delete: <T>(url: UrlType, body?: any, option?: HttpOption<T>) => {
    return fetch<T>(url, { method: 'delete', body, ...option })
  }
}
