<template>
    <div class="app-container">
        <el-form :model="queryParams" ref="queryForm" :inline="true" v-show="showSearch" label-width="68px">
            <el-form-item label="系统模块" prop="title">
                <el-input
                    v-model="queryParams.title"
                    placeholder="请输入系统模块"
                    clearable
                    style="width: 240px;"
                    size="small"
                    @keyup.enter.native="handleQuery"
                />
            </el-form-item>
            <el-form-item label="操作人员" prop="operName">
                <el-input
                    v-model="queryParams.operName"
                    placeholder="请输入操作人员"
                    clearable
                    style="width: 240px;"
                    size="small"
                    @keyup.enter.native="handleQuery"
                />
            </el-form-item>
            <el-form-item label="类型" prop="businessType">
                <el-select
                    v-model="queryParams.businessType"
                    placeholder="操作类型"
                    clearable
                    size="small"
                    style="width: 240px"
                >
                    <el-option
                        v-for="dict in typeOptions"
                        :key="dict.dictValue"
                        :label="dict.dictLabel"
                        :value="dict.dictValue"
                    />
                </el-select>
            </el-form-item>
            <el-form-item label="状态" prop="status">
                <el-select
                    v-model="queryParams.status"
                    placeholder="操作状态"
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
            <el-form-item label="操作时间">
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
                    type="danger"
                                        icon="Delete"
                    size="small"
                    :disabled="multiple"
                    @click="handleDelete"
                    v-hasPermi="['monitor:operlog:remove']"
                >删除
                </el-button>
            </el-col>
            <el-col :span="1.5">
                <el-button
                    type="danger"
                                        icon="Delete"
                    size="small"
                    @click="handleClean"
                    v-hasPermi="['monitor:operlog:remove']"
                >清空
                </el-button>
            </el-col>
            <el-col :span="1.5">
                <el-button
                    type="warning"
                                        icon="Download"
                    size="small"
                    @click="handleExport"
                    v-hasPermi="['system:config:export']"
                >导出
                </el-button>
            </el-col>
            <right-toolbar :showSearch="showSearch" @queryTable="getList"></right-toolbar>
        </el-row>

        <div class="table-main-area">
            <el-table v-loading="loading" :data="list" @selection-change="handleSelectionChange">
                <el-table-column type="selection" width="55" align="center"/>
                <el-table-column label="日志编号" align="center" prop="operId"/>
                <el-table-column label="系统模块" align="center" prop="title"/>
                <el-table-column label="操作类型" align="center" prop="businessType" :formatter="typeFormat"/>
                <el-table-column label="请求方式" align="center" prop="requestMethod"/>
                <el-table-column label="操作人员" align="center" prop="operName"/>
                <el-table-column label="主机" align="center" prop="operIp" width="130" :show-overflow-tooltip="true"/>
                <el-table-column label="操作地点" align="center" prop="operLocation" :show-overflow-tooltip="true"/>
                <el-table-column label="操作状态" align="center" prop="status" :formatter="statusFormat"/>
                <el-table-column label="操作日期" align="center" prop="operTime" width="180">
                    <template v-slot="scope">
                        <span>{{ parseTime(scope.row.operTime) }}</span>
                    </template>
                </el-table-column>
                <el-table-column label="操作" align="center" class-name="small-padding fixed-width">
                    <template v-slot="scope">
                        <el-button
                            size="small"
                            type="text"
                            icon="View"
                            @click="handleView(scope.row,scope.index)"
                            v-hasPermi="['monitor:operlog:query']"
                        >详细
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

        <!-- 操作日志详细 -->
        <el-dialog title="操作日志详细" v-model="open" width="700px" append-to-body>
            <el-form ref="form" :model="form" label-width="100px" size="small">
                <el-row>
                    <el-col :span="12">
                        <el-form-item label="操作模块：">{{ form.title }} / {{ typeFormat(form) }}</el-form-item>
                        <el-form-item
                            label="登录信息："
                        >{{ form.operName }} / {{ form.operIp }} / {{ form.operLocation }}
                        </el-form-item>
                    </el-col>
                    <el-col :span="12">
                        <el-form-item label="请求地址：">{{ form.operUrl }}</el-form-item>
                        <el-form-item label="请求方式：">{{ form.requestMethod }}</el-form-item>
                    </el-col>
                    <el-col :span="24">
                        <el-form-item label="操作方法：">{{ form.method }}</el-form-item>
                    </el-col>
                    <el-col :span="24">
                        <el-form-item label="请求参数：">{{ form.operParam }}</el-form-item>
                    </el-col>
                    <el-col :span="24">
                        <el-form-item label="返回参数：">{{ form.jsonResult }}</el-form-item>
                    </el-col>
                    <el-col :span="12">
                        <el-form-item label="操作状态：">
                            <div v-if="form.status === 0">正常</div>
                            <div v-else-if="form.status === 1">失败</div>
                        </el-form-item>
                    </el-col>
                    <el-col :span="12">
                        <el-form-item label="操作时间：">{{ parseTime(form.operTime) }}</el-form-item>
                    </el-col>
                    <el-col :span="24">
                        <el-form-item label="异常信息：" v-if="form.status === 1">{{ form.errorMsg }}</el-form-item>
                    </el-col>
                </el-row>
            </el-form>
            <template #footer>
                <div class="dialog-footer">
                    <el-button @click="open = false">关 闭</el-button>
                </div>
            </template>
        </el-dialog>
    </div>
</template>

<script>
import {list, delOperlog, cleanOperlog, exportOperlog} from "@/api/monitor/operlog";

export default {
    name: "Operlog",
    data() {
        return {
            // 遮罩层
            loading: true,
            // 选中数组
            ids: [],
            // 非多个禁用
            multiple: true,
            // 显示搜索条件
            showSearch: true,
            // 总条数
            total: 0,
            // 表格数据
            list: [],
            // 是否显示弹出层
            open: false,
            // 类型数据字典
            typeOptions: [],
            // 类型数据字典
            statusOptions: [],
            // 日期范围
            dateRange: [],
            // 表单参数
            form: {},
            // 查询参数
            queryParams: {
                pageNum: 1,
                pageSize: 10,
                title: undefined,
                operName: undefined,
                businessType: undefined,
                status: undefined
            }
        };
    },
    created() {
        this.getList();
        this.getDicts("sys_oper_type").then(response => {
            this.typeOptions = response.data;
        });
        this.getDicts("sys_common_status").then(response => {
            this.statusOptions = response.data;
        });
    },
    methods: {
        /** 查询登录日志 */
        getList() {
            this.loading = true;
            list(this.addDateRange(this.queryParams, this.dateRange)).then(response => {
                    this.list = response.rows;
                    this.total = response.total;
                    this.loading = false;
                }
            );
        },
        // 操作日志状态字典翻译
        statusFormat(row, column) {
            return this.selectDictLabel(this.statusOptions, row.status);
        },
        // 操作日志类型字典翻译
        typeFormat(row, column) {
            return this.selectDictLabel(this.typeOptions, row.businessType);
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
        // 多选框选中数据
        handleSelectionChange(selection) {
            this.ids = selection.map(item => item.operId)
            this.multiple = !selection.length
        },
        /** 详细按钮操作 */
        handleView(row) {
            this.open = true;
            this.form = row;
        },
        /** 删除按钮操作 */
        handleDelete(row) {
            const operIds = row.operId || this.ids;
            this.$confirm('是否确认删除日志编号为"' + operIds + '"的数据项?', "警告", {
                confirmButtonText: "确定",
                cancelButtonText: "取消",
                type: "warning"
            }).then(function () {
                return delOperlog(operIds);
            }).then(() => {
                this.getList();
                this.msgSuccess("删除成功");
            })
        },
        /** 清空按钮操作 */
        handleClean() {
            this.$confirm('是否确认清空所有操作日志数据项?', "警告", {
                confirmButtonText: "确定",
                cancelButtonText: "取消",
                type: "warning"
            }).then(function () {
                return cleanOperlog();
            }).then(() => {
                this.getList();
                this.msgSuccess("清空成功");
            })
        },
        /** 导出按钮操作 */
        handleExport() {
            const queryParams = this.queryParams;
            this.$confirm('是否确认导出所有操作日志数据项?', "警告", {
                confirmButtonText: "确定",
                cancelButtonText: "取消",
                type: "warning"
            }).then(function () {
                return exportOperlog(queryParams);
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
  background: var(--bg-body);
}

/* Search form */
::v-deep .el-form--inline .el-form-item {
    margin-right: var(--space-4);
    margin-bottom: var(--space-4);
}

::v-deep .el-form--inline .el-form-item__label {
    font-size: var(--text-sm);
    color: var(--text-secondary);
}

::v-deep .el-form--inline .el-input__inner,
::v-deep .el-select .el-input__inner {
    border-radius: var(--radius-sm);
    border-color: var(--border-primary);
    transition: border-color var(--duration-fast) var(--ease-default),
                box-shadow var(--duration-fast) var(--ease-default);
}

::v-deep .el-form--inline .el-input__inner:focus,
::v-deep .el-select .el-input__inner:focus {
    border-color: var(--color-primary);
    box-shadow: 0 0 0 2px rgba(var(--color-primary), 0.1);
}

/* Toolbar buttons */
.mb8 {
    margin-bottom: var(--space-4);
}

::v-deep .el-button {
    border-radius: var(--radius-sm);
    font-size: var(--text-sm);
    transition: all var(--duration-fast) var(--ease-default);
}

::v-deep .el-button--primary {
    background: var(--color-primary);
    border-color: var(--color-primary);
}

::v-deep .el-button--primary:hover {
    background: var(--color-primary-600);
    border-color: var(--color-primary-600);
}

/* Table styling */
::v-deep .el-table {
    border-radius: var(--radius-md);
    overflow: hidden;
    border: 1px solid var(--border-primary);
}

::v-deep .el-table th {
    background: var(--bg-body);
    color: var(--text-primary);
    font-weight: var(--font-semibold);
    font-size: var(--text-sm);
    padding: var(--space-3) 0;
}

::v-deep .el-table td {
    color: var(--text-secondary);
    font-size: var(--text-sm);
    padding: var(--space-3) 0;
}

::v-deep .el-table--enable-row-hover .el-table__body tr:hover > td {
    background: var(--bg-hover);
    transition: background-color var(--duration-fast) var(--ease-default);
}

::v-deep .el-table__body tr {
    transition: background-color var(--duration-fast) var(--ease-default);
}

/* Dialog styling */
::v-deep .el-dialog {
    border-radius: var(--radius-lg);
    box-shadow: var(--shadow-lg);
}

::v-deep .el-dialog__header {
    padding: var(--space-4) var(--space-6);
    border-bottom: 1px solid var(--border-primary);
}

::v-deep .el-dialog__title {
    font-size: var(--text-lg);
    font-weight: var(--font-semibold);
    color: var(--text-primary);
}

::v-deep .el-dialog__body {
    padding: var(--space-6);
}

::v-deep .el-dialog__footer {
    padding: var(--space-4) var(--space-6);
    border-top: 1px solid var(--border-primary);
}

::v-deep .el-dialog .el-form-item__label {
    font-size: var(--text-sm);
    color: var(--text-secondary);
}

::v-deep .el-dialog .el-form-item__content {
    font-size: var(--text-sm);
    color: var(--text-primary);
}

/* Text buttons in table */
::v-deep .el-button--text {
    font-size: var(--text-sm);
    color: var(--color-primary);
    transition: color var(--duration-fast) var(--ease-default);
}

::v-deep .el-button--text:hover {
    color: var(--color-primary-600);
}

/* Pagination */
::v-deep .el-pagination {
    margin-top: var(--space-4);
    padding: var(--space-2) 0;
}
</style>
