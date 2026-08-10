package com.pnkx.domain.po;

import lombok.Data;

/**
 * PxArticleType
 *
 * @author 裴浩宇
 * @version 1.0
 * @date 2023/8/18 17:51
 * @description 文章分类实体
 */
@Data
public class PxArticleType {

    /**
     * 分类code
     */
    private String typeCode;

    /**
     * 分类名
     */
    private String typeName;

    /**
     * 文章数量
     */
    private Integer articleNumber;
}
