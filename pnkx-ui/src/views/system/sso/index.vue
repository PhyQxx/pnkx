<template>
    <div class="app-container">
        <el-alert type="info" :closable="false" show-icon style="margin-bottom: 12px">
            接入系统以 <b>https://pnkx.top/.well-known/openid-configuration</b> 发现端点，走授权码流程登录；
            账号即本站账号，白名单未配置时所有用户可登录该应用。
        </el-alert>

        <el-row :gutter="10" class="mb8">
            <el-col :span="1.5">
                <el-button type="primary" icon="Plus" size="small" @click="handleAdd"
                           v-hasPermi="['system:sso:add']">新增应用
                </el-button>
            </el-col>
        </el-row>

        <el-table v-loading="loading" :data="clientList">
            <el-table-column label="应用名称" prop="clientName" min-width="120"/>
            <el-table-column label="client_id" prop="clientId" min-width="130">
                <template v-slot="scope">
                    <el-tag size="small">{{ scope.row.clientId }}</el-tag>
                </template>
            </el-table-column>
            <el-table-column label="类型" width="110" align="center">
                <template v-slot="scope">
                    <el-tag :type="scope.row.publicClient ? 'warning' : 'success'" size="small">
                        {{ scope.row.publicClient ? '公共(PKCE)' : '机密(Secret)' }}
                    </el-tag>
                </template>
            </el-table-column>
            <el-table-column label="回调地址" min-width="240">
                <template v-slot="scope">
                    <div v-for="uri in scope.row.redirectUris" :key="uri" class="redirect-uri">{{ uri }}</div>
                </template>
            </el-table-column>
            <el-table-column label="令牌有效期" width="130" align="center">
                <template v-slot="scope">
                    <span>{{ scope.row.accessTokenMinutes }}m / {{ scope.row.refreshTokenMinutes }}m</span>
                </template>
            </el-table-column>
            <el-table-column label="访问白名单" width="100" align="center">
                <template v-slot="scope">
                    <el-tag v-if="!scope.row.accessRules || scope.row.accessRules.length === 0" type="info" size="small">
                        所有人
                    </el-tag>
                    <span v-else>{{ scope.row.accessRules.length }} 条</span>
                </template>
            </el-table-column>
            <el-table-column label="操作" width="150" align="center">
                <template v-slot="scope">
                    <el-button size="small" type="text" icon="Edit" @click="handleUpdate(scope.row)"
                               v-hasPermi="['system:sso:edit']">修改
                    </el-button>
                    <el-button size="small" type="text" icon="Delete" @click="handleDelete(scope.row)"
                               v-hasPermi="['system:sso:remove']">删除
                    </el-button>
                </template>
            </el-table-column>
        </el-table>

        <!-- 新增/编辑弹窗 -->
        <el-dialog :title="form.id ? '修改应用' : '新增应用'" v-model="open" width="640px" append-to-body>
            <el-form ref="clientForm" :model="form" :rules="rules" label-width="110px">
                <el-form-item label="应用名称" prop="clientName">
                    <el-input v-model="form.clientName" placeholder="如：签到台"/>
                </el-form-item>
                <el-form-item label="client_id" prop="clientId">
                    <el-input v-model="form.clientId" :disabled="!!form.id" placeholder="字母数字下划线连字符"/>
                </el-form-item>
                <el-form-item label="客户端类型">
                    <el-radio-group v-model="form.publicClient">
                        <el-radio-button :label="false">机密（服务端，Secret）</el-radio-button>
                        <el-radio-button :label="true">公共（SPA/App，PKCE）</el-radio-button>
                    </el-radio-group>
                </el-form-item>
                <el-form-item :label="form.id ? '轮换密钥' : '密钥'" v-if="!form.publicClient" prop="clientSecret">
                    <el-input v-model="form.clientSecret" type="textarea" :rows="2"
                              :placeholder="form.id ? '留空不变更；输入新值则轮换（仅保存后返回一次）' : '至少 16 位强随机串，保存后仅返回一次'"/>
                </el-form-item>
                <el-form-item label="回调地址" prop="redirectUrisText">
                    <el-input v-model="form.redirectUrisText" type="textarea" :rows="3"
                              placeholder="每行一个，精确匹配，如 https://rr.pnkx.top:8/login/oauth2/code/pnkx"/>
                </el-form-item>
                <el-form-item label="授权范围">
                    <el-input v-model="form.scopesText" placeholder="额外 scope，逗号分隔（openid/profile 已固定包含）"/>
                </el-form-item>
                <el-form-item label="令牌有效期">
                    <el-input-number v-model="form.accessTokenMinutes" :min="5" :max="1440"/> 分钟 access
                    <span style="margin: 0 8px">/</span>
                    <el-input-number v-model="form.refreshTokenMinutes" :min="10" :max="43200"/> 分钟 refresh
                </el-form-item>
                <el-form-item label="访问白名单">
                    <div v-for="(rule, index) in form.accessRules" :key="index" class="access-rule-row">
                        <el-select v-model="rule.subjectType" style="width: 110px" size="small">
                            <el-option label="用户ID" value="user"/>
                            <el-option label="角色Key" value="role"/>
                        </el-select>
                        <el-input v-model="rule.subjectValue" placeholder="userId 或 roleKey" style="width: 180px"
                                  size="small"/>
                        <el-input v-model="rule.remark" placeholder="备注" style="flex: 1" size="small"/>
                        <el-button icon="Delete" circle size="small" type="text" @click="form.accessRules.splice(index, 1)"/>
                    </div>
                    <el-button size="small" icon="Plus" @click="form.accessRules.push({subjectType: 'user', subjectValue: '', remark: ''})">
                        添加规则
                    </el-button>
                    <div class="el-form-item-msg">不配置则所有本站用户可登录该应用；特权系统务必配置</div>
                </el-form-item>
            </el-form>
            <template #footer>
                <el-button type="primary" @click="submitForm">确 定</el-button>
                <el-button @click="cancel">取 消</el-button>
            </template>
        </el-dialog>

        <!-- 密钥一次性展示 -->
        <el-dialog title="请立即保存密钥" v-model="secretOpen" width="560px" append-to-body>
            <el-alert type="warning" :closable="false" show-icon style="margin-bottom: 12px">
                密钥仅此一次返回，刷新后不可再查；丢失只能重新轮换。
            </el-alert>
            <el-input :model-value="secretValue" type="textarea" :rows="3" readonly/>
        </el-dialog>
    </div>
</template>

<script>
import {addSsoClient, delSsoClient, listSsoClient, updateSsoClient} from "@/api/system/sso";

export default {
    name: "Sso",
    data() {
        return {
            loading: false,
            clientList: [],
            open: false,
            secretOpen: false,
            secretValue: "",
            form: {},
            rules: {
                clientId: [
                    {required: true, message: "client_id 不能为空", trigger: "blur"},
                    {pattern: /^[a-zA-Z0-9_-]{2,50}$/, message: "仅字母数字下划线连字符，2-50 位", trigger: "blur"}
                ],
                redirectUrisText: [{required: true, message: "至少一个回调地址", trigger: "blur"}]
            }
        };
    },
    created() {
        this.getList();
    },
    methods: {
        getList() {
            this.loading = true;
            listSsoClient().then(response => {
                this.clientList = response.data || [];
                this.loading = false;
            }).catch(() => {
                this.loading = false;
            });
        },
        reset() {
            this.form = {
                id: null,
                clientId: "",
                clientName: "",
                clientSecret: "",
                publicClient: false,
                redirectUrisText: "",
                scopesText: "",
                accessTokenMinutes: 30,
                refreshTokenMinutes: 1440,
                accessRules: []
            };
        },
        handleAdd() {
            this.reset();
            this.open = true;
        },
        handleUpdate(row) {
            this.reset();
            this.form.id = row.id;
            this.form.clientId = row.clientId;
            this.form.clientName = row.clientName;
            this.form.publicClient = row.publicClient;
            this.form.redirectUrisText = (row.redirectUris || []).join("\n");
            this.form.scopesText = (row.scopes || []).join(",");
            this.form.accessTokenMinutes = row.accessTokenMinutes;
            this.form.refreshTokenMinutes = row.refreshTokenMinutes;
            this.form.accessRules = (row.accessRules || []).map(r => ({
                subjectType: r.subjectType,
                subjectValue: r.subjectValue,
                remark: r.remark
            }));
            this.open = true;
        },
        submitForm() {
            this.$refs["clientForm"].validate(valid => {
                if (!valid) {
                    return;
                }
                const data = {
                    id: this.form.id,
                    clientId: this.form.clientId,
                    clientName: this.form.clientName,
                    clientSecret: this.form.clientSecret,
                    publicClient: this.form.publicClient,
                    redirectUris: (this.form.redirectUrisText || "").split("\n").map(s => s.trim()).filter(s => s),
                    scopes: (this.form.scopesText || "").split(",").map(s => s.trim()).filter(s => s),
                    accessTokenMinutes: this.form.accessTokenMinutes,
                    refreshTokenMinutes: this.form.refreshTokenMinutes,
                    accessRules: this.form.accessRules.filter(r => r.subjectValue)
                };
                const action = this.form.id ? updateSsoClient(data) : addSsoClient(data);
                action.then(response => {
                    this.$modal.msgSuccess(this.form.id ? "修改成功" : "新增成功");
                    this.open = false;
                    this.getList();
                    if (response.data) {
                        this.secretValue = String(response.data);
                        this.secretOpen = true;
                    }
                });
            });
        },
        handleDelete(row) {
            this.$modal.confirm('是否确认删除应用 "' + row.clientName + '"？其授权记录将一并清除').then(() => {
                return delSsoClient(row.id);
            }).then(() => {
                this.getList();
                this.$modal.msgSuccess("删除成功");
            }).catch(() => {
            });
        },
        cancel() {
            this.open = false;
            this.reset();
        }
    }
};
</script>

<style scoped>
.redirect-uri {
    font-size: 12px;
    color: #606266;
    line-height: 1.6;
    word-break: break-all;
}

.access-rule-row {
    display: flex;
    gap: 8px;
    margin-bottom: 8px;
    align-items: center;
}

.el-form-item-msg {
    font-size: 12px;
    color: #909399;
    line-height: 1.4;
    margin-top: 4px;
}
</style>
