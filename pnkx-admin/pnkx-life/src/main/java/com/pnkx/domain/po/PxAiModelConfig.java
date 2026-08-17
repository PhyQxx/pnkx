package com.pnkx.domain.po;

import com.pnkx.common.core.domain.BaseEntity;
import lombok.Data;

/**
 * AI模型配置对象 px_ai_model_config
 *
 * @author PHY
 */
@Data
public class PxAiModelConfig extends BaseEntity {
    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    private Long id;

    /**
     * 模型名称（如 DeepSeek、OpenAI）
     */
    private String modelName;

    /**
     * 模型标识（如 deepseek-chat、gpt-4o）
     */
    private String modelKey;

    /**
     * API 地址
     */
    private String baseUrl;

    /**
     * API Key
     */
    private String apiKey;

    /**
     * 是否默认 0=否 1=是
     */
    private String isDefault;

    /**
     * 是否启用 0=否 1=是
     */
    private String isEnabled;

    /**
     * 温度参数
     */
    private Double temperature;

    /**
     * 是否开启思考模式（null=不设置，true=开启，false=关闭）
     * 仅对支持思考的模型生效，如智谱 GLM-4.5/4.7/5；为 null 时不向请求体注入 thinking 字段
     */
    private Boolean thinking;

    /**
     * 排序
     */
    private Integer sort;

    /**
     * 删除标志
     */
    private Boolean delFlag;
}
