<template>
    <div class="app-container">
        <el-form :model="queryParams" ref="queryForm" :inline="true" v-show="showSearch" label-width="68px">
            <el-form-item label="公告标题" prop="noticeTitle">
                <el-input
                    v-model="queryParams.noticeTitle"
                    placeholder="请输入公告标题"
                    clearable
                    size="small"
                    @keyup.enter.native="handleQuery"
                />
            </el-form-item>
            <el-form-item label="操作人员" prop="createBy">
                <el-input
                    v-model="queryParams.createBy"
                    placeholder="请输入操作人员"
                    clearable
                    size="small"
                    @keyup.enter.native="handleQuery"
                />
            </el-form-item>
            <el-form-item label="类型" prop="noticeType">
                <el-select v-model="queryParams.noticeType" placeholder="公告类型" clearable size="small">
                    <el-option
                        v-for="dict in typeOptions"
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
                    v-hasPermi="['system:notice:add']"
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
                    v-hasPermi="['system:notice:edit']"
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
                    v-hasPermi="['system:notice:remove']"
                >删除
                </el-button>
            </el-col>
            <right-toolbar :showSearch="showSearch" @queryTable="getList"></right-toolbar>
        </el-row>

        <el-table v-loading="loading" :data="noticeList" @selection-change="handleSelectionChange">
            <el-table-column type="selection" width="55" align="center"/>
            <el-table-column label="序号" align="center" width="100">
                <template v-slot="scope">
                    <span>{{ scope.$index + 1 }}</span>
                </template>
            </el-table-column>
            <el-table-column
                label="公告标题"
                align="center"
                :show-overflow-tooltip="true">
                <template v-slot="scope">
                    <span class="notice-title" @click="toNoticeDetails(scope.row)">{{ scope.row.noticeTitle }}</span>
                </template>
            </el-table-column>
            <el-table-column
                label="公告类型"
                align="center"
                prop="noticeType"
                :formatter="typeFormat"
                width="100"
            />
            <el-table-column
                align="center"
                label="公告标签"
                prop="remark"
            />
            <el-table-column
                label="阅读量"
                align="center"
                prop="read"
                width="100"
            >
                <template v-slot="scope">
                    <span class="theme-blue-text" @click="handleQueryRead(scope.row)">{{ scope.row.read || 0 }}</span>
                </template>
            </el-table-column>
            <el-table-column
                label="状态"
                align="center"
                prop="status"
                :formatter="statusFormat"
                width="100"
            />
            <el-table-column label="创建者" align="center" prop="author" width="100"/>
            <el-table-column label="创建时间" align="center" prop="createTime" width="100">
                <template v-slot="scope">
                    <span>{{ parseTime(scope.row.createTime, '{y}-{m}-{d}') }}</span>
                </template>
            </el-table-column>
            <el-table-column label="操作" align="center" class-name="small-padding fixed-width">
                <template v-slot="scope">
                    <el-button
                        size="small"
                        type="text"
                        icon="Edit"
                        @click="handleUpdate(scope.row)"
                        v-if="scope.row.createBy == $store.getters.id"
                        v-hasPermi="['system:notice:edit']"
                    >修改
                    </el-button>
                    <el-button
                        size="small"
                        type="text"
                        icon="Delete"
                        @click="handleDelete(scope.row)"
                        v-if="scope.row.createBy == $store.getters.id"
                        v-hasPermi="['system:notice:remove']"
                    >删除
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

        <!-- 添加或修改公告对话框 -->
        <el-dialog :close-on-click-modal="true" :title="title" v-model="open" append-to-body width="80vw">
            <el-form ref="form" :model="form" :rules="rules" label-width="80px">
                <el-row>
                    <el-col :span="24">
                        <el-form-item label="公告标题" prop="noticeTitle">
                            <el-input v-model="form.noticeTitle" placeholder="请输入公告标题"/>
                        </el-form-item>
                    </el-col>
                    <el-col :span="24">
                        <el-form-item label="公告类型" prop="noticeType">
                            <el-select v-model="form.noticeType" placeholder="请选择">
                                <el-option
                                    v-for="dict in typeOptions"
                                    :key="dict.dictValue"
                                    :label="dict.dictLabel"
                                    :value="dict.dictValue"
                                ></el-option>
                            </el-select>
                        </el-form-item>
                    </el-col>
                    <el-col :span="24">
                        <el-form-item label="公告标签" prop="remark">
                            <el-input maxlength="4"
                                      placeholder="请输入公告标签"
                                      show-word-limit
                                      v-model="form.remark"/>
                        </el-form-item>
                    </el-col>
                    <el-col :span="24">
                        <el-form-item label="状态">
                            <el-radio-group v-model="form.status">
                                <el-radio
                                    v-for="dict in statusOptions"
                                    :key="dict.dictValue"
                                    :label="dict.dictValue"
                                >{{ dict.dictLabel }}
                                </el-radio>
                            </el-radio-group>
                        </el-form-item>
                    </el-col>
                    <el-col :span="24">
                        <el-form-item label="内容">
                            <CherryMarkdownEditor ref="CherryMarkdown" v-if="cherryMarkDownEditorFlag" height="40vh"
                                                  v-model="form.contentMd"></CherryMarkdownEditor>
                        </el-form-item>
                    </el-col>
                </el-row>
            </el-form>
            <template #footer>
                <div class="dialog-footer">
                    <el-button @click="cancel">取 消</el-button>
                    <el-button type="primary" @click="submitForm">确 定</el-button>
                </div>
            </template>
        </el-dialog>
        <el-dialog title="通知公告已读列表" v-model="readOpen" append-to-body width="60vw">
            <el-table
                :data="readList"
                stripe
                style="width: 100%">
                <el-table-column align="center" prop="nickName" label="用户名"></el-table-column>
                <el-table-column align="center" prop="ip" label="IP"></el-table-column>
                <el-table-column align="center" prop="location" label="经纬度"></el-table-column>
                <el-table-column align="center" prop="country" label="国家"></el-table-column>
                <el-table-column align="center" prop="province" label="省"></el-table-column>
                <el-table-column align="center" prop="city" label="城市"></el-table-column>
                <el-table-column align="center" prop="createTime" label="阅读时间"></el-table-column>
            </el-table>
            <pagination
                v-show="readTotal>0"
                :total="readTotal"
                v-model:page="readQueryParams.pageNum"
                v-model:limit="readQueryParams.pageSize"
                @pagination="handleQueryRead"
            />
        </el-dialog>

    </div>
</template>

<script>
import {addNotice, delNotice, getNotice, listNotice, updateNotice, selectNoticeRead} from "@/api/system/notice";

export default {
    name: "Notice",
    components: {},
    data() {
        return {
            // 编辑器标志
            cherryMarkDownEditorFlag: true,
            //用户信息
            userInfo: {
                userId: 0
            },
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
            // 阅读总条数
            readTotal: 0,
            // 公告表格数据
            noticeList: [],
            // 已读列表
            readList: [],
            // 弹出层标题
            title: "",
            // 是否显示弹出层
            open: false,
            // 已读列表弹出层
            readOpen: false,
            // 类型数据字典
            statusOptions: [],
            // 状态数据字典
            typeOptions: [],
            // 阅读查询参数
            readQueryParams: {
                pageNum: 1,
                pageSize: 10,
            },
            // 查询参数
            queryParams: {
            pageNum: 1,
                pageSize: 10,
                noticeTitle: undefined,
                createBy: undefined,
                status: undefined
            },
            // 表单参数
            form: {},
            // 表单校验
            rules: {
                noticeTitle: [
                    {required: true, message: "请输入公告标题", trigger: "blur"}
                ],
                noticeType: [
                    {required: true, message: "请选择公告类型", trigger: "change"}
                ],
                remark: [
                    {required: true, message: "请输入通知标签", trigger: "blur"}
                ]
            }
        };
    },
    created() {
        this.getUserProfile();
        this.getList();
        this.getDicts("sys_notice_status").then(response => {
            this.statusOptions = response.data;
        });
        this.getDicts("sys_notice_type").then(response => {
            this.typeOptions = response.data;
        });
    },
    methods: {
        /**
         * 查看阅读列表
         * @param row
         */
        handleQueryRead(row) {
            if (row.noticeId) {
                this.readQueryParams.noticeId = row.noticeId
            }
            selectNoticeRead(this.readQueryParams).then(response => {
                this.readList = response.rows;
                this.readTotal = response.total;
                this.readOpen = true;
            });
        },
        /**
         * 跳转到通知详情页面
         */
        toNoticeDetails(row) {
            this.$router.push({
                path: 'noticedetail',
                query: {
                    noticeId: row.noticeId
                }
            })
        },
        /** 查询公告列表 */
        getList() {
            this.loading = true;
            listNotice(this.queryParams).then(response => {
                this.noticeList = response.rows;
                this.total = response.total;
                this.loading = false;
            });
        },
        // 公告状态字典翻译
        statusFormat(row, column) {
            return this.selectDictLabel(this.statusOptions, row.status);
        },
        // 公告状态字典翻译
        typeFormat(row, column) {
            return this.selectDictLabel(this.typeOptions, row.noticeType);
        },
        // 取消按钮
        cancel() {
            this.open = false;
            this.reset();
        },
        // 表单重置
        reset() {
            this.form = {
                noticeId: undefined,
                noticeTitle: undefined,
                noticeType: undefined,
                noticeContent: undefined,
                status: "0"
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
            this.ids = selection.map(item => item.noticeId);
            this.single = selection.length !== 1;
            this.multiple = !selection.length;
        },
        /** 新增按钮操作 */
        handleAdd() {
            this.reset();
            this.open = true;
            this.title = "添加公告";
            this.cherryMarkDownEditorFlag = false;
            setTimeout(() => {
                this.cherryMarkDownEditorFlag = true;
            }, 500)
        },
        /** 修改按钮操作 */
        handleUpdate(row) {
            this.reset();
            this.cherryMarkDownEditorFlag = false;
            setTimeout(() => {
                this.cherryMarkDownEditorFlag = true;
            }, 500)
            const noticeId = row.noticeId || this.ids
            getNotice(noticeId).then(response => {
                this.form = response.data;
                this.open = true;
                this.title = "修改公告";
            });
        },
        /** 提交按钮 */
        submitForm: function () {
            this.form.noticeContent = this.$refs.CherryMarkdown.getData();
            this.$refs["form"].validate(valid => {
                if (valid) {
                    if (this.form.noticeId !== undefined) {
                        updateNotice(this.form).then(response => {
                            this.msgSuccess("修改成功");
                            this.open = false;
                            this.getList();
                        });
                    } else {
                        addNotice(this.form).then(response => {
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
            const noticeIds = row.noticeId || this.ids;
            this.$confirm('是否确认选中的通知公告?', "警告", {
                confirmButtonText: "确定",
                cancelButtonText: "取消",
                type: "warning"
            }).then(function () {
                return delNotice(noticeIds);
            }).then(() => {
                this.getList();
                this.msgSuccess("删除成功");
            })
        }
    }
};
</script>

<style lang="scss" scoped>
.app-container {
  padding: var(--space-4);
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

.notice-title {
  cursor: pointer;
  color: var(--text-primary);
  transition: color var(--duration-fast) var(--ease-default);
}

.notice-title:hover {
  color: var(--color-primary);
}

.theme-blue-text {
  color: var(--color-primary);
  cursor: pointer;
  transition: color var(--duration-fast) var(--ease-default);

  &:hover {
    color: var(--color-primary-600);
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
