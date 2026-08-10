<template>
    <div class="app-container">
        <el-form :model="queryParams" ref="queryForm" :inline="true" v-show="showSearch" label-width="8rem">
            <el-form-item label="文章类型标签" prop="dictLabel">
                <el-input
                    v-model="queryParams.dictLabel"
                    placeholder="请输入文章类型标签"
                    clearable
                    size="small"
                    @keyup.enter.native="handleQuery"
                />
            </el-form-item>
            <el-form-item label="状态" prop="status">
                <el-select v-model="queryParams.status" placeholder="数据状态" clearable size="small">
                    <el-option
                        v-for="dict in statusOptions"
                        :key="dict.dictValue"
                        :label="dict.dictLabel"
                        :value="dict.dictValue"
                    />
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
                >新增
                </el-button>
            </el-col>
            <right-toolbar :showSearch="showSearch" @queryTable="getList"/>
        </el-row>

        <div class="table-main-area">
            <el-table v-loading="loading" :data="dataList" @selection-change="handleSelectionChange">
                <el-table-column label="文章类型标签" align="center" prop="dictLabel"/>
                <el-table-column label="文章类型键值" align="center" prop="dictValue"/>
                <el-table-column label="文章数量" align="center" prop="articleNumber">
                    <template v-slot="scope">
                        <a @click="$router.push('/blog/articlemanager?type=' + scope.row.dictValue)">{{ scope.row.articleNumber }}</a>
                    </template>
                </el-table-column>
                <el-table-column label="状态" align="center" prop="status" :formatter="statusFormat"/>
                <el-table-column label="备注" align="center" prop="remark" :show-overflow-tooltip="true"/>
                <el-table-column label="创建时间" align="center" prop="createTime" width="180">
                    <template v-slot="scope">
                        <span>{{ parseTime(scope.row.createTime) }}</span>
                    </template>
                </el-table-column>
                <el-table-column label="操作" align="center" class-name="small-padding fixed-width">
                    <template v-slot="scope">
                        <el-button
                            size="small"
                            type="text"
                            icon="Edit"
                            @click="handleUpdate(scope.row)"
                        >修改
                        </el-button>
                        <el-button
                            size="small"
                            type="text"
                            icon="Delete"
                            @click="handleDelete(scope.row)"
                        >删除
                        </el-button>
                    </template>
                </el-table-column>
            </el-table>
        </div>

        <pagination
            v-show="total>queryParams.pageSize"
            :total="total"
            v-model:page="queryParams.pageNum"
            v-model:limit="queryParams.pageSize"
            @pagination="getList"
        />

        <!-- 添加或修改参数配置对话框 -->
        <el-dialog :title="title" v-model="open" width="500px" append-to-body>
            <el-form ref="form" :model="form" :rules="rules" label-width="9rem">
                <el-form-item label="文章类型">
                    <el-input v-model="form.dictType" :disabled="true"/>
                </el-form-item>
                <el-form-item label="文章类型标签" prop="dictLabel">
                    <el-input v-model="form.dictLabel" placeholder="请输入数据标签"/>
                </el-form-item>
                <el-form-item label="文章类型键值" prop="dictValue">
                    <el-input v-model="form.dictValue" placeholder="请输入数据键值"/>
                </el-form-item>
                <el-form-item label="显示排序" prop="dictSort">
                    <el-input-number v-model="form.dictSort" controls-position="right" :min="0"/>
                </el-form-item>
                <el-form-item label="状态" prop="status">
                    <el-radio-group v-model="form.status">
                        <el-radio
                            v-for="dict in statusOptions"
                            :key="dict.dictValue"
                            :label="dict.dictValue"
                        >{{ dict.dictLabel }}
                        </el-radio>
                    </el-radio-group>
                </el-form-item>
                <el-form-item label="备注" prop="remark">
                    <el-input v-model="form.remark" type="textarea" placeholder="请输入内容"/>
                </el-form-item>
            </el-form>
            <template #footer>
                <div class="dialog-footer">
                    <el-button :loading="confirmLoading" type="primary" @click="submitForm">确 定</el-button>
                    <el-button @click="cancel">取 消</el-button>
                </div>
            </template>
        </el-dialog>
    </div>
</template>

<script>
import {dictDataCheckUniqueness, getData, delData, addData, updateData, exportData} from "@/api/system/dict/data";
import {listArticleNotContent} from "@/api/px/blog/article";
import {listData} from "@/api/px/blog/type";

export default {
    name: "Articletype",
    data() {
        //文章类型名称校验规则
        const dictLabelValidate = (rule, value, callback) => {
            if (this.form.dictCode === undefined || this.form.dictCode === '') {
                dictDataCheckUniqueness({
                    dictType: 'px_article_type',
                    dictLabel: value
                }).then(res => {
                    if (res.data > 0) {
                        callback(new Error('文章类型名称不能重复'));
                    } else {
                        callback();
                    }
                });
            } else {
                callback();
            }
        };
        //文章类型键值校验规则
        const dictValueValidate = (rule, value, callback) => {
            if (this.form.dictCode === undefined || this.form.dictCode === '') {
                dictDataCheckUniqueness({
                    dictType: 'px_article_type',
                    dictValue: value
                }).then(res => {
                    if (res.data > 0) {
                        callback(new Error('文章类型键值不能重复'));
                    } else {
                        callback();
                    }
                });
            } else {
                callback();
            }
        };
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
            // 文章类型表格数据
            dataList: [],
            // 默认文章类型类型
            defaultDictType: "",
            // 弹出层标题
            title: "",
            // 是否显示弹出层
            open: false,
            // 状态数据文章类型
            statusOptions: [],
            // 类型数据文章类型
            typeOptions: [],
            // 查询参数
            queryParams: {
                pageNum: 1,
                pageSize: 10,
                dictName: undefined,
                dictType: 'px_article_type',
                status: undefined
            },
            // 表单参数
            form: {},
            // 表单校验
            rules: {
                dictLabel: [
                    {required: true, message: "文章类型名称不能为空", trigger: "blur"},
                    {validator: dictLabelValidate, trigger: 'blur'}
                ],
                dictValue: [
                    {required: true, message: "文章类型键值不能为空", trigger: "blur"},
                    {validator: dictValueValidate, trigger: 'blur'}
                ],
                dictSort: [
                    {required: true, message: "文章类型顺序不能为空", trigger: "blur"}
                ]
            },
            //表单提交按钮加载
            confirmLoading: false,
            //表单中的类型字段
            formType: ''
        };
    },
    created() {
        this.getList();
        this.getDicts("sys_normal_disable").then(response => {
            this.statusOptions = response.data;
        });
    },
    methods: {
        /** 查询文章类型数据列表 */
        getList() {
            this.loading = true;
            listData(this.queryParams).then(response => {
                this.dataList = response.rows;
                this.total = response.total;
                this.loading = false;
            });
        },
        // 数据状态文章类型翻译
        statusFormat(row, column) {
            return this.selectDictLabel(this.statusOptions, row.status);
        },
        // 取消按钮
        cancel() {
            this.open = false;
            this.reset();
        },
        // 表单重置
        reset() {
            this.form = {
                dictCode: undefined,
                dictLabel: undefined,
                dictValue: undefined,
                dictSort: 0,
                status: "0",
                remark: undefined
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
            this.queryParams.dictType = 'px_article_type';
            this.handleQuery();
        },
        /** 新增按钮操作 */
        handleAdd() {
            this.reset();
            this.open = true;
            this.title = "添加文章类型数据";
            this.form.dictType = this.queryParams.dictType;
        },
        // 多选框选中数据
        handleSelectionChange(selection) {
            this.ids = selection.map(item => item.dictCode)
            this.single = selection.length !== 1
            this.multiple = !selection.length
        },
        /** 修改按钮操作 */
        handleUpdate(row) {
            this.reset();
            const dictCode = row.dictCode || this.ids
            getData(dictCode).then(response => {
                this.form = response.data;
                this.open = true;
                this.title = "修改文章类型数据";
                this.formType = response.data.dictValue;
            });
        },
        /** 提交按钮 */
        submitForm: function () {
            this.confirmLoading = true;
            this.$refs["form"].validate(valid => {
                if (valid) {
                    if (this.form.dictCode !== undefined) {
                        listArticleNotContent({type: this.formType}).then(res => {
                            if (res.total > 0) {
                                this.$notify.error('该类型下有已发表文章，不可修改键值')
                            } else {
                                updateData(this.form).then(response => {
                                    this.confirmLoading = false;
                                    this.msgSuccess("修改成功");
                                    this.open = false;
                                    this.getList();
                                });
                            }
                        });
                    } else {
                        addData(this.form).then(response => {
                            this.confirmLoading = false;
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
            if (row.articleNumber > 0) {
                this.$notify.warning('该分类下有已发布文章，请删除文章后再删除该分类')
            } else {
                const dictCodes = row.dictCode || this.ids;
                this.$confirm(`是否确认删除“${row.dictLabel}”文章类型?`, "警告", {
                    confirmButtonText: "确定",
                    cancelButtonText: "取消",
                    type: "warning"
                }).then(function () {
                    return delData(dictCodes);
                }).then(() => {
                    this.getList();
                    this.msgSuccess("删除成功");
                })
            }
        },
        /** 导出按钮操作 */
        handleExport() {
            const queryParams = this.queryParams;
            this.$confirm('是否确认导出所有数据项?', "警告", {
                confirmButtonText: "确定",
                cancelButtonText: "取消",
                type: "warning"
            }).then(function () {
                return exportData(queryParams);
            }).then(response => {
                this.download(response.msg);
            })
        }
    }
};
</script>

<style lang="scss" scoped>
@import "@/assets/styles/mixin.scss";

.app-container {
    @include adaptive-table-layout(130px);
    padding: var(--space-6);
    background: var(--bg-body);

    ::v-deep .el-form {
        .el-form-item__label {
            font-size: var(--text-sm);
            color: var(--text-secondary);
        }
    }

    ::v-deep .el-table {
        a {
            color: var(--color-primary);
            cursor: pointer;
            transition: color var(--duration-fast) var(--ease-default);

            &:hover {
                color: var(--color-primary-600);
            }
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
