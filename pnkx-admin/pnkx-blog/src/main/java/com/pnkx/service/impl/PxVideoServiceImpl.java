package com.pnkx.service.impl;

import com.pnkx.common.annotation.DataScopeSelf;
import com.pnkx.domain.po.PxVideo;
import com.pnkx.mapper.PxVideoMapper;
import com.pnkx.service.IPxVideoService;
import com.pnkx.common.utils.DateUtils;
import com.pnkx.common.utils.SecurityUtils;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

/**
 * 视频模块Service业务层处理
 *
 * @author 裴大头
 * @date 2023-04-19
 */
@Service
public class PxVideoServiceImpl implements IPxVideoService {
    @Resource
    private PxVideoMapper pxVideoMapper;

    /**
     * 查询视频模块
     *
     * @param id 视频模块ID
     * @return 视频模块
     */
    @Override
    public PxVideo selectPxVideoById(Long id) {
        pxVideoMapper.visitVideo(id);
        return pxVideoMapper.selectPxVideoById(id);
    }

    /**
     * 查询视频模块列表
     *
     * @param pxVideo 视频模块
     * @return 视频模块
     */
    @Override
    @DataScopeSelf
    public List<PxVideo> selectPxVideoList(PxVideo pxVideo) {
        return pxVideoMapper.selectPxVideoList(pxVideo);
    }

    /**
     * 新增视频模块
     *
     * @param pxVideo 视频模块
     * @return 结果
     */
    @Override
    public int insertPxVideo(PxVideo pxVideo) {
        pxVideo.setCreateTime(DateUtils.getNowDate());
        pxVideo.setCreateBy(SecurityUtils.getUserId());
        return pxVideoMapper.insertPxVideo(pxVideo);
    }

    /**
     * 修改视频模块
     *
     * @param pxVideo 视频模块
     * @return 结果
     */
    @Override
    public int updatePxVideo(PxVideo pxVideo) {
        pxVideo.setUpdateTime(DateUtils.getNowDate());
        pxVideo.setUpdateBy(SecurityUtils.getUserName());
        return pxVideoMapper.updatePxVideo(pxVideo);
    }

    /**
     * 批量删除视频模块
     *
     * @param ids 需要删除的视频模块ID
     * @return 结果
     */
    @Override
    public int deletePxVideoByIds(Long[] ids) {
        return pxVideoMapper.deletePxVideoByIds(ids);
    }

    /**
     * 删除视频模块信息
     *
     * @param id 视频模块ID
     * @return 结果
     */
    @Override
    public int deletePxVideoById(Long id) {
        return pxVideoMapper.deletePxVideoById(id);
    }

    /**
     * 获取视频标签列表
     *
     * @return
     */
    @Override
    public List<String> getLabelList() {
        List<String> result = new ArrayList<>();
        List<String> labelList = pxVideoMapper.getLabelList();
        labelList.forEach(item -> {
            String[] split = item.split(",");
            for (String s : split) {
                if (!result.contains(s)) {
                    result.add(s);
                }
            }
        });
        return result;
    }
}
