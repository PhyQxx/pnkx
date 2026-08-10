<template>
    <div class="app-container">
        <el-form :model="queryParams" ref="queryForm" :inline="true" v-show="showSearch" label-width="68px">
            <el-form-item label="是否是机器人回复" prop="isBotReply">
                <el-select v-model="queryParams.isBotReply" placeholder="请选择是否是机器人回复">
                    <el-option label="是" value="1"></el-option>
                    <el-option label="否" value="0"></el-option>
                </el-select>
            </el-form-item>
            <el-form-item label="用户ID" prop="userId">
                <el-input
                    v-model="queryParams.userId"
                    placeholder="请输入用户ID"
                    clearable
                    size="small"
                    @keyup.enter.native="handleQuery"
                />
            </el-form-item>
            <el-form-item label="群组ID" prop="groupId">
                <el-input
                    v-model="queryParams.groupId"
                    placeholder="请输入群组ID"
                    clearable
                    size="small"
                    @keyup.enter.native="handleQuery"
                />
            </el-form-item>
            <el-form-item>
                <el-button type="primary" icon="Search" size="small" @click="handleQuery">搜索</el-button>
                <el-button icon="Refresh" size="small" @click="resetQuery">重置</el-button>
            </el-form-item>
        </el-form>

        <el-row :gutter="10" class="mb8">
            <el-col :span="1.5">
                <el-button
                    type="danger"
                                        icon="Delete"
                    size="small"
                    :disabled="multiple"
                    @click="handleDelete"
                    v-hasPermi="['system:message:remove']"
                >删除
                </el-button>
            </el-col>
            <el-col :span="1.5">
                <el-button
                    type="warning"
                                        icon="Download"
                    size="small"
                    @click="handleExport"
                    v-hasPermi="['system:message:export']"
                >导出
                </el-button>
            </el-col>
            <right-toolbar :showSearch="showSearch" @queryTable="getList"></right-toolbar>
        </el-row>

        <el-table v-loading="loading" :data="messageList" @selection-change="handleSelectionChange">
            <el-table-column type="selection" width="55" align="center"/>
            <el-table-column label="用户ID" align="center" prop="userId"/>
            <el-table-column label="群组ID" align="center" prop="groupId"/>
            <el-table-column label="消息内容" align="center" prop="content" width="300" show-overflow-tooltip/>
            <el-table-column label="消息类型" align="center" prop="messageType"/>
            <el-table-column label="是否为机器人回复" align="center" prop="isBotReply"/>
            <el-table-column label="机器人回复内容" align="center" prop="botReplyContent" width="300" show-overflow-tooltip/>
            <el-table-column label="记录更新时间" align="center" prop="updateAt" width="180">
                <template v-slot="scope">
                    <span>{{ parseTime(scope.row.updateAt, '{y}-{m}-{d}') }}</span>
                </template>
            </el-table-column>
            <el-table-column label="操作" align="center" class-name="small-padding fixed-width" width="180">
                <template v-slot="scope">
                    <el-button
                        v-if="scope.row.content && scope.row.content.length > 0"
                        size="small"
                        type="text"
                        icon="View"
                        @click="showDetail(scope.row)"
                    >详情
                    </el-button>
                    <el-button
                        size="small"
                        type="text"
                        icon="Delete"
                        @click="handleDelete(scope.row)"
                        v-hasPermi="['system:message:remove']"
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

        <!-- 消息详情弹窗 -->
        <el-dialog
            title="消息详情"
            v-model="detailVisible"
            top="5vh"
            width="60vw"
            append-to-body
        >
            <el-descriptions :column="1" border>
                <el-descriptions-item label="用户名">{{ detailData.userName }}</el-descriptions-item>
                <el-descriptions-item label="群组名">{{ detailData.groupName }}</el-descriptions-item>
                <el-descriptions-item label="消息类型">{{ detailData.messageType }}</el-descriptions-item>
                <el-descriptions-item label="是否为机器人回复">
                    <el-tag :type="detailData.isBotReply ? 'success' : 'info'">
                        {{ detailData.isBotReply ? '是' : '否' }}
                    </el-tag>
                </el-descriptions-item>
                <el-descriptions-item label="消息内容">
                    <div class="detail-content" v-html="renderMarkdown(detailData.content)"></div>
                </el-descriptions-item>
                <el-descriptions-item label="机器人回复内容">
                    <div class="detail-content" v-html="renderMarkdown(detailData.botReplyContent)"></div>
                </el-descriptions-item>
                <el-descriptions-item label="记录创建时间">{{ parseTime(detailData.createAt, '{y}-{m}-{d} {h}:{i}:{s}') }}</el-descriptions-item>
                <el-descriptions-item label="记录更新时间">{{ parseTime(detailData.updateAt, '{y}-{m}-{d} {h}:{i}:{s}') }}</el-descriptions-item>
            </el-descriptions>
            <template #footer>
                <span class="dialog-footer">
                    <el-button @click="detailVisible = false">关闭</el-button>
                </span>
            </template>
        </el-dialog>
    </div>
</template>

<script>
import { listMessage, getMessage, delMessage, addMessage, updateMessage, exportMessage } from '@/api/px/chat/record'
import Editor from '@/components/Editor'

// 简单的markdown渲染函数
const renderMarkdown = (content) => {
    if (!content) return ''
    // 简单的markdown转html，实际项目中可以使用更完整的markdown解析器
    return content
        .replace(/\*\*(.*?)\*\*/g, '<strong>$1</strong>')
        .replace(/\*(.*?)\*/g, '<em>$1</em>')
        .replace(/`(.*?)`/g, '<code>$1</code>')
        .replace(/\n/g, '<br>')
}
export default {
    name: 'ChatMessage ',
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
            // 聊天记录表格数据
            messageList: [],
            // 弹出层标题
            title: '',
            // 是否显示弹出层
            open: false,
            // 查询参数
            queryParams: {
                pageNum: 1,
                pageSize: 10,
                messageId: null,
                userId: null,
                userName: null,
                groupId: null,
                groupName: null,
                content: null,
                messageType: null,
                isBotReply: null,
                botReplyContent: null,
                deleted: null,
                createAt: null,
                updateAt: null
            },
            // 表单参数
            form: {},
            // 表单校验
            rules: {
                messageId: [
                    { required: true, message: '消息ID不能为空', trigger: 'blur' }
                ]
            },
            // 详情弹窗相关
            detailVisible: false,
            detailData: {}
        }
    },
    created() {
        this.getList()
    },
    methods: {
        /** 查询聊天记录列表 */
        getList() {
            this.loading = true
            listMessage(this.queryParams).then(response => {
                this.messageList = response.rows
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
                messageId: null,
                userId: null,
                userName: null,
                groupId: null,
                groupName: null,
                content: null,
                messageType: null,
                createTime: null,
                isBotReply: null,
                botReplyContent: null,
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
            this.title = '添加聊天记录'
        },
        /** 修改按钮操作 */
        handleUpdate(row) {
            this.reset()
            const id = row.id || this.ids
            getMessage(id).then(response => {
                this.form = response.data
                this.open = true
                this.title = '修改聊天记录'
            })
        },
        /** 提交按钮 */
        submitForm() {
            this.$refs['form'].validate(valid => {
                if (valid) {
                    if (this.form.id != null) {
                        updateMessage(this.form).then(response => {
                            this.msgSuccess('修改成功')
                            this.open = false
                            this.getList()
                        })
                    } else {
                        addMessage(this.form).then(response => {
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
            this.$confirm('是否确认删除聊天记录编号为"' + ids + '"的数据项?', '警告', {
                confirmButtonText: '确定',
                cancelButtonText: '取消',
                type: 'warning'
            }).then(function() {
                return delMessage(ids)
            }).then(() => {
                this.getList()
                this.msgSuccess('删除成功')
            })
        },
        /** 导出按钮操作 */
        handleExport() {
            const queryParams = this.queryParams
            this.$confirm('是否确认导出所有聊天记录数据项?', '警告', {
                confirmButtonText: '确定',
                cancelButtonText: '取消',
                type: 'warning'
            }).then(function() {
                return exportMessage(queryParams)
            }).then(response => {
                this.download(response.msg)
            })
        },
        // 渲染markdown内容
        renderMarkdown(content) {
            return renderMarkdown(content)
        },
        // 显示详情弹窗
        showDetail(row) {
            this.detailData = { ...row }
            this.detailVisible = true
        }
    }
}
</script>

<style scoped>
.message-content {
    max-width: 100%;
    position: relative;
}

.content-preview {
    overflow: hidden;
    text-overflow: ellipsis;
    display: -webkit-box;
    -webkit-line-clamp: 3;
    -webkit-box-orient: vertical;
    line-height: 1.4;
    max-height: 4.2em;
    word-break: break-all;
}

.detail-content {
    max-height: 200px;
    overflow-y: auto;
    padding: var(--space-3);
    border: 1px solid var(--border-primary);
    border-radius: var(--radius-md);
    background-color: var(--bg-card);
    line-height: 1.6;
}

.detail-content >>> code {
    background-color: var(--bg-hover);
    padding: 2px 4px;
    border-radius: var(--radius-sm);
    font-family: 'Courier New', monospace;
    font-size: var(--text-sm);
}

.detail-content >>> strong {
    font-weight: var(--font-bold);
}

.detail-content >>> em {
    font-style: italic;
}

.el-descriptions {
    margin-bottom: var(--space-5);
}

.el-descriptions-item__label {
    font-weight: var(--font-semibold);
    width: 120px;
}
</style>
