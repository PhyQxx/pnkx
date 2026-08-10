<template>
    <div class="app-container">
        <el-form :model="queryParams" ref="queryForm" :inline="true" label-width="68px">
            <el-form-item label="登录地址" prop="ipaddr">
                <el-input
                    v-model="queryParams.ipaddr"
                    placeholder="请输入登录地址"
                    clearable
                    size="small"
                    @keyup.enter.native="handleQuery"
                />
            </el-form-item>
            <el-form-item label="用户名称" prop="userName">
                <el-input
                    v-model="queryParams.userName"
                    placeholder="请输入用户名称"
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
        <el-table
            v-loading="loading"
            :data="list.slice((pageNum-1)*pageSize,pageNum*pageSize)"
            style="width: 100%;"
        >
            <el-table-column label="序号" type="index" align="center">
                <template v-slot="scope">
                    <span>{{ (pageNum - 1) * pageSize + scope.$index + 1 }}</span>
                </template>
            </el-table-column>
            <el-table-column label="会话编号" align="center" prop="tokenId" :show-overflow-tooltip="true"/>
            <el-table-column label="登录名称" align="center" prop="userName" :show-overflow-tooltip="true"/>
            <el-table-column label="部门名称" align="center" prop="deptName"/>
            <el-table-column label="主机" align="center" prop="ipaddr" :show-overflow-tooltip="true"/>
            <el-table-column label="登录地点" align="center" prop="loginLocation" :show-overflow-tooltip="true"/>
            <el-table-column label="浏览器" align="center" prop="browser"/>
            <el-table-column label="操作系统" align="center" prop="os"/>
            <el-table-column label="登录时间" align="center" prop="loginTime" width="180">
                <template v-slot="scope">
                    <span>{{ parseTime(scope.row.loginTime) }}</span>
                </template>
            </el-table-column>
            <el-table-column label="操作" align="center" class-name="small-padding fixed-width">
                <template v-slot="scope">
                    <el-button
                        size="small"
                        type="text"
                        icon="Delete"
                        @click="handleForceLogout(scope.row)"
                        v-hasPermi="['monitor:online:forceLogout']"
                    >强退
                    </el-button>
                </template>
            </el-table-column>
        </el-table>

        <pagination v-show="total>queryParams.pageSize" :total="total" v-model:page="pageNum" v-model:limit="pageSize"/>
    </div>
</template>

<script>
import {list, forceLogout} from "@/api/monitor/online";

export default {
    name: "Online",
    data() {
        return {
            // 遮罩层
            loading: true,
            // 总条数
            total: 0,
            // 表格数据
            list: [],
            pageNum: 1,
            pageSize: 10,
            // 查询参数
            queryParams: {
                ipaddr: undefined,
                userName: undefined
            }
        };
    },
    created() {
        this.getList();
    },
    methods: {
        /** 查询登录日志列表 */
        getList() {
            this.loading = true;
            list(this.queryParams).then(response => {
                this.list = response.rows;
                this.total = response.total;
                this.loading = false;
            });
        },
        /** 搜索按钮操作 */
        handleQuery() {
            this.pageNum = 1;
            this.getList();
        },
        /** 重置按钮操作 */
        resetQuery() {
            this.resetForm("queryForm");
            this.handleQuery();
        },
        /** 强退按钮操作 */
        handleForceLogout(row) {
            this.$confirm('是否确认强退名称为"' + row.userName + '"的数据项?', "警告", {
                confirmButtonText: "确定",
                cancelButtonText: "取消",
                type: "warning"
            }).then(function () {
                return forceLogout(row.tokenId);
            }).then(() => {
                this.getList();
                this.msgSuccess("强退成功");
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

>>> .el-form--inline .el-input__inner {
    border-radius: var(--radius-sm);
    border-color: var(--border-primary);
    transition: border-color var(--duration-fast) var(--ease-default),
                box-shadow var(--duration-fast) var(--ease-default);
}

>>> .el-form--inline .el-input__inner:focus {
    border-color: var(--color-primary);
    box-shadow: 0 0 0 2px rgba(var(--color-primary), 0.1);
}

/* Buttons */
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
