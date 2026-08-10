import { getYiYan2 } from '@/apis/poetry'
import type { BlogConfig } from '@/types/home'

export const useBlogStore = defineStore('blog', () => {
  // 博客基本信息
  const blogConfig = ref<BlogConfig | null>(null)
  // 文章数
  const articleNumber = ref(0)
  // 分类数
  const articleTypeNumber = ref(0)
  // 访问数
  const visitsNumber = ref(0)
  // 菜单
  const menuList = ref([
    {
      icon: 'noto:house-with-garden',
      text: '首页',
      path: '/',
      class: 'menu-item-home'
    },
    {
      icon: 'flat-color-icons:calendar',
      text: '归档',
      path: '/archives',
      class: 'menu-item-archives'
    },
    {
      icon: 'icon-park:category-management',
      text: '分类',
      path: '/type',
      class: 'menu-item-type'
    },
    {
      icon: 'icon-park:message',
      text: '留言',
      path: '/message',
      class: ''
    },
    {
      icon: 'icon-park:video',
      text: '视频',
      path: '/video',
      class: ''
    },
    {
      icon: 'icon-park:share',
      text: '分享',
      path: '/share',
      class: ''
    },
    {
      icon: 'icon-park:friends-circle',
      text: '友链',
      path: '/link',
      class: 'menu-item-friends'
    },
    {
      icon: 'flat-color-icons:gallery',
      text: '图库',
      path: '/album',
      class: ''
    }
  ])
  // banner
  const bannerList = ref([
    {
      title: '新闻资讯',
      describe: '覆盖多元热点，直击新闻核心',
      link: 'https://news.pnkx.top:8/',
      bgSrc: 'https://ftp.pnkx.top:8/ftp/gallery/1044978-8dcaca6093474f12bb3de9ecac1e9b97jpg.jpg'
    },
    {
      title: '在线聊天室',
      describe: '这里可以畅所欲言',
      link: 'https://chat.pnkx.top:8/#/chat/channel/1',
      bgSrc: 'https://ftp.pnkx.top:8/ftp/gallery/72.jpg'
    },
    {
      title: '工具箱',
      describe: '海量实用工具，适配多场景高效提效',
      link: 'https://tools.pnkx.top:8/',
      bgSrc: 'https://ftp.pnkx.top:8/ftp/gallery/1049165-48jpg.jpg'
    }
  ])
  // 一言
  const yiYan = ref('梦想是一个天真的词，实现梦想是一个残酷的词')

  // 查看博客信息
  // eslint-disable-next-line require-await
  async function blogInfoData() {
    const { home } = useApi()
    home.getBlogInfo({ lazy: true }).then((res) => {
      if (res.data.value) {
        visitsNumber.value = res.data.value.data.visitsNumber
        articleNumber.value = res.data.value.data.articleNumber
        articleTypeNumber.value = res.data.value.data.articleTypeNumber
        const blogConfigObject = {}
        res.data.value.data.blogConfig.map(
          // eslint-disable-next-line array-callback-return
          (item: { configKey: string | number; configValue: any }) => {
            // @ts-ignore
            blogConfigObject[item.configKey] = item.configValue
          }
        )
        // @ts-ignore
        blogConfig.value = blogConfigObject
      }
    })
  }
  async function setYiYan() {
    // 每日一言
    const { data } = await getYiYan2()
    if (data.value) {
      // const yiyanObj = data.value as string
      yiYan.value = data.value.hitokoto
    }
  }

  return {
    blogConfig,
    menuList,
    bannerList,
    articleNumber,
    articleTypeNumber,
    visitsNumber,
    yiYan,
    blogInfoData,
    setYiYan
  }
})

// console.log(import.meta.hot)
if (import.meta.hot) import.meta.hot.accept(acceptHMRUpdate(useBlogStore, import.meta.hot))
