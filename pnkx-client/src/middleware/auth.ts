export default defineNuxtRouteMiddleware(() => {
  if (process.client) {
    const { getToken } = useToken()
    if (!getToken()) {
      return navigateTo({ path: '/login' })
    }
  }
})
