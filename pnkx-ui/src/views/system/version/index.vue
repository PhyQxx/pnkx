<template>
    <div class="app-container">
        <el-form v-show="showSearch" ref="queryForm" :inline="true" :model="queryParams" label-width="68px">
            <el-form-item label="平台" prop="appType">
                <el-select v-model="queryParams.appType" clearable placeholder="请选择平台" size="small">
                    <el-option label="Android" value="android"/>
                    <el-option label="iOS" value="ios"/>
                </el-select>
            </el-form-item>
            <el-form-item label="状态" prop="status">
                <el-select v-model="queryParams.status" clearable placeholder="请选择状态" size="small">
                    <el-option label="正常" :value="1"/>
                    <el-option label="下线" :value="0"/>
                </el-select>
            </el-form-item>
            <el-form-item>
                <el-button icon="Search" size="small" type="primary" @click="handleQuery">搜索</el-button>
                <el-button icon="Refresh" size="small" @click="resetQuery">重置</el-button>
            </el-form-item>
        </el-form>

        <el-row :gutter="10" class="mb8">
            <el-col :span="1.5">
                <el-button icon="Plus" size="small" type="primary" @click="handleAdd">新增</el-button>
            </el-col>
            <el-col :span="1.5">
                <el-button :disabled="single" icon="Edit" size="small" type="success" @click="handleUpdate">修改</el-button>
            </el-col>
            <el-col :span="1.5">
                <el-button :disabled="multiple" icon="Delete" size="small" type="danger" @click="handleDelete">删除</el-button>
            </el-col>
            <right-toolbar :showSearch="showSearch" @queryTable="getList"></right-toolbar>
        </el-row>

        <el-table v-loading="loading" :data="versionList" @selection-change="handleSelectionChange">
            <el-table-column align="center" type="selection" width="55"/>
            <el-table-column align="center" label="平台" prop="appType" width="100">
                <template v-slot="scope">
                    <el-tag :type="scope.row.appType === 'ios' ? 'warning' : 'success'" size="small">
                        {{ scope.row.appType === 'ios' ? 'iOS' : 'Android' }}
                    </el-tag>
                </template>
            </el-table-column>
            <el-table-column align="center" label="版本名称" prop="versionName" width="120"/>
            <el-table-column align="center" label="版本编号" prop="versionCode" width="100"/>
            <el-table-column align="center" label="更新类型" prop="updateType" width="100">
                <template v-slot="scope">
                    <el-tag :type="scope.row.updateType === 0 ? 'primary' : 'warning'" size="small">
                        {{ scope.row.updateType === 0 ? 'WGT热更新' : '整包更新' }}
                    </el-tag>
                </template>
            </el-table-column>
            <el-table-column align="center" label="强制更新" prop="forceUpdate" width="100">
                <template v-slot="scope">
                    <el-tag :type="scope.row.forceUpdate === 1 ? 'danger' : 'info'" size="small">
                        {{ scope.row.forceUpdate === 1 ? '是' : '否' }}
                    </el-tag>
                </template>
            </el-table-column>
            <el-table-column :show-overflow-tooltip="true" align="center" label="下载地址" prop="downloadUrl"/>
            <el-table-column align="center" label="状态" prop="status" width="80">
                <template v-slot="scope">
                    <el-tag :type="scope.row.status === 1 ? 'success' : 'info'" size="small">
                        {{ scope.row.status === 1 ? '生效' : '下线' }}
                    </el-tag>
                </template>
            </el-table-column>
            <el-table-column align="center" label="创建时间" prop="createTime" width="160"/>
            <el-table-column align="center" class-name="small-padding fixed-width" label="操作" width="150">
                <template v-slot="scope">
                    <el-button icon="Edit" size="small" type="text" @click="handleUpdate(scope.row)">修改</el-button>
                    <el-button icon="Delete" size="small" type="text" @click="handleDelete(scope.row)">删除</el-button>
                </template>
            </el-table-column>
        </el-table>

        <pagination
            v-show="total > 0"
            v-model:limit="queryParams.pageSize"
            v-model:page="queryParams.pageNum"
            :total="total"
            @pagination="getList"
        />

        <!-- 添加或修改App版本对话框 -->
        <el-dialog :title="title" v-model="open" width="600px" append-to-body>
            <el-form ref="form" :model="form" :rules="rules" label-width="100px">
                <el-form-item label="平台" prop="appType">
                    <el-select v-model="form.appType" placeholder="请选择平台">
                        <el-option label="Android" value="android"/>
                        <el-option label="iOS" value="ios"/>
                    </el-select>
                </el-form-item>
                <el-form-item label="版本名称" prop="versionName">
                    <el-input v-model="form.versionName" placeholder="如 2.0.8"/>
                </el-form-item>
                <el-form-item label="版本编号" prop="versionCode">
                    <el-input-number v-model="form.versionCode" :min="1" placeholder="如 208"/>
                </el-form-item>
                <el-form-item label="更新类型" prop="updateType">
                    <el-radio-group v-model="form.updateType">
                        <el-radio :value="0">WGT热更新</el-radio>
                        <el-radio :value="1">整包更新</el-radio>
                    </el-radio-group>
                </el-form-item>
                <el-form-item label="强制更新" prop="forceUpdate">
                    <el-switch v-model="form.forceUpdate" :active-value="1" :inactive-value="0"/>
                </el-form-item>
                <el-form-item label="下载地址" prop="downloadUrl">
                    <el-input v-model="form.downloadUrl" placeholder="WGT/APK文件的下载地址"/>
                </el-form-item>
                <el-form-item label="更新日志" prop="updateLog">
                    <el-input v-model="form.updateLog" :rows="4" placeholder="请输入更新日志" type="textarea"/>
                </el-form-item>
                <el-form-item label="状态" prop="status">
                    <el-radio-group v-model="form.status">
                        <el-radio :value="1">正常生效</el-radio>
                        <el-radio :value="0">下线</el-radio>
                    </el-radio-group>
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
import {addVersion, delVersion, getVersion, listVersion, updateVersion} from "@/api/system/version";

export default {
    name: "AppVersion",
    data() {
        return {
            loading: true,
            ids: [],
            single: true,
            multiple: true,
            showSearch: true,
            total: 0,
            versionList: [],
            title: "",
            open: false,
            queryParams: {
                pageNum: 1,
                pageSize: 10,
                appType: undefined,
                status: undefined
            },
            form: {},
            rules: {
                appType: {required: true, message: '请选择平台', trigger: 'change'},
                versionName: {required: true, message: '请输入版本名称', trigger: 'blur'},
                versionCode: {required: true, message: '请输入版本编号', trigger: 'blur'},
                updateType: {required: true, message: '请选择更新类型', trigger: 'change'},
                downloadUrl: {required: true, message: '请输入下载地址', trigger: 'blur'},
                status: {required: true, message: '请选择状态', trigger: 'change'}
            }
        };
    },
    created() {
        this.getList();
    },
    methods: {
        getList() {
            this.loading = true;
            listVersion(this.queryParams).then(response => {
                this.versionList = response.rows;
                this.total = response.total;
                this.loading = false;
            });
        },
        cancel() {
            this.open = false;
            this.reset();
        },
        reset() {
            this.form = {
                id: null,
                appType: 'android',
                versionName: null,
                versionCode: null,
                updateType: 0,
                forceUpdate: 0,
                downloadUrl: null,
                updateLog: null,
                status: 1
            };
            this.resetForm("form");
        },
        handleQuery() {
            this.queryParams.pageNum = 1;
            this.getList();
        },
        resetQuery() {
            this.resetForm("queryForm");
            this.handleQuery();
        },
        handleSelectionChange(selection) {
            this.ids = selection.map(item => item.id);
            this.single = selection.length !== 1;
            this.multiple = !selection.length;
        },
        handleAdd() {
            this.reset();
            this.open = true;
            this.title = "新增App版本";
        },
        handleUpdate(row) {
            this.reset();
            const id = row.id || this.ids;
            getVersion(id).then(response => {
                this.form = response.data;
                this.open = true;
                this.title = "修改App版本";
            });
        },
        submitForm() {
            this.$refs["form"].validate(valid => {
                if (valid) {
                    if (this.form.id != null) {
                        updateVersion(this.form).then(() => {
                            this.msgSuccess("修改成功");
                            this.open = false;
                            this.getList();
                        });
                    } else {
                        addVersion(this.form).then(() => {
                            this.msgSuccess("新增成功");
                            this.open = false;
                            this.getList();
                        });
                    }
                }
            });
        },
        handleDelete(row) {
            const ids = row.id || this.ids;
            this.$confirm('是否确认删除该版本记录？', "警告", {
                confirmButtonText: "确定",
                cancelButtonText: "取消",
                type: "warning"
            }).then(function () {
                return delVersion(ids);
            }).then(() => {
                this.getList();
                this.msgSuccess("删除成功");
            });
        }
    }
};
</script>

<style lang="scss" scoped>
.app-container {
    padding: var(--space-6);
}

.dialog-footer {
    display: flex;
    justify-content: flex-end;
    gap: var(--space-2);
}

:deep(.el-table) {
    border-radius: var(--radius-lg);
    overflow: hidden;

    th {
        background: var(--bg-body);
        color: var(--text-primary);
        font-weight: var(--font-semibold);
        font-size: var(--text-sm);
    }

    td {
        color: var(--text-secondary);
        font-size: var(--text-sm);
    }

    .el-button--text {
        transition: color var(--duration-fast) var(--ease-default);
    }
}

:deep(.el-dialog) {
    border-radius: var(--radius-lg);

    .el-dialog__header {
        border-bottom: 1px solid var(--border-primary);
        padding: var(--space-4) var(--space-6);
    }

    .el-dialog__body {
        padding: var(--space-6);
    }
}
</style>
