export const useToken = () => {
  // const TokenKey = useCookie('Token', { maxAge: 86400 })
  const TokenKey = useCookie('Token')

  // token前缀
  const tokenPrefix = 'Bearer '

  // 获取
  function getToken() {
    return TokenKey.value
  }

  // 设置
  function setToken(token: string) {
    TokenKey.value = token
  }

  // 删除
  function removeToken() {
    TokenKey.value = ''
  }

  return {
    tokenPrefix,
    getToken,
    setToken,
    removeToken
  }
}
