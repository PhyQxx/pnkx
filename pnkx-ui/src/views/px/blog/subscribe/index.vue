<template>
    <div class="app-container">
        <el-form :inline="true" :model="queryParams" label-width="68px" ref="queryForm" v-show="showSearch">
            <el-form-item label="订阅邮箱" prop="subscribeMail">
                <el-input
                    @keyup.enter.native="handleQuery"
                    clearable
                    placeholder="请输入订阅邮箱"
                    size="small"
                    v-model="queryParams.subscribeMail"
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
            <right-toolbar :showSearch="showSearch" @queryTable="getList"/>
        </el-row>

        <div class="table-main-area">
            <el-table :data="subscribeList" @selection-change="handleSelectionChange" v-loading="loading">
                <el-table-column align="center" type="selection" width="55"/>
                <el-table-column align="center" label="订阅邮箱" prop="subscribeMail"/>
                <el-table-column align="center" label="订阅时间" prop="createTime"/>
                <el-table-column align="center" label="备注" prop="remark"/>
                <el-table-column align="center" class-name="small-padding fixed-width" label="操作">
                    <template v-slot="scope">
                        <el-button
                            @click="handleUpdate(scope.row)"
                            icon="Edit"
                            size="small"
                            type="text"
                        >修改
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
        </div>

        <pagination
            v-model:limit="queryParams.pageSize"
            v-model:page="queryParams.pageNum"
            :total="total"
            @pagination="getList"
            v-show="total>0"
        />

        <!-- 添加或修改订阅对话框 -->
        <el-dialog :title="title" v-model="open" append-to-body width="500px">
            <el-form :model="form" :rules="rules" label-width="80px" ref="form">
                <el-form-item label="订阅邮箱" prop="subscribeMail">
                    <el-input placeholder="请输入订阅邮箱" v-model="form.subscribeMail"/>
                </el-form-item>
                <el-form-item label="版本号" prop="version">
                    <el-input placeholder="请输入版本号" v-model="form.version"/>
                </el-form-item>
                <el-form-item label="备注" prop="remark">
                    <el-input placeholder="请输入内容" type="textarea" v-model="form.remark"/>
                </el-form-item>
            </el-form>
            <template #footer>
                <div class="dialog-footer">
                    <el-button @click="submitForm" type="primary">确 定</el-button>
                    <el-button @click="cancel">取 消</el-button>
                </div>
            </template>
        </el-dialog>
    </div>
</template>

<script>
import {
    listSubscribe,
    getSubscribe,
    delSubscribe,
    addSubscribe,
    updateSubscribe
} from "@/api/px/blog/subscribe";

export default {
    name: "Subscribe",
    components: {},
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
            // 订阅表格数据
            subscribeList: [],
            // 弹出层标题
            title: "",
            // 是否显示弹出层
            open: false,
            // 查询参数
            queryParams: {
                pageNum: 1,
                pageSize: 10,
                subscribeMail: null,
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
        /** 查询订阅列表 */
        getList() {
            this.loading = true;
            listSubscribe(this.queryParams).then(response => {
                this.subscribeList = response.rows;
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
                subscribeMail: null,
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
            this.reset();
            this.open = true;
            this.title = "添加订阅";
        },
        /** 修改按钮操作 */
        handleUpdate(row) {
            this.reset();
            const id = row.id || this.ids
            getSubscribe(id).then(response => {
                this.form = response.data;
                this.open = true;
                this.title = "修改订阅";
            });
        },
        /** 提交按钮 */
        submitForm() {
            this.$refs["form"].validate(valid => {
                if (valid) {
                    if (this.form.id != null) {
                        updateSubscribe(this.form).then(response => {
                            this.msgSuccess("修改成功");
                            this.open = false;
                            this.getList();
                        });
                    } else {
                        addSubscribe(this.form).then(response => {
                            this.msgSuccess("新增成功");
                            this.open = false;
                            this.getList();
                        });
                    }
                }
            });
        },
        /** 删除按钮操作 */
        handleDelete(row) {
            const ids = row.id || this.ids;
            this.$confirm('是否确认删除订阅编号为"' + ids + '"的数据项?', "警告", {
                confirmButtonText: "确定",
                cancelButtonText: "取消",
                type: "warning"
            }).then(function () {
                return delSubscribe(ids);
            }).then(() => {
                this.getList();
                this.msgSuccess("删除成功");
            })
        },
        /** 导出按钮操作 */
        handleExport() {
            const queryParams = this.queryParams;
            this.$confirm('是否确认导出所有订阅数据项?', "警告", {
                confirmButtonText: "确定",
                cancelButtonText: "取消",
                type: "warning"
            }).then(function () {
                return exportSubscribe(queryParams);
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
    background: var(--bg-body);
    min-height: calc(100vh - 84px);

    ::v-deep .el-form {
        .el-form-item__label {
            font-size: var(--text-sm);
            color: var(--text-secondary);
        }
    }

    ::v-deep .el-table {
        border-radius: var(--radius-md);
        overflow: hidden;
        border: 1px solid var(--border-primary);

        th {
            background: var(--bg-card);
            color: var(--text-primary);
            font-weight: var(--font-semibold);
            font-size: var(--text-sm);
            border-bottom: 1px solid var(--border-primary);
        }

        td {
            color: var(--text-primary);
            font-size: var(--text-sm);
            border-bottom: 1px solid var(--border-primary);
        }

        tr:hover > td {
            background: var(--bg-hover);
        }
    }

    ::v-deep .el-button {
        border-radius: var(--radius-sm);
        transition: all var(--duration-normal) var(--ease-default);
    }

    ::v-deep .el-dialog {
        border-radius: var(--radius-lg);
        overflow: hidden;

        .el-dialog__header {
            border-bottom: 1px solid var(--border-primary);
            padding: var(--space-4) var(--space-6);

            .el-dialog__title {
                font-size: var(--text-lg);
                font-weight: var(--font-semibold);
                color: var(--text-primary);
            }
        }

        .el-dialog__body {
            padding: var(--space-6);
        }

        .el-dialog__footer {
            border-top: 1px solid var(--border-primary);
            padding: var(--space-4) var(--space-6);
            background: var(--bg-body);
        }
    }

    .dialog-footer {
        display: flex;
        justify-content: flex-end;
        gap: var(--space-3);
    }

    .mb8 {
        margin-bottom: var(--space-2);
    }
}
</style>
