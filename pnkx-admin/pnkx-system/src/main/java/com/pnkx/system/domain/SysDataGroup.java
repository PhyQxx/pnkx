package com.pnkx.system.domain;

import com.pnkx.common.annotation.Excel;
import com.pnkx.common.core.domain.BaseEntity;
import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * 数据权限群组 px_data_group
 * <p>
 * 多对多群组：组内成员彼此可见数据（自己的 + 组内其他成员的）。
 *
 * @author pnkx
 */
@Data
public class SysDataGroup extends BaseEntity {
    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    private Long id;

    /**
     * 群组名称
     */
    @Excel(name = "群组名称")
    @NotBlank(message = "群组名称不能为空")
    @Size(min = 0, max = 64, message = "群组名称长度不能超过64个字符")
    private String groupName;

    /**
     * 群组编码
     */
    @Excel(name = "群组编码")
    private String groupCode;

    /**
     * 状态（0正常 1停用）
     */
    @Excel(name = "状态", readConverterExp = "0=正常,1=停用")
    private String status;

    /**
     * 成员用户ID列表（新增/编辑时传入，非数据库字段）
     */
    private List<Long> userIds;
}
