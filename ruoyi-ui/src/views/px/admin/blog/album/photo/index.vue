<template>
    <div class="app-container">
        <el-form :model="queryParams" ref="queryForm" :inline="true" v-show="showSearch" label-width="68px">
            <el-form-item label="图片名称" prop="name">
                <el-input
                    v-model="queryParams.name"
                    placeholder="请输入图片名称"
                    clearable
                    size="small"
                    @keyup.enter.native="handleQuery"
                />
            </el-form-item>
            <el-form-item label="所属相册" prop="type">
                <el-select v-model="queryParams.type" placeholder="请选择所属相册" clearable size="small">
                    <el-option
                        v-for="dict in typeOptions"
                        :key="dict.dictValue"
                        :label="dict.dictLabel"
                        :value="dict.dictValue"
                    />
                </el-select>
            </el-form-item>
            <el-form-item label="备注" prop="remark">
                <el-input
                    v-model="queryParams.remark"
                    placeholder="请输入备注"
                    clearable
                    size="small"
                    @keyup.enter.native="handleQuery"
                />
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
                    v-hasPermi="['system:photo:add']"
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
                    v-hasPermi="['system:photo:edit']"
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
                    v-hasPermi="['system:photo:remove']"
                >删除</el-button>
            </el-col>
            <right-toolbar :showSearch.sync="showSearch" @queryTable="getList"/>
        </el-row>
        <no-data text="暂无留言" v-if="photoList.length === 0"/>
        <el-table v-loading="loading" :data="photoList" @selection-change="handleSelectionChange">
            <el-table-column type="selection" width="55" align="center" />
            <el-table-column label="图片名称" align="center" prop="name" />
            <el-table-column label="图片" align="center">
                <template slot-scope="scope" class="photo-column">
                    <el-image
                        class="photo-one pointer"
                        :src="scope.row.photoBase64"
                        :preview-src-list="[scope.row.photoBase64]"
                        fit="scale-down">
                    </el-image>
                </template>
            </el-table-column>
            <el-table-column label="所属相册" align="center" prop="type" :formatter="typeFormat" />
            <el-table-column label="备注" align="center" prop="remark" />
            <el-table-column label="操作" align="center" class-name="small-padding fixed-width">
                <template slot-scope="scope">
                    <el-button
                        size="mini"
                        type="text"
                        icon="el-icon-edit"
                        @click="handleUpdate(scope.row)"
                        v-hasPermi="['system:photo:edit']"
                    >修改</el-button>
                    <el-button
                        size="mini"
                        type="text"
                        icon="el-icon-delete"
                        @click="handleDelete(scope.row)"
                        v-hasPermi="['system:photo:remove']"
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

        <!-- 添加或修改相册对话框 -->
        <el-dialog :title="title" :visible.sync="open" width="500px" append-to-body>
            <div class="dialog" v-loading="dialogLoading">
                <el-form ref="form" :model="form" :rules="rules" label-width="8rem">
                    <el-form-item label="图片名称" prop="name">
                        <el-input v-model="form.name" placeholder="请输入图片名称" />
                    </el-form-item>
                    <el-form-item label="图片Base64码" prop="photoBase64">
                        <div class="photo">
                            <el-image
                                v-if="form.photoBase64"
                                class="header-picture"
                                :src="form.photoBase64"
                                fit="scale-down">
                            </el-image>
                            <i @click="deleteHeader" class="el-icon-circle-close close-icon" v-if="form.photoBase64"/>
                            <input v-if="!form.photoBase64" type="file" id="headerPhoto" capture="camera" accept="image/*" @change="uploadHeader($event)"/>
                        </div>
                    </el-form-item>
                    <el-form-item label="所属相册" prop="type">
                        <el-select v-model="form.type" placeholder="请选择所属相册">
                            <el-option
                                v-for="dict in typeOptions"
                                :key="dict.dictValue"
                                :label="dict.dictLabel"
                                :value="dict.dictValue"
                            />
                        </el-select>
                    </el-form-item>
                    <el-form-item label="备注" prop="remark">
                        <el-input v-model="form.remark" placeholder="请输入备注" />
                    </el-form-item>
                </el-form>
                <div slot="footer" class="dialog-footer">
                    <el-button type="primary" @click="submitForm">确 定</el-button>
                    <el-button @click="cancel">取 消</el-button>
                </div>
            </div>
        </el-dialog>
    </div>
</template>

<script>
    import { listPhoto, getPhoto, delPhoto, addPhoto, updatePhoto, exportPhoto } from "@/api/px/admin/blog/photo";
    import { compressImage } from '@/utils/compressImage'

    export default {
        name: "Photo",
        components: {
        },
        data() {
            return {
                // 遮罩层
                loading: true,
                dialogLoading: false,
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
                // 相册表格数据
                photoList: [],
                // 弹出层标题
                title: "",
                // 是否显示弹出层
                open: false,
                // 所属相册字典
                typeOptions: [],
                // 查询参数
                queryParams: {
                    pageNum: 1,
                    pageSize: 10,
                    name: null,
                    photoAddress: null,
                    type: this.$route.query.type,
                    remark: null
                },
                // 表单参数
                form: {
                    type: this.$route.query.type,
                },
                // 表单校验
                rules: {
                    type: [
                        {required: true, message: "请选择所属相册", trigger: "change"}
                    ]
                },
                config: {
                    width: 100, // 压缩后图片的宽
                    height: 100, // 压缩后图片的高
                    quality: 0.8 // 压缩后图片的清晰度，取值0-1，值越小，所绘制出的图像越模糊
                }
            };
        },
        created() {
            this.getList();
            this.getDicts("px_album_name").then(response => {
                this.typeOptions = response.data;
            });
        },
        methods: {
            blobToDataURL(blob,cb) {
                let reader = new FileReader();
                reader.onload = function (evt) {
                    let base64 = evt.target.result;
                    cb(base64)
                };
                reader.readAsDataURL(blob);
            },
            /**
             * 上传头像
             */
            uploadHeader(e) {
                if(e.target.files[0]){
                    compressImage(e.target.files[0], this.config)
                        .then(result => { // result 为压缩后二进制文件
                            this.blobToDataURL(result, (base64Url) => {
                                this.form.photoBase64 = base64Url;
                            })
                        });
                }
            },
            /**
             * 删除头像
             */
            deleteHeader() {
                this.messageForm.authorHeader = '';
            },
            /** 查询相册列表 */
            getList() {
                this.loading = true;
                listPhoto(this.queryParams).then(response => {
                    this.photoList = response.rows;
                    this.total = response.total;
                    this.loading = false;
                });
            },
            // 所属相册字典翻译
            typeFormat(row, column) {
                return this.selectDictLabel(this.typeOptions, row.type);
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
                    name: null,
                    photoBase64: null,
                    photoAddress: null,
                    type: null,
                    version: null,
                    createBy: null,
                    createTime: null,
                    updateBy: null,
                    updateTime: null,
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
                this.single = selection.length!==1
                this.multiple = !selection.length
            },
            /** 新增按钮操作 */
            handleAdd() {
                this.reset();
                this.open = true;
                this.title = "添加照片";
                this.form.type =  this.queryParams.type;
            },
            /** 修改按钮操作 */
            handleUpdate(row) {
                this.reset();
                const id = row.id || this.ids
                getPhoto(id).then(response => {
                    this.form = response.data;
                    this.open = true;
                    this.title = "修改照片";
                });
            },
            /** 提交按钮 */
            submitForm() {
                this.dialogLoading = true;
                this.$refs["form"].validate(valid => {
                    if (valid) {
                        if (this.form.id != null) {
                            updatePhoto(this.form).then(response => {
                                this.msgSuccess("修改成功");
                                this.dialogLoading = false;
                                this.open = false;
                                this.getList();
                            });
                        } else {
                            addPhoto(this.form).then(response => {
                                this.msgSuccess("新增成功");
                                this.dialogLoading = false;
                                this.open = false;
                                this.getList();
                            });
                        }
                    }
                });
            },
            /** 删除按钮操作 */
            handleDelete(row) {
                const ids = row.id || this.ids;
                this.$confirm('是否确认删除相册编号为"' + ids + '"的数据项?', "警告", {
                    confirmButtonText: "确定",
                    cancelButtonText: "取消",
                    type: "warning"
                }).then(function() {
                    return delPhoto(ids);
                }).then(() => {
                    this.getList();
                    this.msgSuccess("删除成功");
                })
            },
            /** 导出按钮操作 */
            handleExport() {
                const queryParams = this.queryParams;
                this.$confirm('是否确认导出所有相册数据项?', "警告", {
                    confirmButtonText: "确定",
                    cancelButtonText: "取消",
                    type: "warning"
                }).then(function() {
                    return exportPhoto(queryParams);
                }).then(response => {
                    this.download(response.msg);
                })
            }
        }
    };
</script>

<style lang="scss" scoped>
    .photo-one{
        width: 6rem;
        height: 6rem;
        img{
            width: 100%;
            height: 100%;
        }
    }
    .photo-column{
        display: flex;
        justify-content: center;
        align-items: center;
    }
    .photo{
        width: 10rem;
        height: 10rem;
        border: 1px solid #999;
        border-radius: 5px;
        display: flex;
        flex-flow: column;
        align-items: center;
        justify-content: center;
        .close-icon{
            position: absolute;
            margin: -8rem -8rem 0 0;
            color: red;
            font-size: 1.5rem;
        }
        #headerPhoto{
            width: 4.5rem;
            position: absolute;
        }
        .header-picture{
            width: 100%;
            height: 100%;
            padding: 0.5rem;
            display: flex;
            justify-content: center;
            white-space: nowrap;
            img{
                width: 100%;
                height: 100%;
            }
        }
    }
    .dialog-footer{
        display: flex;
        justify-content: flex-end;
    }
</style>
