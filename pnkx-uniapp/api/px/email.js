import request from '@/utils/request'

// 发送简单邮件
export function sendSimpleEmail(query) {
  return request({
    url: '/email/admin/sendSimpleEmail',
    method: 'post',
    data: query
  })
}

// 发送简单邮件
export function sendHtmlEmail(query) {
  return request({
    url: '/email/admin/sendHtmlEmail',
    method: 'post',
    data: query
  })
}
