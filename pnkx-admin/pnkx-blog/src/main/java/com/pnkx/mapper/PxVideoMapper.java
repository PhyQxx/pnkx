package com.pnkx.mapper;

import com.pnkx.domain.po.PxVideo;

import java.util.List;

/**
 * 视频模块Mapper接口
 *
 * @author 裴大头
 * @date 2023-04-19
 */
public interface PxVideoMapper {
    /**
     * 查询视频模块
     *
     * @param id 视频模块ID
     * @return 视频模块
     */
    PxVideo selectPxVideoById(Long id);

    /**
     * 查看视频 +1
     *
     * @param id 视频模块ID
     * @return 视频模块
     */
    int visitVideo(Long id);

    /**
     * 查询视频模块列表
     *
     * @param pxVideo 视频模块
     * @return 视频模块集合
     */
    List<PxVideo> selectPxVideoList(PxVideo pxVideo);

    /**
     * 新增视频模块
     *
     * @param pxVideo 视频模块
     * @return 结果
     */
    int insertPxVideo(PxVideo pxVideo);

    /**
     * 修改视频模块
     *
     * @param pxVideo 视频模块
     * @return 结果
     */
    int updatePxVideo(PxVideo pxVideo);

    /**
     * 删除视频模块
     *
     * @param id 视频模块ID
     * @return 结果
     */
    int deletePxVideoById(Long id);

    /**
     * 批量删除视频模块
     *
     * @param ids 需要删除的数据ID
     * @return 结果
     */
    int deletePxVideoByIds(Long[] ids);

    /**
     * 获取视频标签列表
     *
     * @return
     */
    List<String> getLabelList();
}
