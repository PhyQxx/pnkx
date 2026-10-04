-- ============================================================
-- 参数配置：高德地图 Web 服务 key
-- 用于博客访客 / 留言 / 公告阅读的 IP 归属地定位（IpUtils.getRectangle）。
-- key 属敏感凭据不入仓库，默认空值；上线后在后台
-- 系统管理 → 参数设置 中填写实际值，修改即时生效（Redis 缓存同步刷新）。
-- 为空时相关定位自动跳过，仅记录 ip2region 的省市信息。
-- ============================================================
INSERT INTO `sys_config`(`config_name`, `config_key`, `config_value`, `config_type`, `create_by`, `create_time`, `remark`)
SELECT '高德地图Web服务Key', 'sys.amap.key', '', 'Y', 'admin', NOW(), '高德 IP 定位接口 key（https://console.amap.com 申请，Web服务类型）；为空时跳过高德定位'
WHERE NOT EXISTS (SELECT 1 FROM `sys_config` WHERE `config_key` = 'sys.amap.key');
