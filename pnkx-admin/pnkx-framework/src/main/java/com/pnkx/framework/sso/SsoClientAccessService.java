package com.pnkx.framework.sso;

import com.pnkx.common.core.domain.entity.SysRole;
import com.pnkx.common.core.domain.entity.SysUser;
import com.pnkx.system.service.ISysUserService;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * SSO 应用访问白名单
 *
 * <p>sso_client_access 无记录 = 所有 pnkx 用户可登录该应用；
 * 有记录 = 仅命中用户ID或角色Key的用户可登录。
 * 特权系统（运维工具、签到台等）必须配置，防止开放注册用户越权进入。
 *
 * @author phy
 */
@Service
public class SsoClientAccessService {

    /**
     * 白名单主体类型：指定用户
     */
    public static final String SUBJECT_USER = "user";

    /**
     * 白名单主体类型：指定角色
     */
    public static final String SUBJECT_ROLE = "role";

    @Resource
    private JdbcTemplate jdbcTemplate;

    @Resource
    private ISysUserService userService;

    /**
     * 校验用户能否登录指定应用
     *
     * @param clientId       应用 client_id
     * @param authentication SSO 会话中的认证信息（principal 名为 pnkx 用户名）
     */
    public boolean isAllowed(String clientId, org.springframework.security.core.Authentication authentication) {
        List<Map<String, Object>> rules = jdbcTemplate.queryForList(
                "SELECT subject_type, subject_value FROM sso_client_access WHERE client_id = ?", clientId);
        if (rules.isEmpty()) {
            return true;
        }
        SysUser user = userService.selectUserByUserName(authentication.getName());
        if (user == null) {
            return false;
        }
        for (Map<String, Object> rule : rules) {
            String type = String.valueOf(rule.get("subject_type"));
            String value = String.valueOf(rule.get("subject_value"));
            if (SUBJECT_USER.equals(type) && String.valueOf(user.getUserId()).equals(value)) {
                return true;
            }
            if (SUBJECT_ROLE.equals(type) && hasRole(user, value)) {
                return true;
            }
        }
        return false;
    }

    private boolean hasRole(SysUser user, String roleKey) {
        if (user.getRoles() == null) {
            return false;
        }
        for (SysRole role : user.getRoles()) {
            if (roleKey.equals(role.getRoleKey())) {
                return true;
            }
        }
        return false;
    }

    /**
     * 查询应用的白名单规则（管理界面用）
     */
    public List<SsoClientAccessRule> listRules(String clientId) {
        return jdbcTemplate.query(
                "SELECT id, client_id, subject_type, subject_value, remark FROM sso_client_access WHERE client_id = ? ORDER BY id",
                (rs, rowNum) -> {
                    SsoClientAccessRule rule = new SsoClientAccessRule();
                    rule.setId(rs.getLong("id"));
                    rule.setClientId(rs.getString("client_id"));
                    rule.setSubjectType(rs.getString("subject_type"));
                    rule.setSubjectValue(rs.getString("subject_value"));
                    rule.setRemark(rs.getString("remark"));
                    return rule;
                }, clientId);
    }

    /**
     * 全量替换应用的白名单规则（管理界面保存时调用）
     */
    public void replaceRules(String clientId, List<SsoClientAccessRule> rules) {
        Set<String> seen = new HashSet<>();
        List<SsoClientAccessRule> deduped = new ArrayList<>();
        if (rules != null) {
            for (SsoClientAccessRule rule : rules) {
                String key = rule.getSubjectType() + ":" + rule.getSubjectValue();
                if (seen.add(key)) {
                    rule.setClientId(clientId);
                    deduped.add(rule);
                }
            }
        }
        jdbcTemplate.update("DELETE FROM sso_client_access WHERE client_id = ?", clientId);
        for (SsoClientAccessRule rule : deduped) {
            jdbcTemplate.update(
                    "INSERT INTO sso_client_access(client_id, subject_type, subject_value, remark) VALUES (?, ?, ?, ?)",
                    clientId, rule.getSubjectType(), rule.getSubjectValue(), rule.getRemark());
        }
    }

    /**
     * 删除应用时清理规则
     */
    public void deleteRules(String clientId) {
        jdbcTemplate.update("DELETE FROM sso_client_access WHERE client_id = ?", clientId);
    }

    /**
     * 白名单规则（type: user=用户ID / role=角色Key）
     */
    public static class SsoClientAccessRule {
        private Long id;
        private String clientId;
        private String subjectType;
        private String subjectValue;
        private String remark;

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getClientId() {
            return clientId;
        }

        public void setClientId(String clientId) {
            this.clientId = clientId;
        }

        public String getSubjectType() {
            return subjectType;
        }

        public void setSubjectType(String subjectType) {
            this.subjectType = subjectType;
        }

        public String getSubjectValue() {
            return subjectValue;
        }

        public void setSubjectValue(String subjectValue) {
            this.subjectValue = subjectValue;
        }

        public String getRemark() {
            return remark;
        }

        public void setRemark(String remark) {
            this.remark = remark;
        }
    }
}
