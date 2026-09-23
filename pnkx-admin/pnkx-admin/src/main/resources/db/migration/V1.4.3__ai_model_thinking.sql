-- ============================================================
-- V1.4.3  AI模型配置新增「思考模式」开关
--   thinking: NULL=不设置(沿用模型/厂商默认), 1=开启思考, 0=关闭思考
--   仅对支持思考的模型生效，如智谱 GLM-4.5 / GLM-4.7 / GLM-5
--   关闭思考可大幅降低意图识别、记账解析等结构化任务的延迟
-- ============================================================

ALTER TABLE `px_ai_model_config`
    ADD COLUMN `thinking` TINYINT(1) NULL DEFAULT NULL COMMENT '是否开启思考模式(null=不设置,1=开启,0=关闭)' AFTER `temperature`;

-- 可选：立即关闭当前默认模型(glm-5)的思考模式以加速结构化任务
-- UPDATE `px_ai_model_config` SET `thinking` = 0 WHERE `is_default` = '1' AND `del_flag` = 0;
