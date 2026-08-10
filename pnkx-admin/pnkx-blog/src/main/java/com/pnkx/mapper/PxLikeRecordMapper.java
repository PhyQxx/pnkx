package com.pnkx.mapper;

import com.pnkx.domain.po.PxLikeRecord;

import java.util.List;

/**
 * 点赞记录Mapper接口
 *
 * @author pnkx
 * @date 2023-08-25
 */
public interface PxLikeRecordMapper {
    /**
     * 查询点赞记录
     *
     * @param pxLikeRecord 点赞记录
     * @return 点赞记录
     */
    public PxLikeRecord selectPxLikeRecord(PxLikeRecord pxLikeRecord);

    /**
     * 查询点赞记录列表
     *
     * @param pxLikeRecord 点赞记录
     * @return 点赞记录集合
     */
    public List<PxLikeRecord> selectPxLikeRecordList(PxLikeRecord pxLikeRecord);

    /**
     * 新增点赞记录
     *
     * @param pxLikeRecord 点赞记录
     * @return 结果
     */
    public int insertPxLikeRecord(PxLikeRecord pxLikeRecord);

    /**
     * 删除点赞记录
     *
     * @param id 点赞记录ID
     * @return 结果
     */
    public int deletePxLikeRecordById(Long id);

    /**
     * 按内容+用户精确查询点赞记录（create_by 参与过滤）
     *
     * @param pxLikeRecord itemId/type/createBy
     * @return 点赞记录（唯一）
     */
    public PxLikeRecord selectLikeByUser(PxLikeRecord pxLikeRecord);

    /**
     * 按内容+用户精确删除点赞记录（create_by 参与过滤）
     *
     * @param pxLikeRecord itemId/type/createBy
     * @return 影响行数
     */
    public int deleteLikeByUser(PxLikeRecord pxLikeRecord);

    /**
     * 我的记录列表（点赞/收藏通用，按 create_by + type 过滤，最新优先）
     *
     * @param pxLikeRecord createBy/type
     * @return 记录集合
     */
    public List<PxLikeRecord> selectMyRecordList(PxLikeRecord pxLikeRecord);

    /**
     * 管理端：查询记录列表（按 type 过滤，可选按 create_by 过滤，JOIN 壁纸表 COALESCE 兜底）
     *
     * @param pxLikeRecord type/createBy
     * @return 记录集合
     */
    public List<PxLikeRecord> selectRecordListWithWallpaper(PxLikeRecord pxLikeRecord);

    /**
     * 管理端：操作记录中出现过的用户（按 type 过滤，用于用户筛选下拉）
     *
     * @param pxLikeRecord type
     * @return 用户集合（userId, nickName）
     */
    public List<java.util.Map<String, Object>> selectRecordUsers(PxLikeRecord pxLikeRecord);

    /**
     * 统计：按日期分组的点赞/收藏数量
     */
    public List<java.util.Map<String, Object>> selectRecordStatsByDate(java.util.Map<String, Object> params);

    /**
     * 统计：按文件夹分组的点赞/收藏数量
     */
    public List<java.util.Map<String, Object>> selectRecordStatsByFolder(java.util.Map<String, Object> params);
}
