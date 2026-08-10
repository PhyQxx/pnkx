package com.pnkx.domain.po;

import com.pnkx.common.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * AI操作审计日志对象 px_ai_operation_log
 *
 * @author PHY
 * @date 2026-05-19
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class PxAiOperationLog extends BaseEntity {
    private static final long serialVersionUID = 1L;

    /** 主键 */
    private Long id;

    /** 请求ID */
    private String requestId;

    /** 用户ID */
    private String userId;

    /** 用户输入 */
    private String question;

    /** 识别意图 */
    private String intent;

    /** 意图置信度 */
    private BigDecimal confidence;

    /** 模型ID */
    private Long modelId;

    /** 模型Key */
    private String modelKey;

    /** 是否流式请求 */
    private Integer isStream;

    /** 是否写库 */
    private Integer isWrite;

    /** 写库状态: draft, confirmed, cancelled, failed, none */
    private String writeStatus;

    /** 解析后的意图JSON或草稿JSON */
    private String parsedJson;

    /** 错误信息 */
    private String errorMsg;

    /** 耗时毫秒 */
    private Long durationMs;
}
