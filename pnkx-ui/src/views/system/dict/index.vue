<template>
    <div class="app-container">
        <el-form :model="queryParams" ref="queryForm" :inline="true" v-show="showSearch" label-width="68px">
            <el-form-item label="字典名称" prop="dictName">
                <el-input
                    v-model="queryParams.dictName"
                    placeholder="请输入字典名称"
                    clearable
                    size="small"
                    style="width: 240px"
                    @keyup.enter.native="handleQuery"
                />
            </el-form-item>
            <el-form-item label="字典类型" prop="dictType">
                <el-input
                    v-model="queryParams.dictType"
                    placeholder="请输入字典类型"
                    clearable
                    size="small"
                    style="width: 240px"
                    @keyup.enter.native="handleQuery"
                />
            </el-form-item>
            <el-form-item label="状态" prop="status">
                <el-select
                    v-model="queryParams.status"
                    placeholder="字典状态"
                    clearable
                    size="small"
                    style="width: 240px"
                >
                    <el-option
                        v-for="dict in statusOptions"
                        :key="dict.dictValue"
                        :label="dict.dictLabel"
                        :value="dict.dictValue"
                    />
                </el-select>
            </el-form-item>
            <el-form-item label="创建时间">
                <el-date-picker
                    v-model="dateRange"
                    size="small"
                    style="width: 240px"
                    value-format="YYYY-MM-DD"
                    type="daterange"
                    range-separator="-"
                    start-placeholder="开始日期"
                    end-placeholder="结束日期"
                ></el-date-picker>
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
                    v-hasPermi="['system:dict:add']"
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
                    v-hasPermi="['system:dict:edit']"
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
                    v-hasPermi="['system:dict:remove']"
                >删除
                </el-button>
            </el-col>
            <el-col :span="1.5">
                <el-button
                    type="warning"
                                        icon="Download"
                    size="small"
                    @click="handleExport"
                    v-hasPermi="['system:dict:export']"
                >导出
                </el-button>
            </el-col>
            <el-col :span="1.5">
                <el-button
                    type="danger"
                                        icon="Refresh"
                    size="small"
                    @click="handleClearCache"
                    v-hasPermi="['system:dict:remove']"
                >清理缓存
                </el-button>
            </el-col>
            <right-toolbar :showSearch="showSearch" @queryTable="getList"></right-toolbar>
        </el-row>

        <div class="table-main-area">
            <el-table v-loading="loading" :data="typeList" @selection-change="handleSelectionChange">
                <el-table-column type="selection" width="55" align="center"/>
                <el-table-column label="字典编号" align="center" prop="dictId"/>
                <el-table-column label="字典名称" align="center" prop="dictName" :show-overflow-tooltip="true"/>
                <el-table-column label="字典类型" align="center" :show-overflow-tooltip="true">
                    <template v-slot="scope">
                        <router-link :to="'/system/dict/data?dictType=' + scope.row.dictType" class="link-type">
                            <span>{{ scope.row.dictType }}</span>
                        </router-link>
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
                            v-hasPermi="['system:dict:edit']"
                        >修改
                        </el-button>
                        <el-button
                            size="small"
                            type="text"
                            icon="Delete"
                            @click="handleDelete(scope.row)"
                            v-hasPermi="['system:dict:remove']"
                        >删除
                        </el-button>
                    </template>
                </el-table-column>
            </el-table>
        </div>

        <pagination
            v-show="total>0"
            :total="total"
            v-model:page="queryParams.pageNum"
            v-model:limit="queryParams.pageSize"
            @pagination="getList"
        />

        <!-- 添加或修改参数配置对话框 -->
        <el-dialog :title="title" v-model="open" width="500px" append-to-body>
            <el-form ref="form" :model="form" :rules="rules" label-width="80px">
                <el-form-item label="字典名称" prop="dictName">
                    <el-input v-model="form.dictName" placeholder="请输入字典名称"/>
                </el-form-item>
                <el-form-item label="字典类型" prop="dictType">
                    <el-input v-model="form.dictType" placeholder="请输入字典类型"/>
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
                    <el-input v-model="form.remark" type="textarea" placeholder="请输入内容"></el-input>
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
import {listType, getType, delType, addType, updateType, exportType, clearCache} from "@/api/system/dict/type";

export default {
    name: "Dict",
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
            // 字典表格数据
            typeList: [],
            // 弹出层标题
            title: "",
            // 是否显示弹出层
            open: false,
            // 状态数据字典
            statusOptions: [],
            // 日期范围
            dateRange: [],
            // 查询参数
            queryParams: {
                pageNum: 1,
                pageSize: 10,
                dictName: undefined,
                dictType: undefined,
                status: undefined
            },
            // 表单参数
            form: {},
            // 表单校验
            rules: {
                dictName: [
                    {required: true, message: "字典名称不能为空", trigger: "blur"}
                ],
                dictType: [
                    {required: true, message: "字典类型不能为空", trigger: "blur"}
                ]
            }
        };
    },
    created() {
        this.getList();
        this.getDicts("sys_normal_disable").then(response => {
            this.statusOptions = response.data;
        });
    },
    methods: {
        /** 查询字典类型列表 */
        getList() {
            this.loading = true;
            listType(this.addDateRange(this.queryParams, this.dateRange)).then(response => {
                    this.typeList = response.rows;
                    this.total = response.total;
                    this.loading = false;
                }
            );
        },
        // 字典状态字典翻译
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
                dictId: undefined,
                dictName: undefined,
                dictType: undefined,
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
            this.dateRange = [];
            this.resetForm("queryForm");
            this.handleQuery();
        },
        /** 新增按钮操作 */
        handleAdd() {
            this.reset();
            this.open = true;
            this.title = "添加字典类型";
        },
        // 多选框选中数据
        handleSelectionChange(selection) {
            this.ids = selection.map(item => item.dictId)
            this.single = selection.length != 1
            this.multiple = !selection.length
        },
        /** 修改按钮操作 */
        handleUpdate(row) {
            this.reset();
            const dictId = row.dictId || this.ids
            getType(dictId).then(response => {
                this.form = response.data;
                this.open = true;
                this.title = "修改字典类型";
            });
        },
        /** 提交按钮 */
        submitForm: function () {
            this.$refs["form"].validate(valid => {
                if (valid) {
                    if (this.form.dictId != undefined) {
                        updateType(this.form).then(response => {
                            this.msgSuccess("修改成功");
                            this.open = false;
                            this.getList();
                        });
                    } else {
                        addType(this.form).then(response => {
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
            const dictIds = row.dictId || this.ids;
            this.$confirm('是否确认删除字典编号为"' + dictIds + '"的数据项?', "警告", {
                confirmButtonText: "确定",
                cancelButtonText: "取消",
                type: "warning"
            }).then(function () {
                return delType(dictIds);
            }).then(() => {
                this.getList();
                this.msgSuccess("删除成功");
            })
        },
        /** 导出按钮操作 */
        handleExport() {
            const queryParams = this.queryParams;
            this.$confirm('是否确认导出所有类型数据项?', "警告", {
                confirmButtonText: "确定",
                cancelButtonText: "取消",
                type: "warning"
            }).then(function () {
                return exportType(queryParams);
            }).then(response => {
                this.download(response.msg);
            })
        },
        /** 清理缓存按钮操作 */
        handleClearCache() {
            clearCache().then(response => {
                this.msgSuccess("清理成功");
            });
        }
    }
};
</script>

<style lang="scss" scoped>
@import "@/assets/styles/mixin.scss";

.app-container {
  @include adaptive-table-layout(130px);
  background: var(--bg-body);
}

::v-deep .el-form--inline .el-form-item {
  margin-right: var(--space-4);
}

::v-deep .el-table {
  background: var(--bg-card);
  border-radius: var(--radius-lg);
  overflow: hidden;
  box-shadow: var(--shadow-sm);
  border: 1px solid var(--border-primary);

  th {
    background: var(--bg-hover);
    color: var(--text-primary);
    font-weight: var(--font-semibold);
    font-size: var(--text-sm);
  }

  td {
    color: var(--text-secondary);
    font-size: var(--text-sm);
  }

  .el-table__row:hover > td {
    background: var(--bg-hover);
    transition: background var(--duration-fast) var(--ease-default);
  }
}

::v-deep .el-table::before {
  display: none;
}

::v-deep .link-type {
  color: var(--color-primary);
  text-decoration: none;
  transition: color var(--duration-fast) var(--ease-default);

  &:hover {
    color: var(--color-primary-600);
    text-decoration: underline;
  }
}

::v-deep .el-dialog {
  border-radius: var(--radius-xl);
  box-shadow: var(--shadow-lg);

  .el-dialog__header {
    border-bottom: 1px solid var(--border-primary);
    padding: var(--space-4) var(--space-6);
  }

  .el-dialog__body {
    padding: var(--space-6);
  }

  .el-dialog__footer {
    border-top: 1px solid var(--border-primary);
    padding: var(--space-4) var(--space-6);
  }
}

::v-deep .el-button {
  border-radius: var(--radius-md);
  font-size: var(--text-sm);
  transition: all var(--duration-fast) var(--ease-default);

  &:hover {
    transform: translateY(-1px);
    box-shadow: var(--shadow-sm);
  }
}

::v-deep .el-input__inner,
::v-deep .el-textarea__inner {
  border-radius: var(--radius-md);
  border-color: var(--border-primary);
  transition: border-color var(--duration-fast) var(--ease-default),
              box-shadow var(--duration-fast) var(--ease-default);

  &:focus {
    border-color: var(--color-primary);
    box-shadow: 0 0 0 3px rgba(var(--color-primary), 0.1);
  }
}

.mb8 {
  margin-bottom: var(--space-2);
}
</style>
