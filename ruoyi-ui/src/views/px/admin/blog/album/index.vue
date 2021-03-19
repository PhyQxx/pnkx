<template>
    <div class="app-container">
        <el-form :model="queryParams" ref="queryForm" :inline="true" v-show="showSearch" label-width="6rem">
            <el-form-item label="相册名称标签" prop="dictLabel">
                <el-input
                    v-model="queryParams.dictLabel"
                    placeholder="请输入相册名称标签"
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
                <el-button type="primary" icon="el-icon-search" size="mini" @click="handleQuery">搜索</el-button>
                <el-button icon="el-icon-refresh" size="mini" @click="resetQuery">重置</el-button>
            </el-form-item>
        </el-form>

        <el-row :gutter="10" class="mb8">
            <el-col :span="1.5">
                <el-button
                    type="primary"
                    plain
                    icon="el-icon-plus"
                    size="mini"
                    @click="handleAdd"
                    v-hasPermi="['system:dict:add']"
                >新增</el-button>
            </el-col>
            <el-col :span="1.5">
                <el-button
                    type="success"
                    plain
                    icon="el-icon-edit"
                    size="mini"
                    :disabled="single"
                    @click="handleUpdate"
                    v-hasPermi="['system:dict:edit']"
                >修改</el-button>
            </el-col>
            <el-col :span="1.5">
                <el-button
                    type="danger"
                    plain
                    icon="el-icon-delete"
                    size="mini"
                    :disabled="multiple"
                    @click="handleDelete"
                    v-hasPermi="['system:dict:remove']"
                >删除</el-button>
            </el-col>
            <el-col :span="1.5">
                <el-button
                    type="warning"
                    plain
                    icon="el-icon-download"
                    size="mini"
                    @click="handleExport"
                    v-hasPermi="['system:dict:export']"
                >导出</el-button>
            </el-col>
            <right-toolbar :showSearch.sync="showSearch" @queryTable="getList"/>
        </el-row>

        <el-table v-loading="loading" :data="dataList" @selection-change="handleSelectionChange">
            <el-table-column type="selection" width="55" align="center" />
            <el-table-column label="相册名称" align="center">
                <template slot-scope="scope">
                    <div class="album-name pointer" @click="goToPhoto(scope.row)">{{scope.row.dictLabel}}</div>
                </template>
            </el-table-column>
            <el-table-column label="相册名称键值" align="center" prop="dictValue" />
            <el-table-column label="状态" align="center" prop="status" :formatter="statusFormat" />
            <el-table-column label="备注" align="center" prop="remark" :show-overflow-tooltip="true" />
            <el-table-column label="创建时间" align="center" prop="createTime" width="180">
                <template slot-scope="scope">
                    <span>{{ parseTime(scope.row.createTime) }}</span>
                </template>
            </el-table-column>
            <el-table-column label="操作" align="center" class-name="small-padding fixed-width">
                <template slot-scope="scope">
                    <el-button
                        size="mini"
                        type="text"
                        icon="el-icon-edit"
                        @click="handleUpdate(scope.row)"
                        v-hasPermi="['system:dict:edit']"
                    >修改</el-button>
                    <el-button
                        size="mini"
                        type="text"
                        icon="el-icon-delete"
                        @click="handleDelete(scope.row)"
                        v-hasPermi="['system:dict:remove']"
                    >删除</el-button>
                </template>
            </el-table-column>
        </el-table>

        <pagination
            v-show="total>0"
            :total="total"
            :page.sync="queryParams.pageNum"
            :limit.sync="queryParams.pageSize"
            @pagination="getList"
        />

        <!-- 添加或修改参数配置对话框 -->
        <el-dialog :title="title" :visible.sync="open" width="500px" append-to-body>
            <el-form ref="form" :model="form" :rules="rules" label-width="7rem">
                <el-form-item label="相册名称">
                    <el-input v-model="form.dictType" :disabled="true" />
                </el-form-item>
                <el-form-item label="相册名称标签" prop="dictLabel">
                    <el-input v-model="form.dictLabel" placeholder="请输入数据标签" />
                </el-form-item>
                <el-form-item label="相册名称键值" prop="dictValue">
                    <el-input v-model="form.dictValue" placeholder="请输入数据键值" />
                </el-form-item>
                <el-form-item label="显示排序" prop="dictSort">
                    <el-input-number v-model="form.dictSort" controls-position="right" :min="0" />
                </el-form-item>
                <el-form-item label="状态" prop="status">
                    <el-radio-group v-model="form.status">
                        <el-radio
                            v-for="dict in statusOptions"
                            :key="dict.dictValue"
                            :label="dict.dictValue"
                        >{{dict.dictLabel}}</el-radio>
                    </el-radio-group>
                </el-form-item>
                <el-form-item label="备注" prop="remark">
                    <el-input v-model="form.remark" type="textarea" placeholder="请输入内容"/>
                </el-form-item>
            </el-form>
            <div slot="footer" class="dialog-footer">
                <el-button type="primary" @click="submitForm">确 定</el-button>
                <el-button @click="cancel">取 消</el-button>
            </div>
        </el-dialog>
    </div>
</template>

<script>
    import { dictDataCheckUniqueness, getDicts, getData, delData, addData, updateData, exportData } from "@/api/system/dict/data";
    import { listPhoto } from "@/api/px/admin/blog/photo.js";

    export default {
        name: "Adminalbum",
        data() {
            //相册名称名称校验规则
            const dictLabelValidate = (rule, value, callback) => {
                if (this.form.dictCode === undefined || this.form.dictCode === '') {
                    dictDataCheckUniqueness({
                        dictType: 'px_album_name',
                        dictLabel: value
                    }).then(res => {
                        if (res.data > 0) {
                            callback(new Error('相册名称名称不能重复'));
                        } else {
                            callback();
                        }
                    });
                } else {
                    callback();
                }
            };
            //相册名称键值校验规则
            const dictValueValidate = (rule, value, callback) => {
                    if (this.form.dictCode === undefined || this.form.dictCode === '') {
                        dictDataCheckUniqueness({
                            dictType: 'px_album_name',
                            dictValue: value
                        }).then(res => {
                            if (res.data > 0) {
                                callback(new Error('相册名称键值不能重复'));
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
                // 相册名称表格数据
                dataList: [],
                // 默认相册名称类型
                defaultDictType: "",
                // 弹出层标题
                title: "",
                // 是否显示弹出层
                open: false,
                // 状态数据相册名称
                statusOptions: [],
                // 类型数据相册名称
                typeOptions: [],
                // 查询参数
                queryParams: {
                    pageNum: 1,
                    pageSize: 10,
                    dictName: undefined,
                    dictType: 'px_album_name',
                    status: undefined
                },
                // 表单参数
                form: {},
                // 表单校验
                rules: {
                    dictLabel: [
                        { required: true, message: "相册名称名称不能为空", trigger: "blur" },
                        { validator: dictLabelValidate, trigger: 'blur' }
                    ],
                    dictValue: [
                        { required: true, message: "相册名称键值不能为空", trigger: "blur" },
                        { validator: dictValueValidate, trigger: 'blur' }
                    ],
                    dictSort: [
                        { required: true, message: "相册名称顺序不能为空", trigger: "blur" }
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
            /**
             * 跳转照片页面
             */
            goToPhoto(album) {
                this.$router.push({
                    path: 'adminphoto',
                    query: {
                        type: album.dictValue
                    }
                })
            },
            /** 查询相册名称数据列表 */
            getList() {
                this.loading = true;
                getDicts('px_album_name').then(response => {
                    this.dataList = response.data;
                    this.total = response.data.length;
                    this.loading = false;
                });
            },
            // 数据状态相册名称翻译
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
                this.queryParams.dictType = this.defaultDictType;
                this.handleQuery();
            },
            /** 新增按钮操作 */
            handleAdd() {
                this.reset();
                this.open = true;
                this.title = "添加相册名称数据";
                this.form.dictType = this.queryParams.dictType;
            },
            // 多选框选中数据
            handleSelectionChange(selection) {
                this.ids = selection.map(item => item.dictCode);
                this.single = selection.length!==1;
                this.multiple = !selection.length
            },
            /** 修改按钮操作 */
            handleUpdate(row) {
                this.reset();
                const dictCode = row.dictCode || this.ids;
                getData(dictCode).then(response => {
                    this.form = response.data;
                    this.open = true;
                    this.title = "修改相册名称数据";
                });
            },
            /** 提交按钮 */
            submitForm: function() {
                this.$refs["form"].validate(valid => {
                    if (valid) {
                        if (this.form.dictCode !== undefined) {
                            updateData(this.form).then(response => {
                                this.msgSuccess("修改成功");
                                this.open = false;
                                this.getList();
                            });
                        } else {
                            addData(this.form).then(response => {
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
                const dictCodes = row.dictCode || this.ids;
                this.$confirm('是否确认删除相册名称编码为"' + dictCodes + '"的数据项?', "警告", {
                    confirmButtonText: "确定",
                    cancelButtonText: "取消",
                    type: "warning"
                }).then(function() {
                    return delData(dictCodes);
                }).then(() => {
                    this.getList();
                    this.msgSuccess("删除成功");
                })
            },
            /** 导出按钮操作 */
            handleExport() {
                const queryParams = this.queryParams;
                this.$confirm('是否确认导出所有数据项?', "警告", {
                    confirmButtonText: "确定",
                    cancelButtonText: "取消",
                    type: "warning"
                }).then(function() {
                    return exportData(queryParams);
                }).then(response => {
                    this.download(response.msg);
                })
            }
        }
    };
</script>
<style lang="scss" scoped>
    .album-name:hover{
        color: #3399FF;
    }
</style>
