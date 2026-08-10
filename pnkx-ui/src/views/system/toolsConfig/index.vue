<!--
 * @File: toolsConfig
 * @Author: PHY
 * @Description: 小程序工具台配置（系统配置-小程序配置）
 *               工具列表存储于 sys_config 表，config_key = sys.tools.list，value 为 JSON 数组
-->
<template>
    <div class="app-container">
        <el-card shadow="never" class="config-card">
            <template #header>
                <div class="card-header">
                    <div class="card-header-left">
                        <el-icon class="header-icon"><Tools/></el-icon>
                        <span class="header-title">小程序工具台配置</span>
                    </div>
                    <div class="card-header-right">
                        <el-button type="primary" icon="Plus" size="small" @click="handleAdd">添加工具</el-button>
                        <el-button type="success" icon="Check" size="small" :loading="saving" @click="handleSave">保存配置</el-button>
                    </div>
                </div>
            </template>

            <el-alert
                title="此处的工具列表将展示在小程序「工具箱」首页。每个工具需配置图标、名称、描述、类型（页面跳转/网页链接）及目标地址。"
                type="info"
                :closable="false"
                show-icon
                style="margin-bottom: 16px"
            />

            <el-table
                v-loading="loading"
                :data="toolList"
                border
                empty-text="暂无工具，点击右上角「添加工具」"
            >
                <el-table-column label="排序" width="70" align="center">
                    <template v-slot="scope">
                        <span>{{ scope.$index + 1 }}</span>
                    </template>
                </el-table-column>
                <el-table-column label="图标预览" width="90" align="center">
                    <template v-slot="scope">
                        <div class="icon-preview" :style="{ background: scope.row.color + '1f' }">
                            <svg-icon :icon-class="scope.row.icon || 'tool'" :style="{ color: scope.row.color }"/>
                        </div>
                    </template>
                </el-table-column>
                <el-table-column label="名称" prop="name" align="center" min-width="120"/>
                <el-table-column label="描述" prop="desc" align="center" min-width="180" :show-overflow-tooltip="true"/>
                <el-table-column label="类型" width="100" align="center">
                    <template v-slot="scope">
                        <el-tag :type="scope.row.type === 'web' ? 'warning' : 'success'" size="small">
                            {{ scope.row.type === 'web' ? '网页' : '页面' }}
                        </el-tag>
                    </template>
                </el-table-column>
                <el-table-column label="跳转地址" prop="target" align="center" min-width="200" :show-overflow-tooltip="true"/>
                <el-table-column label="启用" width="90" align="center">
                    <template v-slot="scope">
                        <el-switch v-model="scope.row.enabled" @change="handleEnabledChange"/>
                    </template>
                </el-table-column>
                <el-table-column label="操作" width="180" align="center">
                    <template v-slot="scope">
                        <el-button size="small" type="text" icon="Top" :disabled="scope.$index === 0" @click="moveUp(scope.$index)">上移</el-button>
                        <el-button size="small" type="text" icon="Bottom" :disabled="scope.$index === toolList.length - 1" @click="moveDown(scope.$index)">下移</el-button>
                        <el-button size="small" type="text" icon="Edit" @click="handleEdit(scope.row, scope.$index)">编辑</el-button>
                        <el-button size="small" type="text" icon="Delete" style="color: #f56c6c" @click="handleRemove(scope.$index)">删除</el-button>
                    </template>
                </el-table-column>
            </el-table>

            <!-- 预览区 -->
            <div class="preview-section">
                <div class="preview-title">小程序预览效果</div>
                <div class="phone-frame">
                    <div class="phone-screen">
                        <div class="phone-header">工具箱</div>
                        <div class="phone-subtitle">实用小工具集合，免登录即用</div>
                        <div class="phone-list" v-if="toolList.length">
                            <div v-for="(tool, index) in toolList" :key="index" class="phone-item" :class="{ 'phone-item--disabled': tool.enabled === false }">
                                <div class="phone-icon" :style="{ background: tool.color + '1f' }">
                                    <svg-icon :icon-class="tool.icon || 'tool'" :style="{ color: tool.color }"/>
                                </div>
                                <div class="phone-info">
                                    <div class="phone-name">{{ tool.name }}</div>
                                    <div class="phone-desc">{{ tool.desc }}</div>
                                </div>
                                <el-icon class="phone-arrow"><ArrowRight/></el-icon>
                            </div>
                        </div>
                        <div v-else class="phone-empty">暂无工具</div>
                    </div>
                </div>
            </div>
        </el-card>

        <!-- 添加/编辑对话框 -->
        <el-dialog :title="dialog.title" v-model="dialog.open" width="560px" append-to-body>
            <el-form ref="toolForm" :model="dialog.form" :rules="dialog.rules" label-width="90px">
                <el-form-item label="工具ID" prop="id">
                    <el-input v-model="dialog.form.id" placeholder="唯一标识，如 wallpaper、blog（英文）"/>
                </el-form-item>
                <el-form-item label="名称" prop="name">
                    <el-input v-model="dialog.form.name" placeholder="如：壁纸下载"/>
                </el-form-item>
                <el-form-item label="描述" prop="desc">
                    <el-input v-model="dialog.form.desc" placeholder="一句话描述"/>
                </el-form-item>
                <el-form-item label="图标" prop="icon">
                    <el-input v-model="dialog.form.icon" placeholder="svg 图标名，如：picture、article"/>
                    <div class="form-tip">
                        可用图标：picture（壁纸）、article（文章）、tool、setting、user、edit、message、log 等
                    </div>
                </el-form-item>
                <el-form-item label="主题色" prop="color">
                    <el-color-picker v-model="dialog.form.color"/>
                    <span class="color-value">{{ dialog.form.color }}</span>
                </el-form-item>
                <el-form-item label="跳转类型" prop="type">
                    <el-radio-group v-model="dialog.form.type">
                        <el-radio label="page">页面跳转（小程序内部页面）</el-radio>
                        <el-radio label="web">网页链接（外部网页/博客后台）</el-radio>
                    </el-radio-group>
                </el-form-item>
                <el-form-item label="跳转地址" prop="target">
                    <el-input
                        v-model="dialog.form.target"
                        :placeholder="dialog.form.type === 'web' ? 'https://pnkx.top' : '/pages_life/wallpaper/index'"
                    />
                </el-form-item>
                <el-form-item label="是否启用" prop="enabled">
                    <el-switch v-model="dialog.form.enabled"/>
                    <span class="form-tip" style="margin-left: 12px">{{ dialog.form.enabled ? '启用：小程序工具台展示该工具' : '禁用：小程序工具台不展示该工具' }}</span>
                </el-form-item>
            </el-form>
            <template #footer>
                <div class="dialog-footer">
                    <el-button @click="dialog.open = false">取 消</el-button>
                    <el-button type="primary" @click="submitDialog">确 定</el-button>
                </div>
            </template>
        </el-dialog>
    </div>
</template>

<script>
import { listConfig, updateConfig, addConfig } from '@/api/system/config'
import { Tools, ArrowRight } from '@element-plus/icons-vue'

const CONFIG_KEY = 'sys.tools.list'

export default {
    name: 'ToolsConfig',
    components: {Tools, ArrowRight},
    data() {
        return {
            loading: false,
            saving: false,
            // 已存在的配置ID（编辑用），新增时为空
            configId: null,
            toolList: [],
            dialog: {
                open: false,
                title: '',
                editIndex: -1,
                form: {},
                rules: {
                    id: [{required: true, message: '请输入工具ID', trigger: 'blur'}],
                    name: [{required: true, message: '请输入名称', trigger: 'blur'}],
                    type: [{required: true, message: '请选择跳转类型', trigger: 'change'}],
                    target: [{required: true, message: '请输入跳转地址', trigger: 'blur'}]
                }
            }
        }
    },
    created() {
        this.loadConfig()
    },
    methods: {
        /** 加载配置（用 listConfig 按 configKey 查询，拿到完整对象含 configId） */
        loadConfig() {
            this.loading = true
            listConfig({ configKey: CONFIG_KEY }).then(res => {
                const row = (res.rows || []).find(r => r.configKey === CONFIG_KEY)
                if (row) {
                    this.configId = row.configId
                    try {
                        this.toolList = JSON.parse(row.configValue || '[]')
                        // 兼容老数据：缺 enabled 字段的工具默认为启用
                        this.toolList.forEach(t => {
                            if (t.enabled === undefined || t.enabled === null) {
                                t.enabled = true
                            }
                        })
                    } catch (e) {
                        this.toolList = []
                    }
                } else {
                    // 配置不存在，初始化默认工具
                    this.configId = null
                    this.toolList = this.getDefaultTools()
                }
            }).catch(() => {
                this.configId = null
                this.toolList = this.getDefaultTools()
            }).finally(() => {
                this.loading = false
            })
        },

        /** 默认工具（首次配置时用） */
        getDefaultTools() {
            return [
                {
                    id: 'wallpaper',
                    name: '壁纸下载',
                    icon: 'picture',
                    color: '#38BDF8',
                    desc: '精选高清壁纸，一键保存到相册',
                    type: 'page',
                    target: '/pages_life/wallpaper/index',
                    enabled: true
                },
                {
                    id: 'blog',
                    name: '博客后台',
                    icon: 'article',
                    color: '#60A5FA',
                    desc: '管理文章、照片与博客内容',
                    type: 'web',
                    target: 'https://pnkx.top',
                    enabled: true
                }
            ]
        },

        /** 切换启用状态（仅提示需保存，不立即生效） */
        handleEnabledChange(val) {
            this.$message.success(val ? '已启用，记得保存' : '已禁用，记得保存')
        },

        /** 添加工具 */
        handleAdd() {
            this.dialog.title = '添加工具'
            this.dialog.editIndex = -1
            this.dialog.form = {
                id: '',
                name: '',
                desc: '',
                icon: 'tool',
                color: '#38BDF8',
                type: 'page',
                target: '',
                enabled: true
            }
            this.dialog.open = true
        },

        /** 编辑工具 */
        handleEdit(row, index) {
            this.dialog.title = '编辑工具'
            this.dialog.editIndex = index
            this.dialog.form = JSON.parse(JSON.stringify(row))
            this.dialog.open = true
        },

        /** 提交对话框 */
        submitDialog() {
            this.$refs.toolForm.validate(valid => {
                if (!valid) return
                if (this.dialog.editIndex >= 0) {
                    this.toolList.splice(this.dialog.editIndex, 1, {...this.dialog.form})
                } else {
                    this.toolList.push({...this.dialog.form})
                }
                this.dialog.open = false
                this.$message.success(this.dialog.editIndex >= 0 ? '已修改，记得保存' : '已添加，记得保存')
            })
        },

        /** 删除工具 */
        handleRemove(index) {
            this.$confirm('确定删除该工具吗？', '提示', {
                confirmButtonText: '确定',
                cancelButtonText: '取消',
                type: 'warning'
            }).then(() => {
                this.toolList.splice(index, 1)
                this.$message.success('已删除，记得保存')
            }).catch(() => {})
        },

        /** 上移 */
        moveUp(index) {
            if (index === 0) return
            const list = this.toolList
            ;[list[index - 1], list[index]] = [list[index], list[index - 1]]
            this.toolList = [...list]
        },

        /** 下移 */
        moveDown(index) {
            if (index === this.toolList.length - 1) return
            const list = this.toolList
            ;[list[index], list[index + 1]] = [list[index + 1], list[index]]
            this.toolList = [...list]
        },

        /** 保存配置到 sys_config */
        handleSave() {
            this.$confirm('确定保存工具台配置吗？保存后小程序立即生效。', '提示', {
                confirmButtonText: '确定',
                cancelButtonText: '取消',
                type: 'warning'
            }).then(() => {
                this.saving = true
                const payload = {
                    configName: '小程序工具台工具列表',
                    configKey: CONFIG_KEY,
                    configValue: JSON.stringify(this.toolList),
                    configType: 'miniprogram',
                    remark: '工具台首页展示的工具列表（JSON 数组）'
                }
                let promise
                if (this.configId) {
                    payload.configId = this.configId
                    promise = updateConfig(payload)
                } else {
                    promise = addConfig(payload)
                }
                promise.then(res => {
                    this.$message.success('保存成功')
                    // 新增后需要重新加载拿到 configId
                    this.loadConfig()
                }).catch(err => {
                    // 显式提示错误，避免请求失败时"无反应"
                    this.$message.error('保存失败：' + (err && err.message ? err.message : err))
                }).finally(() => {
                    this.saving = false
                })
            }).catch(() => {})
        }
    }
}
</script>

<style lang="scss" scoped>
.app-container {
    padding: 16px;
}

.config-card {
    .card-header {
        display: flex;
        align-items: center;
        justify-content: space-between;

        .card-header-left {
            display: flex;
            align-items: center;

            .header-icon {
                font-size: 18px;
                margin-right: 8px;
                color: var(--el-color-primary);
            }

            .header-title {
                font-size: 16px;
                font-weight: 600;
            }
        }
    }
}

.icon-preview {
    width: 40px;
    height: 40px;
    border-radius: 8px;
    display: flex;
    align-items: center;
    justify-content: center;
    margin: 0 auto;
    font-size: 20px;
}

.form-tip {
    font-size: 12px;
    color: #909399;
    line-height: 1.6;
    margin-top: 4px;
}

.color-value {
    margin-left: 12px;
    color: #909399;
    font-size: 13px;
}

/* 预览区 */
.preview-section {
    margin-top: 24px;

    .preview-title {
        font-size: 14px;
        font-weight: 600;
        color: #303133;
        margin-bottom: 12px;
    }
}

.phone-frame {
    width: 340px;
    margin: 0 auto;
    border: 8px solid #1a1a1a;
    border-radius: 32px;
    overflow: hidden;
    box-shadow: 0 8px 24px rgba(0, 0, 0, 0.15);
    background: #f7f8fb;
}

.phone-screen {
    min-height: 480px;
}

.phone-header {
    background: linear-gradient(135deg, #0EA5E9 0%, #38BDF8 100%);
    color: #fff;
    text-align: center;
    font-size: 16px;
    font-weight: 600;
    padding: 14px 0 10px;
}

.phone-subtitle {
    background: linear-gradient(135deg, #0EA5E9 0%, #38BDF8 100%);
    color: rgba(255, 255, 255, 0.85);
    text-align: center;
    font-size: 12px;
    padding: 0 0 16px;
}

.phone-list {
    padding: 16px 12px;
}

.phone-item {
    display: flex;
    align-items: center;
    background: #fff;
    border-radius: 12px;
    padding: 12px;
    margin-bottom: 10px;
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
}

.phone-item--disabled {
    opacity: 0.45;
    filter: grayscale(1);
    position: relative;
}

.phone-item--disabled::after {
    content: '已禁用';
    position: absolute;
    top: 50%;
    right: 12px;
    transform: translateY(-50%);
    font-size: 10px;
    color: #f56c6c;
    background: rgba(245, 108, 108, 0.12);
    padding: 2px 6px;
    border-radius: 4px;
}

.phone-icon {
    width: 40px;
    height: 40px;
    border-radius: 10px;
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 20px;
    margin-right: 12px;
    flex-shrink: 0;
}

.phone-info {
    flex: 1;
    min-width: 0;
}

.phone-name {
    font-size: 14px;
    font-weight: 600;
    color: #303133;
}

.phone-desc {
    font-size: 12px;
    color: #909399;
    margin-top: 3px;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
}

.phone-arrow {
    color: #c0c4cc;
    flex-shrink: 0;
}

.phone-empty {
    text-align: center;
    color: #c0c4cc;
    padding: 60px 0;
    font-size: 13px;
}
</style>
