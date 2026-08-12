package com.pnkx.service.impl;

import com.pnkx.common.annotation.DataScopeSelf;
import com.pnkx.common.utils.DateUtils;
import com.pnkx.common.utils.SecurityUtils;
import com.pnkx.domain.po.PxBookkeepingClassification;
import com.pnkx.mapper.PxBookkeepingClassificationMapper;
import com.pnkx.mapper.PxBookkeepingRecordMapper;
import com.pnkx.service.IPxBookkeepingClassificationService;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

@Service
public class PxBookkeepingClassificationServiceImpl implements IPxBookkeepingClassificationService {
    @Resource
    private PxBookkeepingClassificationMapper pxBookkeepingClassificationMapper;
    @Resource
    PxBookkeepingRecordMapper pxBookkeepingRecordMapper;

    /**
     * 查询账本分类
     *
     * @param id 账本分类ID
     * @return 账本分类
     */
    @Override
    public PxBookkeepingClassification selectPxBookkeepingClassificationById(Long id) {
        return pxBookkeepingClassificationMapper.selectPxBookkeepingClassificationById(id);
    }

    /**
     * 查询账本分类列表
     *
     * @param pxBookkeepingClassification 账本分类
     * @return 账本分类
     */
    @Override
    @DataScopeSelf
    public List<PxBookkeepingClassification> selectPxBookkeepingClassificationList(PxBookkeepingClassification pxBookkeepingClassification) {
        return pxBookkeepingClassificationMapper.selectPxBookkeepingClassificationList(pxBookkeepingClassification);
    }


    /**
     * 新增账本分类
     *
     * @param pxBookkeepingClassification 账本分类
     * @return 结果
     */
    @Override
    public int insertPxBookkeepingClassification(PxBookkeepingClassification pxBookkeepingClassification) {
        pxBookkeepingClassification.setCreateTime(DateUtils.getNowDate());
        pxBookkeepingClassification.setCreateBy(SecurityUtils.getUserId());
        return pxBookkeepingClassificationMapper.insertPxBookkeepingClassification(pxBookkeepingClassification);
    }

    /**
     * 修改账本分类
     *
     * @param pxBookkeepingClassification 账本分类
     * @return 结果
     */
    @Override
    public int updatePxBookkeepingClassification(PxBookkeepingClassification pxBookkeepingClassification) {
        pxBookkeepingClassification.setUpdateTime(DateUtils.getNowDate());
        pxBookkeepingClassification.setUpdateBy(SecurityUtils.getUserId());
        return pxBookkeepingClassificationMapper.updatePxBookkeepingClassification(pxBookkeepingClassification);
    }

    /**
     * 批量删除账本分类
     *
     * @param ids 需要删除的账本分类ID
     * @return 结果
     */
    @Override
    public int deletePxBookkeepingClassificationByIds(Long[] ids) {
        pxBookkeepingRecordMapper.deletePxBookkeepingRecordByTypes(ids);
        return pxBookkeepingClassificationMapper.deletePxBookkeepingClassificationByIds(ids);
    }

    /**
     * 删除账本分类信息
     *
     * @param id 账本分类ID
     * @return 结果
     */
    @Override
    public int deletePxBookkeepingClassificationById(Long id) {
        PxBookkeepingClassification pxBookkeepingClassification = pxBookkeepingClassificationMapper.selectPxBookkeepingClassificationById(id);
        if ("0".equals(pxBookkeepingClassification.getTypeLevel())) {
            List<Long> typeList = pxBookkeepingClassificationMapper.getTypeListByParentId(id);
            Long[] typeArray = typeList.toArray(new Long[0]);
            pxBookkeepingClassificationMapper.deletePxBookkeepingClassificationByIds(typeArray);
            pxBookkeepingRecordMapper.deletePxBookkeepingRecordByTypes(typeArray);
        }
        return pxBookkeepingClassificationMapper.deletePxBookkeepingClassificationById(id);
    }

    /**
     * 查询最近使用分类列表
     *
     * @param pxBookkeepingClassification 分类
     * @return 分类列表
     */
    @DataScopeSelf(alias = "r", onlySelf = true)
    @Override
    public List<PxBookkeepingClassification> getLatelyTypeList(PxBookkeepingClassification pxBookkeepingClassification) {
        List<PxBookkeepingClassification> latelyTypeList = pxBookkeepingClassificationMapper.getLatelyTypeList(pxBookkeepingClassification);
        // 根据id去重，返回前10条记录
        List<PxBookkeepingClassification> result = new ArrayList<>();
        for (PxBookkeepingClassification bookkeepingClassification : latelyTypeList) {
            if (!result.contains(bookkeepingClassification)) {
                result.add(bookkeepingClassification);
            }
            if (result.size() >= 10) {
                break;
            }
        }
        return result;
    }
}
