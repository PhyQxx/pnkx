<template>
    <div class="app-container">
        <el-form :model="queryParams" ref="queryForm" :inline="true" v-show="showSearch" label-width="68px">
            <el-form-item label="任务名称" prop="jobName">
                <el-input
                    v-model="queryParams.jobName"
                    placeholder="请输入任务名称"
                    clearable
                    size="small"
                    style="width: 240px"
                    @keyup.enter.native="handleQuery"
                />
            </el-form-item>
            <el-form-item label="任务组名" prop="jobGroup">
                <el-select
                    v-model="queryParams.jobGroup"
                    placeholder="请任务组名"
                    clearable
                    size="small"
                    style="width: 240px"
                >
                    <el-option
                        v-for="dict in jobGroupOptions"
                        :key="dict.dictValue"
                        :label="dict.dictLabel"
                        :value="dict.dictValue"
                    />
                </el-select>
            </el-form-item>
            <el-form-item label="执行状态" prop="status">
                <el-select
                    v-model="queryParams.status"
                    placeholder="请选择执行状态"
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
            <el-form-item label="执行时间">
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
                    v-hasPermi="['monitor:job:remove']"
                >删除
                </el-button>
            </el-col>
            <el-col :span="1.5">
                <el-button
                    type="danger"
                                        icon="Delete"
                    size="small"
                    @click="handleClean"
                    v-hasPermi="['monitor:job:remove']"
                >清空
                </el-button>
            </el-col>
            <el-col :span="1.5">
                <el-button
                    type="warning"
                                        icon="Download"
                    size="small"
                    @click="handleExport"
                    v-hasPermi="['monitor:job:export']"
                >导出
                </el-button>
            </el-col>
            <right-toolbar :showSearch="showSearch" @queryTable="getList"></right-toolbar>
        </el-row>

        <el-table v-loading="loading" :data="jobLogList" @selection-change="handleSelectionChange">
            <el-table-column type="selection" width="55" align="center"/>
            <el-table-column label="日志编号" width="80" align="center" prop="jobLogId"/>
            <el-table-column label="任务名称" align="center" prop="jobName" :show-overflow-tooltip="true"/>
            <el-table-column label="任务组名" align="center" prop="jobGroup" :formatter="jobGroupFormat"
                             :show-overflow-tooltip="true"/>
            <el-table-column label="调用目标字符串" align="center" prop="invokeTarget" :show-overflow-tooltip="true"/>
            <el-table-column label="日志信息" align="center" prop="jobMessage" :show-overflow-tooltip="true"/>
            <el-table-column label="执行状态" align="center" prop="status" :formatter="statusFormat"/>
            <el-table-column label="执行时间" align="center" prop="createTime" width="180">
                <template v-slot="scope">
                    <span>{{ parseTime(scope.row.createTime) }}</span>
                </template>
            </el-table-column>
            <el-table-column label="操作" align="center" class-name="small-padding fixed-width">
                <template v-slot="scope">
                    <el-button
                        size="small"
                        type="text"
                        icon="View"
                        @click="handleView(scope.row)"
                        v-hasPermi="['monitor:job:query']"
                    >详细
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

        <!-- 调度日志详细 -->
        <el-dialog title="调度日志详细" v-model="open" width="700px" append-to-body>
            <el-form ref="form" :model="form" label-width="100px" size="small">
                <el-row>
                    <el-col :span="12">
                        <el-form-item label="日志序号：">{{ form.jobLogId }}</el-form-item>
                        <el-form-item label="任务名称：">{{ form.jobName }}</el-form-item>
                    </el-col>
                    <el-col :span="12">
                        <el-form-item label="任务分组：">{{ form.jobGroup }}</el-form-item>
                        <el-form-item label="执行时间：">{{ form.createTime }}</el-form-item>
                    </el-col>
                    <el-col :span="24">
                        <el-form-item label="调用方法：">{{ form.invokeTarget }}</el-form-item>
                    </el-col>
                    <el-col :span="24">
                        <el-form-item label="日志信息：">{{ form.jobMessage }}</el-form-item>
                    </el-col>
                    <el-col :span="24">
                        <el-form-item label="执行状态：">
                            <div v-if="form.status == 0">正常</div>
                            <div v-else-if="form.status == 1">失败</div>
                        </el-form-item>
                    </el-col>
                    <el-col :span="24">
                        <el-form-item label="异常信息：" v-if="form.status == 1">{{ form.exceptionInfo }}</el-form-item>
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
import {listJobLog, delJobLog, exportJobLog, cleanJobLog} from "@/api/monitor/jobLog";

export default {
    name: "JobLog",
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
            // 调度日志表格数据
            jobLogList: [],
            // 是否显示弹出层
            open: false,
            // 日期范围
            dateRange: [],
            // 表单参数
            form: {},
            // 执行状态字典
            statusOptions: [],
            // 任务组名字典
            jobGroupOptions: [],
            // 查询参数
            queryParams: {
                pageNum: 1,
                pageSize: 10,
                jobName: undefined,
                jobGroup: undefined,
                status: undefined
            }
        };
    },
    created() {
        this.getList();
        this.getDicts("sys_job_status").then(response => {
            this.statusOptions = response.data;
        });
        this.getDicts("sys_job_group").then(response => {
            this.jobGroupOptions = response.data;
        });
    },
    methods: {
        /** 查询调度日志列表 */
        getList() {
            this.loading = true;
            listJobLog(this.addDateRange(this.queryParams, this.dateRange)).then(response => {
                    this.jobLogList = response.rows;
                    this.total = response.total;
                    this.loading = false;
                }
            );
        },
        // 执行状态字典翻译
        statusFormat(row, column) {
            return this.selectDictLabel(this.statusOptions, row.status);
        },
        // 任务组名字典翻译
        jobGroupFormat(row, column) {
            return this.selectDictLabel(this.jobGroupOptions, row.jobGroup);
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
            this.ids = selection.map(item => item.jobLogId);
            this.multiple = !selection.length;
        },
        /** 详细按钮操作 */
        handleView(row) {
            this.open = true;
            this.form = row;
        },
        /** 删除按钮操作 */
        handleDelete(row) {
            const jobLogIds = this.ids;
            this.$confirm('是否确认删除调度日志编号为"' + jobLogIds + '"的数据项?', "警告", {
                confirmButtonText: "确定",
                cancelButtonText: "取消",
                type: "warning"
            }).then(function () {
                return delJobLog(jobLogIds);
            }).then(() => {
                this.getList();
                this.msgSuccess("删除成功");
            })
        },
        /** 清空按钮操作 */
        handleClean() {
            this.$confirm("是否确认清空所有调度日志数据项?", "警告", {
                confirmButtonText: "确定",
                cancelButtonText: "取消",
                type: "warning"
            }).then(function () {
                return cleanJobLog();
            }).then(() => {
                this.getList();
                this.msgSuccess("清空成功");
            })
        },
        /** 导出按钮操作 */
        handleExport() {
            const queryParams = this.queryParams;
            this.$confirm("是否确认导出所有调度日志数据项?", "警告", {
                confirmButtonText: "确定",
                cancelButtonText: "取消",
                type: "warning"
            }).then(function () {
                return exportJobLog(queryParams);
            }).then(response => {
                this.download(response.msg);
            })
        }
    }
};
</script>

<style scoped>
.app-container {
    padding: var(--space-6);
}

/* Search form */
>>> .el-form--inline .el-form-item {
    margin-right: var(--space-4);
    margin-bottom: var(--space-4);
}

>>> .el-form--inline .el-form-item__label {
    font-size: var(--text-sm);
    color: var(--text-secondary);
}

>>> .el-form--inline .el-input__inner,
>>> .el-form--inline .el-select .el-input__inner {
    border-radius: var(--radius-sm);
    border-color: var(--border-primary);
    transition: border-color var(--duration-fast) var(--ease-default),
                box-shadow var(--duration-fast) var(--ease-default);
}

>>> .el-form--inline .el-input__inner:focus,
>>> .el-form--inline .el-select .el-input__inner:focus {
    border-color: var(--color-primary);
    box-shadow: 0 0 0 2px rgba(var(--color-primary), 0.1);
}

/* Toolbar buttons */
.mb8 {
    margin-bottom: var(--space-4);
}

>>> .el-button {
    border-radius: var(--radius-sm);
    font-size: var(--text-sm);
    transition: all var(--duration-fast) var(--ease-default);
}

>>> .el-button--primary {
    background: var(--color-primary);
    border-color: var(--color-primary);
}

>>> .el-button--primary:hover {
    background: var(--color-primary-600);
    border-color: var(--color-primary-600);
}

/* Table styling */
>>> .el-table {
    border-radius: var(--radius-md);
    overflow: hidden;
    border: 1px solid var(--border-primary);
}

>>> .el-table th {
    background: var(--bg-body);
    color: var(--text-primary);
    font-weight: var(--font-semibold);
    font-size: var(--text-sm);
    padding: var(--space-3) 0;
}

>>> .el-table td {
    color: var(--text-secondary);
    font-size: var(--text-sm);
    padding: var(--space-3) 0;
}

>>> .el-table--enable-row-hover .el-table__body tr:hover > td {
    background: var(--bg-hover);
    transition: background-color var(--duration-fast) var(--ease-default);
}

>>> .el-table__body tr {
    transition: background-color var(--duration-fast) var(--ease-default);
}

/* Dialog styling */
>>> .el-dialog {
    border-radius: var(--radius-lg);
    box-shadow: var(--shadow-lg);
}

>>> .el-dialog__header {
    padding: var(--space-4) var(--space-6);
    border-bottom: 1px solid var(--border-primary);
}

>>> .el-dialog__title {
    font-size: var(--text-lg);
    font-weight: var(--font-semibold);
    color: var(--text-primary);
}

>>> .el-dialog__body {
    padding: var(--space-6);
}

>>> .el-dialog__footer {
    padding: var(--space-4) var(--space-6);
    border-top: 1px solid var(--border-primary);
}

>>> .el-dialog .el-form-item__label {
    font-size: var(--text-sm);
    color: var(--text-secondary);
}

>>> .el-dialog .el-form-item__content {
    font-size: var(--text-sm);
    color: var(--text-primary);
}

/* Text buttons in table */
>>> .el-button--text {
    font-size: var(--text-sm);
    color: var(--color-primary);
    transition: color var(--duration-fast) var(--ease-default);
}

>>> .el-button--text:hover {
    color: var(--color-primary-600);
}

/* Pagination */
>>> .el-pagination {
    margin-top: var(--space-4);
    padding: var(--space-2) 0;
}
</style>
