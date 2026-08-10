export interface Wallpaper {
  id: number
  name: string
  url: string
  thumbnail: string | null
  folder: number | null
  likeCount: number
  width: number | null
  height: number | null
  createTime: string
  updateTime: string
}

export interface WallpaperPageQuery {
  pageNum: number
  pageSize: number
  sort: 'time' | 'like' | 'name'
}
