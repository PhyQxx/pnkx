<!--
 * @File: manage
 * @Author: PHY
 * @Date: 2022/5/22 10:19
 * @Description: 卡券管理
-->
<template>
    <div class="app-container">
        <el-form v-show="showSearch" ref="queryForm" :inline="true" :model="queryParams" label-width="8rem">
            <el-form-item label="卡券名称" prop="title">
                <el-input
                    v-model="queryParams.title"
                    clearable
                    placeholder="请输入卡券名称"
                    size="small"
                    @keyup.enter.native="handleQuery"
                />
            </el-form-item>
            <el-form-item>
                <el-button icon="Search" size="small" type="primary" @click="handleQuery">搜索</el-button>
                <el-button icon="Refresh" size="small" @click="resetQuery">重置</el-button>
            </el-form-item>
        </el-form>

        <el-row :gutter="10" class="mb8">
            <el-col :span="1.5">
                <el-button
                    icon="Plus"
                                        size="small"
                    type="primary"
                    @click="handleAdd"
                >新增
                </el-button>
            </el-col>
            <el-col :span="1.5">
                <el-button
                    :disabled="single"
                    icon="Edit"
                                        size="small"
                    type="success"
                    @click="handleUpdate"
                >修改
                </el-button>
            </el-col>
            <el-col :span="1.5">
                <el-button
                    :disabled="multiple"
                    icon="Delete"
                                        size="small"
                    type="danger"
                    @click="handleDelete"
                >删除
                </el-button>
            </el-col>
            <el-col :span="1.5">
                <el-button
                    icon="Download"
                                        size="small"
                    type="warning"
                    @click="handleExport"
                >导出
                </el-button>
            </el-col>
            <right-toolbar :showSearch="showSearch" @queryTable="getList"></right-toolbar>
        </el-row>

        <el-table v-loading="loading" :data="cardList" @selection-change="handleSelectionChange">
            <el-table-column align="center" type="selection" width="55"/>
            <el-table-column align="center" label="卡券名称" prop="title"/>
            <el-table-column align="center" label="卡券描述" prop="describe"/>
            <el-table-column align="center" label="卡券logo" prop="logo">
                <template v-slot="scope">
                    <el-image :preview-src-list="[scope.row.logo]"
                        :src="scope.row.thumbnail || scope.row.logo"
                              fit="scale-down"
                              style="width: 5rem; height: 5rem;">
                        <template #error>
                            <div class="image-slot invalid-svg">
                                <svg-icon icon-class="已失效2"/>
                            </div>
                        </template>
                    </el-image>
                </template>
            </el-table-column>
            <el-table-column align="center" label="卡券价值" prop="money" show-summary>
                <template v-slot="scope">
                    {{scope.row.money}}元
                </template>
            </el-table-column>
            <el-table-column align="center" label="定期发放数量" prop="number" show-summary/>
            <el-table-column align="center" label="版本号" prop="version"/>
            <el-table-column align="center" label="备注" prop="remark"/>
            <el-table-column align="center" class-name="small-padding fixed-width" label="操作">
                <template v-slot="scope">
                    <el-button
                        icon="Edit"
                        size="small"
                        type="text"
                        @click="handleUpdate(scope.row)"
                    >修改
                    </el-button>
                    <el-button
                        icon="Delete"
                        size="small"
                        type="text"
                        @click="handleDelete(scope.row)"
                    >删除
                    </el-button>
                </template>
            </el-table-column>
        </el-table>

        <pagination
            v-show="total>0"
            v-model:limit="queryParams.pageSize"
            v-model:page="queryParams.pageNum"
            :total="total"
            @pagination="getList"
        />

        <!-- 添加或修改情侣卡券对话框 -->
        <el-dialog :title="title" v-model="open" append-to-body width="60vw">
            <el-form ref="form" :model="form" :rules="rules" label-width="11rem">
                <el-form-item label="卡券名称" prop="title">
                    <el-input v-model="form.title" placeholder="请输入卡券名称"/>
                </el-form-item>
                <el-form-item label="卡券描述" prop="describe">
                    <el-input v-model="form.describe" placeholder="请输入内容" type="textarea"/>
                </el-form-item>
                <el-form-item label="卡券价值" prop="money">
                    <el-input v-model="form.money" placeholder="请输入内容" type="number">
                        <template slot="append">元</template>
                    </el-input>
                </el-form-item>
                <el-form-item label="卡券图标" prop="logo">
                    <imageUpload v-model="form.logo" image-type="qlkq"/>
                </el-form-item>
                <el-form-item label="定期发放数量" prop="number">
                    <el-input v-model="form.number" placeholder="请输入内容" type="number"/>
                </el-form-item>
                <el-form-item label="备注" prop="remark">
                    <el-input v-model="form.remark" placeholder="请输入内容" type="textarea"/>
                </el-form-item>
            </el-form>
            <template #footer>
                <div class="dialog-footer">
                    <el-button type="primary" @click="submitForm">确 定</el-button>
                    <el-button @click="cancel">取 消</el-button>
                </div>
            </template>
        </el-dialog>
    </div>
</template>

<script>
import {listCard, getCard, delCard, addCard, updateCard, exportCard} from "@/api/px/life/card";
import IconSelect from "./IconSelect/index.vue";
import ImageUpload from "@/components/ImageUpload/index.vue";

export default {
    name: "Card",
    components: {ImageUpload, IconSelect},
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
            // 情侣卡券表格数据
            cardList: [],
            // 弹出层标题
            title: "",
            // 是否显示弹出层
            open: false,
            // 查询参数
            queryParams: {
                pageNum: 1,
                pageSize: 10,
                title: null,
                describe: null,
                logo: null,
                version: null,
            },
            // 表单参数
            form: {
                title: '',
                describe: '',
                logo: ''
            },
            // 表单校验
            rules: {
                title: [{required: true, message: "卡券名称不能为空", trigger: "blur"}],
                describe: [{required: true, message: "卡券描述不能为空", trigger: "blur"}],
                logo: [{required: true, message: "卡券图标不能为空", trigger: ["blur", "change"]}],
                money: [{required: true, message: "卡券价值不能为空", trigger: "blur"}],
                number: [{required: true, message: "定期发放数量不能为空", trigger: "blur"}],
            }
        };
    },
    created() {
        this.getList();
    },
    methods: {
        /**
         * 卡券图标选择
         */
        logoSelected(name) {
            this.form.logo = name;
        },
        /** 查询情侣卡券列表 */
        getList() {
            this.loading = true;
            listCard(this.queryParams).then(response => {
                this.cardList = response.rows;
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
                title: null,
                describe: null,
                logo: '',
                delFlag: null,
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
            this.title = "添加情侣卡券";
        },
        /** 修改按钮操作 */
        handleUpdate(row) {
            this.reset();
            const id = row.id || this.ids
            getCard(id).then(response => {
                this.form = response.data;
                this.open = true;
                this.title = "修改情侣卡券";
            });
        },
        /** 提交按钮 */
        submitForm() {
            this.$refs["form"].validate(valid => {
                if (valid) {
                    this.form.thumbnail = this.form.logo.slice(0, this.form.logo.lastIndexOf('/') + 1) + 'thumbnail-' + this.form.logo.slice(this.form.logo.lastIndexOf('/') + 1);
                    if (this.form.id != null) {
                        updateCard(this.form).then(response => {
                            this.msgSuccess("修改成功");
                            this.open = false;
                            this.getList();
                        });
                    } else {
                        addCard(this.form).then(response => {
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
            this.$confirm('是否确认删除情侣卡券编号为"' + ids + '"的数据项?', "警告", {
                confirmButtonText: "确定",
                cancelButtonText: "取消",
                type: "warning"
            }).then(function () {
                return delCard(ids);
            }).then(() => {
                this.getList();
                this.msgSuccess("删除成功");
            })
        },
        /** 导出按钮操作 */
        handleExport() {
            const queryParams = this.queryParams;
            this.$confirm('是否确认导出所有情侣卡券数据项?', "警告", {
                confirmButtonText: "确定",
                cancelButtonText: "取消",
                type: "warning"
            }).then(function () {
                return exportCard(queryParams);
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

    .invalid-svg {
        display: flex;
        align-items: center;
        justify-content: center;
        width: 5rem;
        height: 5rem;
        background: var(--bg-hover);
        border-radius: var(--radius-sm);
    }
}
</style>
