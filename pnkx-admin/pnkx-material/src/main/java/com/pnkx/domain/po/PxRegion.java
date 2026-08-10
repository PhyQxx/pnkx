package com.pnkx.domain.po;

import lombok.Data;

import javax.swing.plaf.synth.Region;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 地区类
 *
 * @author pnkx
 * @date 2023-12-06
 */
@Data
public class PxRegion implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 主键id
     */
    private Long id;

    /**
     * 名称
     */
    private String name;

    /**
     * 拼音首字母
     */
    private String py;

    /**
     * 父级id
     */
    private Long ssdqdm;

    /**
     * 层级
     */
    private Long lb;

    /**
     * 邮编
     */
    private Long yb;

    /**
     * 记录状态
     */
    private Long jlzt;

    /**
     * 下级地区
     */
    private List<PxRegion> children = new ArrayList<>();
}
