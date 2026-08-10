import type { PageListStore } from '@/types'

export const useImageStore = defineStore('image', () => {
  const pageList = ref<PageListStore>({
    home: 'https://t.mwm.moe/fj/',
    message: 'https://ftp.pnkx.top:8/ftp/gallery/20.jpg',
    link: 'https://ftp.pnkx.top:8/ftp/gallery/28.png',
    archive: 'https://ftp.pnkx.top:8/ftp/gallery/22.jpg',
    video: 'https://ftp.pnkx.top:8/ftp/gallery/24.jpg',
    share: 'https://ftp.pnkx.top:8/ftp/gallery/28.png',
    login: 'https://ftp.pnkx.top:8/ftp/gallery/56.png',
    user: 'https://ftp.pnkx.top:8/ftp/gallery/68.jpg',
    album: 'https://ftp.pnkx.top:8/ftp/gallery/25.jpg',
    type: 'https://ftp.pnkx.top:8/ftp/gallery/23.jpg',
  })
  const randomImage = ref([
    'https://api.ghser.com/random/api.php',
    'https://t.mwm.moe/fj/',
    'https://imgapi.xl0408.top/index.php',
    'https://service-5z0sdahv-1306777571.sh.apigw.tencentcs.com/release/'
  ])
  const videoUrl = ref(['https://ftp.pnkx.top:8/ftp/pnkx/video/007.mp4'])
  const colors = ref([
    '#ee7752',
    '#e73c7e',
    '#23a6d5',
    '#23d5ab',
    '#23d5ab',
    '#ff4757',
    '#ff7f50',
    '#eccc68',
    '#7bed9f',
    '#2ed573',
    '#1e90ff',
    '#5352ed',
    '#2f3542',
    '#fd79a8',
    '#6c5ce7',
    '#63cdda',
    '#2bcbba'
  ])

  function togglePage(page: keyof PageListStore, url: string) {
    pageList.value[page] = url
  }

  return { pageList, randomImage, videoUrl, colors, togglePage }
})

// console.log(import.meta.hot)
if (import.meta.hot) import.meta.hot.accept(acceptHMRUpdate(useImageStore, import.meta.hot))
