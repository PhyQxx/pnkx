import type { Video } from '@/types/video'
import type { HttpOption } from '@/composables/useHttp'
import type { PageQuery } from '@/types'

enum Api {
    videoList = '/client/video/list',
    getVideo = '/client/video/'
}

/**
 * 查看视频列表
 * @param params 查询条件
 * @param option useFetch 配置选项
 * @returns 视频列表
 */
export function getVideoList(params: PageQuery, option?: HttpOption<Video[]>) {
    return useHttp.get<Video[]>(Api.videoList, params, option)
}

/**
 * 查看视频
 * @param id 视频id
 * @param option useFetch 配置选项
 * @returns 视频列表
 */
export function getVideo(id: number, option?: HttpOption<Video>) {
    return useHttp.get<Video>(Api.getVideo + id, option)
}
