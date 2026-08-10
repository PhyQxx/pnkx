enum Api {
  likeArticle = '/client/like/likeArticle/',
  likeComment = '/client/like/likeComment/',
}

/**
 * 文章点赞
 * @param id
 */
export function likeArticle(id: number) {
  return useHttp.get<boolean>(Api.likeArticle + id)
}

/**
 * 评论点赞
 * @param id
 */
export function likeComment(id: number) {
  return useHttp.get<boolean>(Api.likeComment + id)
}
