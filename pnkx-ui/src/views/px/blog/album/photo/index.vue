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
                        v-for="(dict, index) in typeOptions"
                        :key="dict.dictValue + index"
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
                >新增
                </el-button>
            </el-col>
            <right-toolbar :showSearch="showSearch" @queryTable="getList"/>
        </el-row>
        <el-table v-loading="loading" :data="photoList" @selection-change="handleSelectionChange">
            <el-table-column label="图片名称" align="center" prop="name"/>
            <el-table-column label="图片" align="center">
                <template #default="scope">
                    <div class="photo-column">
                    <cus-image
                        :is-admin="true"
                        :image-id="scope.row.id"
                        :remark="scope.row.remark"
                        class="photo-one pointer"
                        :src="scope.row.thumbnail || scope.row.photoBase64"
                        :preview-src-list="[scope.row.photoBase64]"
                        fit="scale-down">
                    </cus-image>
                    </div>
                </template>
            </el-table-column>
            <el-table-column label="所属相册" align="center" prop="type" :formatter="typeFormat"/>
            <el-table-column label="备注" align="center" prop="remark"/>
            <el-table-column label="操作" align="center" class-name="small-padding fixed-width">
                <template v-slot="scope">
                    <el-button
                        size="small"
                        type="text"
                        icon="Edit"
                        @click="handleUpdate(scope.row)"
                    >修改
                    </el-button>
                    <el-button
                        size="small"
                        type="text"
                        icon="Delete"
                        @click="handleDelete(scope.row)"
                    >删除
                    </el-button>
                </template>
            </el-table-column>
        </el-table>

        <pagination
            v-show="total>queryParams.pageSize"
            :total="total"
            v-model:page="queryParams.pageNum"
            v-model:limit="queryParams.pageSize"
            @pagination="getList"
        />
        <admin-message-board v-if="messageBoardStatus" :articleId="queryParams.type" messageType="2"/>
        <!-- 添加或修改相册对话框 -->
        <el-dialog :title="title" v-model="openDialog" width="500px" :close-on-click-modal="false" append-to-body>
            <div class="dialog" v-loading="dialogLoading">
                <el-form ref="form" :model="form" :rules="rules" label-width="8rem">
                    <el-form-item label="图片名称" prop="name">
                        <el-input v-model="form.name" placeholder="请输入图片名称"/>
                    </el-form-item>
                    <el-form-item label="图片预览" prop="photoBase64">
                        <div class="photo">
                            <el-switch
                                active-text="本地上传"
                                inactive-text="网络资源"
                                v-model="uploadPhotoFlag">
                            </el-switch>
                            <div @click="editCropper()" class="user-info" v-if="uploadPhotoFlag">
                                <div class="user-info-head">
                                    <el-image :src="options.img" fit="scale-down">
                                        <template #error>
                                            <div class="image-slot">
                                                请上传
                                            </div>
                                        </template>
                                    </el-image>
                                </div>
                            </div>
                            <div class="input-photo" v-if="!uploadPhotoFlag">
                                <el-image :preview-src-list="[form.photoBase64]" :src="form.photoBase64"
                                          fit="scale-down" style="width: 100%;">
                                    <template #error>
                                        <div class="image-slot invalid-svg">
                                            <svg-icon icon-class="已失效2"/>
                                        </div>
                                    </template>
                                </el-image>
                                <el-input placeholder="请输入图片URL" v-model="form.photoBase64"/>
                            </div>
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
                        <el-input v-model="form.remark" placeholder="请输入备注"/>
                    </el-form-item>
                </el-form>
            </div>
            <template #footer>
                <div class="dialog-footer">
                    <el-button type="primary" @click="submitForm">确 定</el-button>
                    <el-button @click="cancel">取 消</el-button>
                </div>
            </template>
        </el-dialog>
        <el-dialog v-model="open" @opened="modalOpened" append-to-body class="edit-picture" title="修改图片">
            <div class="edit-avatar">
                <el-row>
                    <el-col :md="24" :style="{height: '80vh'}" :xs="24" @click="editPicture">
                        <vue-cropper
                            ref="cropper"
                            :img="options.img"
                            :info="true"
                            :autoCrop="options.autoCrop"
                            :autoCropWidth="options.autoCropWidth"
                            :autoCropHeight="options.autoCropHeight"
                            :fixedBox="options.fixedBox"
                            @realTime="realTime"
                            v-if="visible"
                        />
                    </el-col>
                </el-row>
                <br/>
                <el-row>
                    <div class="button">
                        <el-upload action="#" :http-request="requestUpload" :show-file-list="false"
                                   :before-upload="beforeUpload">
                            <el-button size="small">
                                选择
                                <el-icon><Upload /></el-icon>
                            </el-button>
                        </el-upload>
                        <el-button @click="changeScale(1)" icon="Plus" size="small"/>
                        <el-button @click="changeScale(-1)" icon="Minus" size="small"/>
                        <el-button @click="rotateLeft()" icon="RefreshLeft" size="small"/>
                        <el-button @click="rotateRight()" icon="RefreshRight" size="small"/>
                        <el-button type="primary" size="small" @click="uploadImg()">提 交</el-button>
                    </div>
                </el-row>
            </div>
        </el-dialog>
    </div>
</template>

<script>
import {listPhoto, getPhoto, delPhoto, addPhoto, updatePhoto, exportPhoto} from "@/api/px/blog/photo";
import {VueCropper} from "vue-cropper";
import {uploadImage} from '@/api/system/image'
import cusImage from '@/components/CusImage/index.vue'

export default {
    name: "Adminphoto",
    components: {
        VueCropper,
        cusImage
    },
    data() {
        return {
            //上传图片方式
            uploadPhotoFlag: true,
            //留言板标志位
            messageBoardStatus: true,
            // 是否显示弹出层
            open: false,
            // 是否显示cropper
            visible: false,
            // 弹出层标题
            options: {
                img: '', //裁剪图片的地址
                autoCrop: false, // 是否默认生成截图框
                fixedBox: false // 固定截图框大小 不允许改变
            },
            previews: {},
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
            openDialog: false,
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
                name: null,
                type: this.$route.query.type,
                photoBase64: null,
                remark: ''
            },
            // 表单校验
            rules: {
                type: [
                    {required: true, message: "请选择所属相册", trigger: "change"}
                ],
                photoBase64: [
                    {required: true, message: "请上传图片", trigger: "change"}
                ],
            },
        };
    },
    mounted() {
        this.options.img = this.form.photoBase64;
    },
    created() {
        this.getList();
        this.getDicts("px_album_name").then(response => {
            this.typeOptions = response.data;
        });
    },
    methods: {
        /**
         * 打开编辑框
         */
        editPicture() {
            this.options.autoCrop = !this.options.autoCrop
        },
        // 编辑头像
        editCropper() {
            this.open = true;
        },
        // 打开弹出层结束时的回调
        modalOpened() {
            this.visible = true;
        },
        // 覆盖默认的上传行为
        requestUpload() {
        },
        // 向左旋转
        rotateLeft() {
            this.$refs.cropper.rotateLeft();
        },
        // 向右旋转
        rotateRight() {
            this.$refs.cropper.rotateRight();
        },
        // 图片缩放
        changeScale(num) {
            num = num || 1;
            this.$refs.cropper.changeScale(num);
        },
        // 上传预处理
        beforeUpload(file) {
            if (file.type.indexOf("image/") === -1) {
                this.msgError("文件格式错误，请上传图片类型,如：JPG，PNG后缀的文件。");
            } else {
                const reader = new FileReader();
                reader.readAsDataURL(file);
                reader.onload = () => {
                    this.options.img = reader.result;
                };
            }
        },
        // 上传图片
        uploadImg() {
            this.$refs.cropper.getCropBlob(data => {
                let formData = new FormData();
                formData.append('file', data, `${this.queryParams.type}-${this.form.name || this.parseTime(new Date, '{y}{m}{d}{h}{i}{s}')}.jpg`);
                formData.append('fileType', 'xczp');
                uploadImage(formData).then(res => {
                    if (res.code === 200) {
                        this.form.photoBase64 = res.url;
                        this.form.thumbnail = res.thumbnail;
                    } else {
                        this.$notify({
                            message: '上传失败',
                            type: 'error'
                        })
                    }
                    this.open = false;
                })
            })
        },
        // 实时预览
        realTime(data) {
            this.previews = data;
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
            this.options.img = '';
            this.form.photoBase64 = '';
            this.openDialog = false;
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
            this.messageBoardStatus = false;
            this.$nextTick(() => {
                this.messageBoardStatus = true;
            });
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
        /** 新增按钮操作 */
        handleAdd() {
            this.reset();
            this.openDialog = true;
            this.title = "添加照片";
            this.form.type = this.queryParams.type;
        },
        /** 修改按钮操作 */
        handleUpdate(row) {
            this.reset();
            const id = row.id || this.ids
            getPhoto(id).then(response => {
                this.form = response.data;
                this.openDialog = true;
                this.title = "修改照片";
            });
        },
        /** 提交按钮 */
        submitForm() {
            this.dialogLoading = true;
            this.$refs["form"].validate(valid => {
                if (valid) {
                    if (this.form.photoBase64) {
                        if (this.form.id != null) {
                            updatePhoto(this.form).then(response => {
                                this.msgSuccess("修改成功");
                                this.dialogLoading = false;
                                this.openDialog = false;
                                this.reset();
                                this.getList();
                            });
                        } else {
                            addPhoto(this.form).then(response => {
                                this.msgSuccess("新增成功");
                                this.dialogLoading = false;
                                this.openDialog = false;
                                this.reset();
                                this.getList();
                            });
                        }
                        this.options.img = '';
                    } else {
                        this.$notify.error('请上传图片');
                        this.dialogLoading = false;
                    }
                }
            });
        },
        /** 删除按钮操作 */
        handleDelete(row) {
            const ids = row.id || this.ids;
            this.$confirm('是否确认删除图片?', "警告", {
                confirmButtonText: "确定",
                cancelButtonText: "取消",
                type: "warning"
            }).then(function () {
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
            }).then(function () {
                return exportPhoto(queryParams);
            }).then(response => {
                this.download(response.msg);
            })
        }
    }
};
</script>

<style lang="scss" scoped>
.button {
    display: flex;
    flex-flow: wrap;
    align-items: center;
    justify-content: flex-start;

    .el-button {
        margin-right: 2rem;
    }
}

.user-info {
    width: 6rem;
    min-height: 6rem;
    border: 1px solid var(--border-primary);
    border-radius: var(--radius-md);

    .user-info-head {
        width: 6rem;
        height: 6rem;
        display: flex;
        align-items: center;
        justify-content: center;
    }
}

.edit-picture {
    :deep(.el-dialog) {
        width: 100% !important;
        height: 100%;
        margin: 0 !important;
        background-color: var(--bg-overlay);
    }
}

.photo-one {
    width: 6rem;
    height: 6rem;

    img {
        width: 100%;
        height: 100%;
    }
}

.photo-column {
    display: flex;
    justify-content: center;
    align-items: center;
}

.dialog-footer {
    display: flex;
    justify-content: flex-end;
}
</style>
