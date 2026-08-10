import type { UserInfo } from '@/types/user'

export const useUserStore = defineStore(
  'user',
  () => {
    // 用户信息
    const userInfo = ref<UserInfo | null>(null)

    // 获取用户信息
    async function getUserInfo() {
      const { user } = useApi()
      const { data } = await user.getUserInfo()
      if (data.value?.user) {
        userInfo.value = data.value.user
      }
    }
    // 清除用户信息
    function clearUserInfo() {
      const { removeToken } = useToken()

      userInfo.value = null
      removeToken()
    }

    // 文章点赞
    function setLikeArticle(articleId: number) {
      if (userInfo.value) {
        if (userInfo.value?.articleLikeSet?.includes(articleId)) {
          userInfo.value.articleLikeSet.splice(userInfo.value.articleLikeSet.indexOf(articleId), 1)
        } else {
          userInfo.value.articleLikeSet?.push(articleId)
        }
      }
    }
    // 评论点赞
    function setLikeComment(commentId: number) {
      if (userInfo.value) {
        if (userInfo.value.commentLikeSet?.includes(commentId)) {
          userInfo.value.commentLikeSet.splice(userInfo.value.commentLikeSet.indexOf(commentId), 1)
        } else {
          userInfo.value.commentLikeSet?.push(commentId)
        }
      }
    }

    return {
      userInfo,
      clearUserInfo,
      getUserInfo,
      setLikeArticle,
      setLikeComment
    }
  },
  {
    // 持久化
    persist: true
  }
)

if (import.meta.hot) import.meta.hot.accept(acceptHMRUpdate(useUserStore, import.meta.hot))
