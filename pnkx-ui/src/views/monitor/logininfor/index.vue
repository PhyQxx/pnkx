<template>
    <div class="app-container">
        <el-form :model="queryParams" ref="queryForm" :inline="true" v-show="showSearch" label-width="68px">
            <el-form-item label="登录地址" prop="ipaddr">
                <el-input
                    v-model="queryParams.ipaddr"
                    placeholder="请输入登录地址"
                    clearable
                    style="width: 240px;"
                    size="small"
                    @keyup.enter.native="handleQuery"
                />
            </el-form-item>
            <el-form-item label="用户名称" prop="userName">
                <el-input
                    v-model="queryParams.userName"
                    placeholder="请输入用户名称"
                    clearable
                    style="width: 240px;"
                    size="small"
                    @keyup.enter.native="handleQuery"
                />
            </el-form-item>
            <el-form-item label="状态" prop="status">
                <el-select
                    v-model="queryParams.status"
                    placeholder="登录状态"
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
            <el-form-item label="登录时间">
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
                    v-hasPermi="['monitor:logininfor:remove']"
                >删除
                </el-button>
            </el-col>
            <el-col :span="1.5">
                <el-button
                    type="danger"
                                        icon="Delete"
                    size="small"
                    @click="handleClean"
                    v-hasPermi="['monitor:logininfor:remove']"
                >清空
                </el-button>
            </el-col>
            <el-col :span="1.5">
                <el-button
                    type="warning"
                                        icon="Download"
                    size="small"
                    @click="handleExport"
                    v-hasPermi="['system:logininfor:export']"
                >导出
                </el-button>
            </el-col>
            <right-toolbar :showSearch="showSearch" @queryTable="getList"></right-toolbar>
        </el-row>

        <div class="table-main-area">
            <el-table v-loading="loading" :data="list" @selection-change="handleSelectionChange">
                <el-table-column type="selection" width="55" align="center"/>
                <el-table-column label="访问编号" align="center" prop="infoId"/>
                <el-table-column label="用户名称" align="center" prop="userName"/>
                <el-table-column label="登录地址" align="center" prop="ipaddr" width="130" :show-overflow-tooltip="true"/>
                <el-table-column label="登录地点" align="center" prop="loginLocation" :show-overflow-tooltip="true"/>
                <el-table-column label="浏览器" align="center" prop="browser"/>
                <el-table-column label="操作系统" align="center" prop="os"/>
                <el-table-column label="登录状态" align="center" prop="status" :formatter="statusFormat"/>
                <el-table-column label="操作信息" align="center" prop="msg"/>
                <el-table-column label="登录日期" align="center" prop="loginTime" width="180">
                    <template v-slot="scope">
                        <span>{{ parseTime(scope.row.loginTime) }}</span>
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
    </div>
</template>

<script>
import {list, delLogininfor, cleanLogininfor, exportLogininfor} from "@/api/monitor/logininfor";

export default {
    name: "Logininfor",
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
            // 状态数据字典
            statusOptions: [],
            // 日期范围
            dateRange: [],
            // 查询参数
            queryParams: {
                pageNum: 1,
                pageSize: 10,
                ipaddr: undefined,
                userName: undefined,
                status: undefined
            }
        };
    },
    created() {
        this.getList();
        this.getDicts("sys_common_status").then(response => {
            this.statusOptions = response.data;
        });
    },
    methods: {
        /** 查询登录日志列表 */
        getList() {
            this.loading = true;
            list(this.addDateRange(this.queryParams, this.dateRange)).then(response => {
                    this.list = response.rows;
                    this.total = response.total;
                    this.loading = false;
                }
            );
        },
        // 登录状态字典翻译
        statusFormat(row, column) {
            return this.selectDictLabel(this.statusOptions, row.status);
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
            this.ids = selection.map(item => item.infoId)
            this.multiple = !selection.length
        },
        /** 删除按钮操作 */
        handleDelete(row) {
            const infoIds = row.infoId || this.ids;
            this.$confirm('是否确认删除访问编号为"' + infoIds + '"的数据项?', "警告", {
                confirmButtonText: "确定",
                cancelButtonText: "取消",
                type: "warning"
            }).then(function () {
                return delLogininfor(infoIds);
            }).then(() => {
                this.getList();
                this.msgSuccess("删除成功");
            })
        },
        /** 清空按钮操作 */
        handleClean() {
            this.$confirm('是否确认清空所有登录日志数据项?', "警告", {
                confirmButtonText: "确定",
                cancelButtonText: "取消",
                type: "warning"
            }).then(function () {
                return cleanLogininfor();
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
                return exportLogininfor(queryParams);
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

/* Pagination */
::v-deep .el-pagination {
    margin-top: var(--space-4);
    padding: var(--space-2) 0;
}
</style>
