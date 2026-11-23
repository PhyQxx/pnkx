import { createDiscreteApi } from 'naive-ui'
import type { SearchParameters } from 'ofetch'
import type { UseFetchOptions } from '#app'
import type { Result } from '@/types'
import { API_BASE_URL } from '@/utils/apiBase'

type UrlType = string | Request | Ref<string | Request> | (() => string | Request)

export type HttpOption<T> = UseFetchOptions<Result<T>>

// UI notifications are created only in the browser, never during SSR.
let message: ReturnType<typeof createDiscreteApi>['message'] | undefined
function notifyError(text: string) {
  if (!process.client) return
  message ??= createDiscreteApi(['message']).message
  message.error(text)
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
  const nuxtApp = useNuxtApp()
  const userStore = useUserStore()
  const { tokenPrefix, getToken, removeToken } = useToken()
  const route = useRoute()
  async function fail(status: number, text?: string) {
    if (status === 401) {
      const hadToken = !!getToken()
      removeToken()
      userStore.userInfo = null
      if (hadToken) {
        notifyError('登录状态已过期，需要重新登录')
        if (route.path !== '/login') {
          await nuxtApp.runWithContext(() => navigateTo('/login'))
        }
      }
    } else {
      notifyError(text || (({ 403: '没有权限访问该资源', 404: '服务器资源不存在', 500: '服务器内部错误' } as Record<number, string>)[status] ?? '请求失败，请稍后重试'))
    }
    throw new Error(text || `请求失败（${status}）`)
  }
  return useFetch<Result<T>>(url, {
    retry: 0,
    timeout: 15000,
    onRequest({ options }) {
      options.params = paramsSerializer(options.params)
      options.baseURL = API_BASE_URL
      options.headers = new Headers(options.headers)
      if (getToken()) options.headers.set('Authorization', tokenPrefix + getToken())
    },
    async onResponse({ response }) {
      // HTTP failures belong exclusively to onResponseError.
      if (!response.ok) return
      if (response._data?.code !== 200) {
        await fail(response._data?.code ?? 500, response._data?.msg)
      }
    },
    async onResponseError({ response }) {
      await fail(response.status, response._data?.msg)
    },
    onRequestError() {
      notifyError('网络连接失败或请求超时，请稍后重试')
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
