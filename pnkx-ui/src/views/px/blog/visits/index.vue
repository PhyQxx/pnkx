<template>
    <div class="app-container">
        <el-form :inline="true" :model="queryParams" label-width="68px" ref="queryForm" v-show="showSearch">
            <el-form-item label="IP地址" prop="ip">
                <el-input
                    @keyup.enter.native="handleQuery"
                    clearable
                    placeholder="请输入IP地址"
                    size="small"
                    v-model="queryParams.ip"
                />
            </el-form-item>
            <el-form-item label="国家" prop="country">
                <el-input
                    @keyup.enter.native="handleQuery"
                    clearable
                    placeholder="请输入国家"
                    size="small"
                    v-model="queryParams.country"
                />
            </el-form-item>
            <el-form-item label="省份" prop="province">
                <el-input
                    @keyup.enter.native="handleQuery"
                    clearable
                    placeholder="请输入省份"
                    size="small"
                    v-model="queryParams.province"
                />
            </el-form-item>
            <el-form-item label="城市" prop="city">
                <el-input
                    @keyup.enter.native="handleQuery"
                    clearable
                    placeholder="请输入城市"
                    size="small"
                    v-model="queryParams.city"
                />
            </el-form-item>
            <el-form-item label="运营商" prop="isp">
                <el-input
                    @keyup.enter.native="handleQuery"
                    clearable
                    placeholder="请输入运营商"
                    size="small"
                    v-model="queryParams.isp"
                />
            </el-form-item>
            <el-form-item label="经纬度" prop="location">
                <el-input
                    @keyup.enter.native="handleQuery"
                    clearable
                    placeholder="请输入运营商"
                    size="small"
                    v-model="queryParams.location"
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
            <right-toolbar :showSearch="showSearch" @queryTable="getList"></right-toolbar>
        </el-row>

        <el-table :data="visitsList" @selection-change="handleSelectionChange" v-loading="loading">
            <el-table-column align="center" type="selection" width="55"/>
            <el-table-column align="center" label="IP地址" prop="ip"/>
            <el-table-column align="center" label="国家" prop="country"/>
            <el-table-column align="center" label="省份" prop="province"/>
            <el-table-column align="center" label="城市" prop="city"/>
            <el-table-column align="center" label="运营商" prop="isp"/>
            <el-table-column align="center" label="经纬度" prop="location"/>
            <el-table-column align="center" label="创建时间" prop="createTime"/>
            <el-table-column align="center" label="备注" prop="remark"/>
            <el-table-column align="center" class-name="small-padding fixed-width" label="操作">
                <template v-slot="scope">
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

    </div>
</template>

<script>
import {listVisits, delVisits, exportVisits} from "@/api/px/blog/visits";

export default {
    name: "Visits",
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
            // 访客表格数据
            visitsList: [],
            // 弹出层标题
            title: "",
            // 是否显示弹出层
            open: false,
            // 查询参数
            queryParams: {
                pageNum: 1,
                pageSize: 10,
                country: null,
                ip: null,
                province: null,
                version: null,
                city: null,
                district: null,
                isp: null,
                location: null,
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
        /** 查询访客列表 */
        getList() {
            this.loading = true;
            listVisits(this.queryParams).then(response => {
                this.visitsList = response.rows;
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
                country: null,
                ip: null,
                province: null,
                version: null,
                city: null,
                createBy: null,
                district: null,
                createTime: null,
                isp: null,
                updateBy: null,
                updateTime: null,
                location: null,
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
        /** 删除按钮操作 */
        handleDelete(row) {
            const ids = row.id || this.ids;
            this.$confirm('是否确认删除访客编号为"' + ids + '"的数据项?', "警告", {
                confirmButtonText: "确定",
                cancelButtonText: "取消",
                type: "warning"
            }).then(function () {
                return delVisits(ids);
            }).then(() => {
                this.getList();
                this.msgSuccess("删除成功");
            })
        },
        /** 导出按钮操作 */
        handleExport() {
            const queryParams = this.queryParams;
            this.$confirm('是否确认导出所有访客数据项?', "警告", {
                confirmButtonText: "确定",
                cancelButtonText: "取消",
                type: "warning"
            }).then(function () {
                return exportVisits(queryParams);
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

    .mb8 {
        margin-bottom: var(--space-2);
    }
}
</style>
