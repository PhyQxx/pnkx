<template>
    <div class="app-container">
        <el-form :model="queryParams" ref="queryForm" :inline="true" v-show="showSearch" label-width="68px">
            <el-form-item label="规则名称" prop="ruleName">
                <el-input
                    v-model="queryParams.ruleName"
                    placeholder="请输入规则名称"
                    clearable
                    size="small"
                    @keyup.enter.native="handleQuery"
                />
            </el-form-item>
            <el-form-item label="是否启用" prop="enabled">
                <el-select v-model="queryParams.enabled" placeholder="请选择是否启用" clearable size="small" style="width: 160px">
                    <el-option label="启用" :value="true" />
                    <el-option label="禁用" :value="false" />
                </el-select>
            </el-form-item>
            <el-form-item>
                <el-button type="primary" icon="Search" size="small" @click="handleQuery">搜索</el-button>
                <el-button icon="Refresh" size="small" @click="resetQuery">重置</el-button>
            </el-form-item>
        </el-form>

        <el-row :gutter="10" class="mb8">
            <el-col :span="1.5">
                <el-button
                    type="primary"
                                        icon="Plus"
                    size="small"
                    @click="handleAdd"
                    v-hasPermi="['system:rule:add']"
                >新增
                </el-button>
            </el-col>
            <el-col :span="1.5">
                <el-button
                    type="success"
                                        icon="Edit"
                    size="small"
                    :disabled="single"
                    @click="handleUpdate"
                    v-hasPermi="['system:rule:edit']"
                >修改
                </el-button>
            </el-col>
            <el-col :span="1.5">
                <el-button
                    type="danger"
                                        icon="Delete"
                    size="small"
                    :disabled="multiple"
                    @click="handleDelete"
                    v-hasPermi="['system:rule:remove']"
                >删除
                </el-button>
            </el-col>
            <el-col :span="1.5">
                <el-button
                    type="warning"
                                        icon="Download"
                    size="small"
                    @click="handleExport"
                    v-hasPermi="['system:rule:export']"
                >导出
                </el-button>
            </el-col>
            <right-toolbar :showSearch="showSearch" @queryTable="getList"></right-toolbar>
        </el-row>

        <el-table v-loading="loading" :data="ruleList" @selection-change="handleSelectionChange">
            <el-table-column type="selection" width="55" align="center"/>
            <el-table-column label="主键ID" align="center" prop="id"/>
            <el-table-column label="规则名称" align="center" prop="ruleName"/>
            <el-table-column label="关键词列表" align="center" prop="keywords" width="600">
                <template v-slot="scope">
                    <el-tag style="margin: 0 5px 5px 0" :type="['primary', 'info', 'success', 'danger', 'warning'][index % 5]" v-for="(keyword, index) in safeParseKeywords(scope.row.keywords)" :key="index">{{ keyword }}</el-tag>
                </template>
            </el-table-column>
            <el-table-column label="回复内容" align="center" prop="replyContent">
                <template v-slot="scope">
                    <el-tag v-if="scope.row.replyContents && scope.row.replyContents.length > 0" type="info">{{ scope.row.replyContents.length }} 条回复</el-tag>
                    <el-tag v-else type="danger">无回复</el-tag>
                </template>
            </el-table-column>
            <el-table-column label="是否精确匹配" align="center" prop="exactMatch">
                <template v-slot="scope">
                    <el-tag :type="scope.row.exactMatch ? 'success' : 'danger'">{{ scope.row.exactMatch ? '是' : '否' }}</el-tag>
                </template>
            </el-table-column>
            <el-table-column label="是否启用" align="center" prop="enabled">
                <template v-slot="scope">
                    <el-tag :type="scope.row.enabled ? 'success' : 'danger'">{{ scope.row.enabled ? '是' : '否' }}</el-tag>
                </template>
            </el-table-column>
            <el-table-column label="记录更新时间" align="center" prop="updateAt" width="180">
                <template v-slot="scope">
                    <span>{{ parseTime(scope.row.updateAt, '{y}-{m}-{d}') }}</span>
                </template>
            </el-table-column>
            <el-table-column label="操作" align="center" class-name="small-padding fixed-width">
                <template v-slot="scope">
                    <el-button
                        size="small"
                        type="text"
                        icon="Edit"
                        @click="handleUpdate(scope.row)"
                        v-hasPermi="['system:rule:edit']"
                    >修改
                    </el-button>
                    <el-button
                        size="small"
                        type="text"
                        icon="Delete"
                        @click="handleDelete(scope.row)"
                        v-hasPermi="['system:rule:remove']"
                    >删除
                    </el-button>
                </template>
            </el-table-column>
        </el-table>

        <pagination
            v-show="total>0"
            :total="total"
            v-model:page="queryParams.pageNum"
            v-model:limit="queryParams.pageSize"
            @pagination="getList"
        />

        <!-- 添加或修改自定义回复规则对话框 -->
        <el-dialog :title="title" v-model="open" width="80vw" top="5vh" append-to-body>
            <el-form ref="form" :model="form" :rules="rules" label-width="8rem">
                <el-form-item label="规则名称" prop="ruleName">
                    <el-input v-model="form.ruleName" placeholder="请输入规则名称"/>
                </el-form-item>
                <el-form-item label="关键词列表" prop="keywords">
                    <el-select
                        v-model="form.keywordsList"
                        multiple
                        filterable
                        allow-create
                        default-first-option
                        style="width: 100%"
                        placeholder="请选择关键词">
                    </el-select>
                </el-form-item>
                <el-form-item label="回复内容列表">
                    <div class="reply-list-container">
                        <div class="reply-list-header">
                            <span>回复列表 ({{ form.replyContents ? form.replyContents.length : 0 }})</span>
                            <el-button type="primary" size="small" icon="Plus" @click="addReplyItem">添加回复</el-button>
                        </div>
                        <el-table :data="form.replyContents" size="small" border class="reply-table">
                            <el-table-column label="序号" type="index" width="60" align="center"/>
                            <el-table-column label="回复内容" prop="content" min-width="300">
                                <template v-slot="scope">
                                    <div class="reply-content" v-html="renderMarkdown(scope.row.content)"></div>
                                </template>
                            </el-table-column>
                            <el-table-column label="权重" prop="weight" width="100" align="center">
                                <template v-slot="scope">
                                    <el-input-number v-model="scope.row.weight" :min="1" :max="100" size="small"/>
                                </template>
                            </el-table-column>
                            <el-table-column label="操作" width="120" align="center">
                                <template v-slot="scope">
                                    <el-button size="small" type="text" icon="Edit" @click="editReplyItem(scope.row, scope.$index)">编辑</el-button>
                                    <el-button size="small" type="text" icon="Delete" @click="removeReplyItem(scope.$index)" style="color: #F56C6C;">删除</el-button>
                                </template>
                            </el-table-column>
                        </el-table>
                    </div>
                </el-form-item>
                <el-form-item label="是否启用" prop="enabled">
                    <el-radio-group v-model="form.enabled">
                        <el-radio :label="true">是</el-radio>
                        <el-radio :label="false">否</el-radio>
                    </el-radio-group>
                </el-form-item>
                <el-form-item label="是否精确匹配" prop="exactMatch">
                    <el-radio-group v-model="form.exactMatch">
                        <el-radio :label="true">是</el-radio>
                        <el-radio :label="false">否</el-radio>
                    </el-radio-group>
                </el-form-item>
                <el-form-item label="优先级" prop="priority">
                    <el-input v-model="form.priority" placeholder="请输入优先级"/>
                </el-form-item>
            </el-form>
            <template #footer>
                <div class="dialog-footer">
                    <el-button type="primary" @click="submitForm">确 定</el-button>
                    <el-button @click="cancel">取 消</el-button>
                </div>
            </template>
        </el-dialog>

        <!-- 回复内容编辑对话框 -->
        <el-dialog :title="replyTitle" v-model="replyOpen" width="60vw" top="10vh" append-to-body>
            <el-form ref="replyForm" :model="replyForm" :rules="replyRules" label-width="80px">
                <el-form-item label="回复内容" prop="content">
                    <CherryMarkdownEditor ref="CherryMarkdown" v-if="replyOpen" height="30vh"
                                          v-model="replyForm.content"></CherryMarkdownEditor>
                </el-form-item>
                <el-form-item label="权重" prop="weight">
                    <el-input-number v-model="replyForm.weight" :min="1" :max="100" style="width: 200px;"/>
                    <span style="margin-left: 10px; color: #909399;">权重越高，被选中的概率越大</span>
                </el-form-item>
            </el-form>
            <template #footer>
                <div class="dialog-footer">
                    <el-button type="primary" @click="submitReplyForm">确 定</el-button>
                    <el-button @click="cancelReply">取 消</el-button>
                </div>
            </template>
        </el-dialog>
    </div>
</template>

<script>
import { listRule, getRule, delRule, addRule, updateRule, exportRule } from '@/api/px/chat/custom'
import Editor from '@/components/Editor'
import { marked } from 'marked'
import { sanitizeHtml } from '@/utils/sanitizeHtml'

export default {
    name: 'CustomReplyRule',
    components: {
        Editor
    },
    data() {
        return {
            // 遮罩层
            loading: true,
            // 选中数组
            ids: [],
            // 非单个禁用
            single: true,
            // 非多个禁用
            multiple: true,
            // 显示搜索条件
            showSearch: true,
            // 总条数
            total: 0,
            // 自定义回复规则表格数据
            ruleList: [],
            // 弹出层标题
            title: '',
            // 是否显示弹出层
            open: false,
            // 回复内容编辑对话框
            replyOpen: false,
            replyTitle: '',
            replyForm: {
                content: '',
                weight: 0
            },
            currentReplyIndex: -1,
            // 查询参数
            queryParams: {
                pageNum: 1,
                pageSize: 10,
                ruleName: null,
                keywords: null,
                enabled: null,
                exactMatch: null,
                priority: null,
                deleted: null,
                createAt: null,
                updateAt: null
            },
            // 表单参数
            form: {},
            // 表单校验
            rules: {
                ruleName: [
                    { required: true, message: '规则名称不能为空', trigger: 'blur' }
                ]
            },
            // 回复内容表单校验
            replyRules: {
                content: [
                    { required: true, message: '回复内容不能为空', trigger: 'blur' }
                ],
                weight: [
                    { required: true, message: '权重不能为空', trigger: 'blur' }
                ]
            }
        }
    },
    created() {
        this.getList()
    },
    methods: {
        /** 查询自定义回复规则列表 */
        getList() {
            this.loading = true
            listRule(this.queryParams).then(response => {
                this.ruleList = response.rows
                this.total = response.total
                this.loading = false
            })
        },
        // 取消按钮
        cancel() {
            this.open = false
            this.reset()
        },
        // 表单重置
        reset() {
            this.form = {
                id: null,
                ruleName: null,
                keywords: null,
                keywordsList: null,
                replyContents: [],
                enabled: null,
                exactMatch: null,
                priority: null,
                deleted: null,
                createAt: null,
                updateAt: null
            }
            this.resetForm('form')
        },
        /** 搜索按钮操作 */
        handleQuery() {
            this.queryParams.pageNum = 1
            this.getList()
        },
        /** 重置按钮操作 */
        resetQuery() {
            this.resetForm('queryForm')
            this.handleQuery()
        },
        // 多选框选中数据
        handleSelectionChange(selection) {
            this.ids = selection.map(item => item.id)
            this.single = selection.length !== 1
            this.multiple = !selection.length
        },
        /** 新增按钮操作 */
        handleAdd() {
            this.reset()
            this.open = true
            this.title = '添加自定义回复规则'
        },
        /** 修改按钮操作 */
        handleUpdate(row) {
            this.reset()
            const id = row.id || this.ids
            getRule(id).then(response => {
                this.form = response.data;
                this.form.keywordsList = this.safeParseKeywords(this.form.keywords)
                this.open = true
                this.title = '修改自定义回复规则'
            })
        },
        /** 提交按钮 */
        submitForm() {
            this.$refs['form'].validate(valid => {
                if (valid) {
                    this.form.keywords = JSON.stringify(this.form.keywordsList)
                    if (this.form.id != null) {
                        updateRule(this.form).then(response => {
                            this.msgSuccess('修改成功')
                            this.open = false
                            this.getList()
                        })
                    } else {
                        addRule(this.form).then(response => {
                            this.msgSuccess('新增成功')
                            this.open = false
                            this.getList()
                        })
                    }
                }
            })
        },
        /** 删除按钮操作 */
        handleDelete(row) {
            const ids = row.id || this.ids
            this.$confirm('是否确认删除自定义回复规则编号为"' + ids + '"的数据项?', '警告', {
                confirmButtonText: '确定',
                cancelButtonText: '取消',
                type: 'warning'
            }).then(function() {
                return delRule(ids)
            }).then(() => {
                this.getList()
                this.msgSuccess('删除成功')
            })
        },
        /** 导出按钮操作 */
        handleExport() {
            const queryParams = this.queryParams
            this.$confirm('是否确认导出所有自定义回复规则数据项?', '警告', {
                confirmButtonText: '确定',
                cancelButtonText: '取消',
                type: 'warning'
            }).then(function() {
                return exportRule(queryParams)
            }).then(response => {
                this.download(response.msg)
            })
        },
        // 添加回复项
        addReplyItem() {
            this.replyForm = {
                content: '',
                weight: 0
            }
            this.currentReplyIndex = -1
            this.replyTitle = '添加回复内容'
            this.replyOpen = true
        },
        // 编辑回复项
        editReplyItem(item, index) {
            this.replyForm = {
                content: item.content,
                weight: item.weight
            }
            this.currentReplyIndex = index
            this.replyTitle = '编辑回复内容'
            this.replyOpen = true
        },
        // 删除回复项
        removeReplyItem(index) {
            this.$confirm('是否确认删除该回复内容?', '警告', {
                confirmButtonText: '确定',
                cancelButtonText: '取消',
                type: 'warning'
            }).then(() => {
                this.form.replyContents.splice(index, 1)
                this.msgSuccess('删除成功')
            })
        },
        // 提交回复表单
        submitReplyForm() {
            this.$refs['replyForm'].validate(valid => {
                if (valid) {
                    if (this.currentReplyIndex === -1) {
                        // 新增
                        if (!this.form.replyContents) {
                            this.form.replyContents = []
                        }
                        this.form.replyContents.push({
                            content: this.replyForm.content,
                            weight: this.replyForm.weight
                        })
                    } else {
                        // 编辑
                        this.form.replyContents[this.currentReplyIndex] = {
                            content: this.replyForm.content,
                            weight: this.replyForm.weight
                        }
                    }
                    this.replyOpen = false
                    this.msgSuccess('操作成功')
                }
            })
        },
        // 取消回复编辑
        cancelReply() {
            this.replyOpen = false
            this.resetReplyForm()
        },
        // 重置回复表单
        resetReplyForm() {
            this.replyForm = {
                content: '',
                weight: 0
            }
            this.currentReplyIndex = -1
        },
        // 安全解析关键词
        safeParseKeywords(value) {
            if (!value) return []
            if (Array.isArray(value)) return value
            try {
                const parsed = JSON.parse(value)
                return Array.isArray(parsed) ? parsed : [String(value)]
            } catch (e) {
                return String(value).split(/[,，\s]+/).filter(Boolean)
            }
        },
        // 渲染markdown内容
        renderMarkdown(content) {
            return sanitizeHtml(marked.parse(content || '', { breaks: true }))
        }
    }
}
</script>

<style scoped>
.reply-list-container {
    border: 1px solid var(--border-primary);
    border-radius: var(--radius-md);
    padding: var(--space-3);
    background-color: var(--bg-card);
    box-shadow: var(--shadow-sm);
}

.reply-list-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: var(--space-3);
    padding-bottom: var(--space-3);
    border-bottom: 1px solid var(--border-primary);
    font-weight: var(--font-semibold);
}

.reply-table {
    width: 100%;
}

.reply-content {
    max-height: 100px;
    overflow: auto;
    line-height: 1.5;
}

.reply-content >>> strong {
    font-weight: var(--font-bold);
}

.reply-content >>> em {
    font-style: italic;
}

.reply-content >>> code {
    background-color: var(--bg-hover);
    padding: 2px 4px;
    border-radius: var(--radius-sm);
    font-family: 'Courier New', monospace;
    font-size: var(--text-sm);
}
</style>
