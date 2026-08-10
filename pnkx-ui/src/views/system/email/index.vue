<template>
    <div class="app-container">
        <el-form :inline="true" :model="queryParams" label-width="8rem" ref="queryForm" v-show="showSearch">
            <el-form-item label="收件人邮箱" prop="receiverEmail">
                <el-input
                    @keyup.enter.native="handleQuery"
                    clearable
                    placeholder="请输入收件人邮箱"
                    size="small"
                    v-model="queryParams.receiverEmail"
                />
            </el-form-item>
            <el-form-item label="抄送人邮箱" prop="ccEmail">
                <el-input
                    @keyup.enter.native="handleQuery"
                    clearable
                    placeholder="请输入抄送人邮箱"
                    size="small"
                    v-model="queryParams.ccEmail"
                />
            </el-form-item>
            <el-form-item>
                <el-button @click="handleQuery" icon="Search" size="small" type="primary">搜索</el-button>
                <el-button @click="resetQuery" icon="Refresh" size="small">重置</el-button>
            </el-form-item>
        </el-form>

        <el-row :gutter="10" class="mb8">
            <el-col :span="1.5">
                <el-button
                    @click="handleAdd"
                    icon="Plus"
                                        size="small"
                    type="primary"
                >新增
                </el-button>
            </el-col>
            <el-col :span="1.5">
                <el-button
                    :disabled="multiple"
                    @click="handleDelete"
                    icon="Delete"
                                        size="small"
                    type="danger"
                >删除
                </el-button>
            </el-col>
            <el-col :span="1.5">
                <el-button
                    @click="handleExport"
                    icon="Download"
                                        size="small"
                    type="warning"
                >导出
                </el-button>
            </el-col>
            <right-toolbar :showSearch="showSearch" @queryTable="getList"/>
        </el-row>

        <el-table :data="emailList" @selection-change="handleSelectionChange" v-loading="loading">
            <el-table-column align="center" type="selection" width="55"/>
            <el-table-column align="center" label="收件人邮箱" prop="receiverEmail"/>
            <el-table-column align="center" label="抄送人邮箱" prop="ccEmail"/>
            <el-table-column align="center" label="邮件主题" prop="subject"/>
            <el-table-column align="center" label="发送时间" prop="createTime"/>
            <el-table-column align="center" label="备注" prop="remark"/>
            <el-table-column align="center" class-name="small-padding fixed-width" label="操作">
                <template v-slot="scope">
                    <el-button
                        @click="handleUpdate(scope.row)"
                        icon="Edit"
                        size="small"
                        type="text"
                    >查看
                    </el-button>
                    <el-button
                        @click="handleDelete(scope.row)"
                        icon="Delete"
                        size="small"
                        type="text"
                    >删除
                    </el-button>
                </template>
            </el-table-column>
        </el-table>

        <pagination
            v-model:limit="queryParams.pageSize"
            v-model:page="queryParams.pageNum"
            :total="total"
            @pagination="getList"
            v-show="total>0"
        />

        <!-- 添加或修改邮件记录对话框 -->
        <el-dialog :title="title" v-model="open" append-to-body top="5vh" width="60vw">
            <el-form :model="form" :rules="rules" label-width="8rem" ref="form">
                <el-form-item label="收件人邮箱" prop="receiverEmail">
                    <el-input :disabled="!editFlag" placeholder="请输入收件人邮箱" v-model="form.receiverEmail"/>
                </el-form-item>
                <el-form-item label="抄送人邮箱" prop="ccEmail">
                    <el-input :disabled="!editFlag" placeholder="请输入抄送人邮箱,多个用逗号分隔" v-model="form.ccEmail"/>
                </el-form-item>
                <el-form-item label="邮件主题" prop="subject">
                    <el-input :disabled="!editFlag" placeholder="请输入主题" v-model="form.subject"/>
                </el-form-item>
                <el-form-item label="邮件内容" v-if="open">
                    <editor ref="editor" :height="400" v-model="form.content"></editor>
                </el-form-item>
            </el-form>
            <div class="dialog-footer" slot="footer">
                <el-button :loading="addLoading" @click="submitForm" type="primary" v-if="editFlag">确 定</el-button>
                <el-button @click="cancel">取 消</el-button>
            </div>
        </el-dialog>
    </div>
</template>

<script>
import {listEmail, getEmail, delEmail, exportEmail, sendHtmlEmail} from "@/api/system/email";

export default {
    name: "Email",
    data() {
        return {
            //修改标志
            editFlag: false,
            // 遮罩层
            loading: true,
            addLoading: false,
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
            // 邮件记录表格数据
            emailList: [],
            // 弹出层标题
            title: "",
            // 是否显示弹出层
            open: false,
            // 查询参数
            queryParams: {
                pageNum: 1,
                pageSize: 10,
                sendId: null,
                receiverId: null,
                receiverEmail: null,
                ccId: null,
                ccEmail: null,
                subject: null,
                content: null,
                version: null,
            },
            // 表单参数
            form: {},
            // 表单校验
            rules: {}
        };
    },
    created() {
        this.getList();
    },
    methods: {
        /** 查询邮件记录列表 */
        getList() {
            this.loading = true;
            listEmail(this.queryParams).then(response => {
                this.emailList = response.rows;
                this.total = response.total;
                this.loading = false;
            });
        },
        // 取消按钮
        cancel() {
            this.open = false;
            this.reset();
        },
        // 表单重置
        reset() {
            this.form = {
                id: null,
                sendId: null,
                receiverId: null,
                receiverEmail: null,
                ccId: null,
                ccEmail: null,
                subject: null,
                content: null,
                version: null,
                createBy: null,
                createTime: null,
                updateBy: null,
                updateTime: null,
                remark: null
            };
            this.resetForm("form");
        },
        /** 搜索按钮操作 */
        handleQuery() {
            this.queryParams.pageNum = 1;
            this.getList();
        },
        /** 重置按钮操作 */
        resetQuery() {
            this.resetForm("queryForm");
            this.handleQuery();
        },
        // 多选框选中数据
        handleSelectionChange(selection) {
            this.ids = selection.map(item => item.id)
            this.single = selection.length !== 1
            this.multiple = !selection.length
        },
        /** 新增按钮操作 */
        handleAdd() {
            this.editFlag = true;
            this.reset();
            this.open = true;
            this.title = "添加邮件记录";
        },
        /** 修改按钮操作 */
        handleUpdate(row) {
            this.editFlag = false;
            this.reset();
            const id = row.id || this.ids
            getEmail(id).then(response => {
                this.form = response.data;
                this.open = true;
                this.title = "修改邮件记录";
            });
        },
        /** 提交按钮 */
        submitForm() {
            this.addLoading = true;
            sendHtmlEmail(this.form).then(response => {
                if (response.data) {
                    this.$notify.success('已发送');
                    this.addLoading = false;
                    this.open = false;
                    this.getList();
                }
            });
        },
        /** 删除按钮操作 */
        handleDelete(row) {
            const ids = row.id || this.ids;
            this.$confirm('是否确认删除邮件记录编号为"' + ids + '"的数据项?', "警告", {
                confirmButtonText: "确定",
                cancelButtonText: "取消",
                type: "warning"
            }).then(function () {
                return delEmail(ids);
            }).then(() => {
                this.getList();
                this.msgSuccess("删除成功");
            })
        },
        /** 导出按钮操作 */
        handleExport() {
            const queryParams = this.queryParams;
            this.$confirm('是否确认导出所有邮件记录数据项?', "警告", {
                confirmButtonText: "确定",
                cancelButtonText: "取消",
                type: "warning"
            }).then(function () {
                return exportEmail(queryParams);
            }).then(response => {
                this.download(response.msg);
            })
        }
    }
};
</script>

<style lang="scss" scoped>
.app-container {
    padding: var(--space-6);
}

.content {
    display: flex;
    flex-flow: column;

    .right {
        border-radius: var(--radius-md);
        border: 1px solid var(--border-primary);
        margin-top: var(--space-4);
    }
}

.dialog-footer {
    display: flex;
    justify-content: flex-end;
    gap: var(--space-2);
}
</style>
