import request from '@/utils/request'


// 图片上传
export function uploadImage(data) {
    return request({
        url: '/common/upload',
        method: 'post',
        data: data
    })
}
