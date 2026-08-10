<template>
    <div class="app-container">
        <el-form :model="queryParams" ref="queryForm" :inline="true" v-show="showSearch" label-width="68px">
            <el-form-item label="友链标题" prop="title">
                <el-input
                    v-model="queryParams.title"
                    placeholder="请输入友链标题"
                    clearable
                    size="small"
                    @keyup.enter.native="handleQuery"
                />
            </el-form-item>
            <el-form-item label="友链状态" prop="status">
                <el-select v-model="queryParams.status" clearable placeholder="友链状态" size="small">
                    <el-option
                        v-for="dict in statusOptions"
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
                >新增
                </el-button>
            </el-col>
            <el-col :span="1.5">
                <el-button
                    type="success"
                    icon="ChatDotRound"
                    size="small"
                    @click="handleMessageManage"
                >留言管理
                </el-button>
            </el-col>
            <right-toolbar :showSearch="showSearch" @queryTable="getList"/>
        </el-row>

        <div class="table-main-area">
            <el-table v-loading="loading" :data="linkList" @selection-change="handleSelectionChange">
                <el-table-column label="序号" align="center">
                    <template v-slot="scope">
                        <span>{{ scope.$index + 1 + (queryParams.pageNum - 1) * queryParams.pageSize }}</span>
                    </template>
                </el-table-column>
                <el-table-column label="友链图片" align="center">
                    <template v-slot="scope" class="photo-column">
                        <el-image
                            class="photo-one pointer"
                            :src="scope.row.img"
                            :preview-src-list="[scope.row.img]"
                            fit="scale-down">
                            <template #error>
                                <div class="image-slot invalid-svg">
                                    <svg-icon icon-class="已失效2"/>
                                </div>
                            </template>
                        </el-image>
                    </template>
                </el-table-column>
                <el-table-column label="友链标题" align="center" prop="title"/>
                <el-table-column label="友链url" align="center">
                    <template v-slot="scope">
                        <a :href="scope.row.url" style="color: #5A8DEE" target="_blank">{{ scope.row.url }}</a>
                    </template>
                </el-table-column>
                <el-table-column label="描述" align="center" prop="remark"/>
                <el-table-column :formatter="statusFormat" align="center" label="状态" prop="status"/>
                <el-table-column align="center" label="申请时间" prop="createTime"/>
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
        </div>

        <pagination
            v-show="total>queryParams.pageSize"
            :total="total"
            v-model:page="queryParams.pageNum"
            v-model:limit="queryParams.pageSize"
            @pagination="getList"
        />

        <!-- 添加或修改友链对话框 -->
        <el-dialog :title="title" v-model="editDialog" width="500px" append-to-body>
            <el-form ref="form" :model="form" :rules="rules" label-width="80px">
                <el-form-item label="博客logo" prop="photoBase64">
                    <div class="one-info">
                        <div class="content" v-if="!form.img">
                            <el-switch
                                @change="changeLogoFlag"
                                active-text="网络资源"
                                inactive-text="本地上传"
                                v-model="logoFlag">
                            </el-switch>
                            <el-input placeholder="请输入博客logo" v-if="logoFlag" v-model="form.img"/>
                            <el-button @click="editCropper" v-if="!logoFlag">上传</el-button>
                        </div>
                        <div class="img" v-if="form.img">
                            <el-image :src="form.img" fit="scale-down">
                                <template #error>
                                    <div class="image-slot invalid-svg">
                                        <svg-icon icon-class="已失效2"/>
                                    </div>
                                </template>
                            </el-image>
                        </div>
                        <el-button @click="form.img=''" type="text" v-if="form.img">删除</el-button>
                    </div>
                </el-form-item>
                <el-form-item label="友链标题" prop="title">
                    <el-input v-model="form.title" placeholder="请输入友链标题"/>
                </el-form-item>
                <el-form-item label="友链url" prop="url">
                    <el-input v-model="form.url" placeholder="请输入友链url"/>
                </el-form-item>
                <el-form-item label="友链状态" prop="status">
                    <el-radio-group v-model="form.status">
                        <el-radio
                            v-for="dict in statusOptions"
                            :key="dict.dictValue"
                            :label="dict.dictValue"
                        >{{ dict.dictLabel }}
                        </el-radio>
                    </el-radio-group>
                </el-form-item>
                <el-form-item label="描述" prop="remark">
                    <el-input v-model="form.remark" type="textarea" placeholder="请输入内容"/>
                </el-form-item>
            </el-form>
            <template #footer>
                <div class="dialog-footer">
                    <el-button type="primary" @click="submitForm">确 定</el-button>
                    <el-button @click="cancel">取 消</el-button>
                </div>
            </template>
        </el-dialog>
        <el-dialog title="修改图片" v-model="open" width="800px" append-to-body @opened="modalOpened">
            <div class="edit-avatar">
                <el-row>
                    <el-col :xs="24" :md="12" :style="{height: '350px'}">
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
                    <el-col :xs="24" :md="12" :style="{height: '350px'}">
                        <div class="avatar-upload-preview">
                            <img :src="previews.url" :style="previews.img"/>
                        </div>
                    </el-col>
                </el-row>
                <br/>
                <el-row>
                    <el-col :lg="2" :md="2">
                        <el-upload action="#" :http-request="requestUpload" :show-file-list="false"
                                   :before-upload="beforeUpload">
                            <el-button size="small">
                                选择
                                <el-icon><Upload /></el-icon>
                            </el-button>
                        </el-upload>
                    </el-col>
                    <el-col :lg="{span: 1, offset: 2}" :md="2">
                        <el-button icon="Plus" size="small" @click="changeScale(1)"></el-button>
                    </el-col>
                    <el-col :lg="{span: 1, offset: 1}" :md="2">
                        <el-button icon="Minus" size="small" @click="changeScale(-1)"></el-button>
                    </el-col>
                    <el-col :lg="{span: 1, offset: 1}" :md="2">
                        <el-button icon="RefreshLeft" size="small" @click="rotateLeft()"></el-button>
                    </el-col>
                    <el-col :lg="{span: 1, offset: 1}" :md="2">
                        <el-button icon="RefreshRight" size="small" @click="rotateRight()"></el-button>
                    </el-col>
                    <el-col :lg="{span: 2, offset: 6}" :md="2">
                        <el-button type="primary" size="small" @click="uploadImg()">提 交</el-button>
                    </el-col>
                </el-row>
            </div>
        </el-dialog>

    </div>
</template>

<script>
import {listLink, getLink, delLink, addLink, updateLink, exportLink} from "@/api/px/blog/link";
import {VueCropper} from "vue-cropper";

export default {
    name: "Link",
    components: {
        VueCropper,
    },
    data() {
        return {
            // 配置
            options: {
                img: '', //裁剪图片的地址
                autoCrop: true, // 是否默认生成截图框
                autoCropWidth: 200, // 默认生成截图框宽度
                autoCropHeight: 200, // 默认生成截图框高度
                fixedBox: false // 固定截图框大小 不允许改变
            },
            previews: {},
            // 是否显示cropper
            visible: false,
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
            // 友链表格数据
            linkList: [],
            // 弹出层标题
            title: "",
            // 是否显示弹出层
            editDialog: false,
            open: false,
            // 查询参数
            queryParams: {
                pageNum: 1,
                pageSize: 10,
                img: null,
                title: null,
                url: null,
                version: null,
            },
            //logo输入标志
            logoFlag: true,
            // 表单参数
            form: {},
            // 表单校验
            rules: {},
            // 友链状态数据字典
            statusOptions: []
        };
    },
    created() {
        this.getDicts("bolg_friend_statue").then(response => {
            this.statusOptions = response.data;
        });
        this.getList();
    },
    mounted() {
        // 获取友链详情
        const id = this.$route.params.id;
        if (id) {
            this.handleUpdate({id})
        }
    },
    methods: {
        // 字典状态字典翻译
        statusFormat(row, column) {
            return this.selectDictLabel(this.statusOptions, row.status);
        },
        /**
         * logo的上传方式改变
         */
        changeLogoFlag() {
            this.apply.logo = ''
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
            if (file.type.indexOf("image/") == -1) {
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
                const reader = new FileReader();
                reader.onload = (e) => {
                    this.form.img = e.target.result;
                    this.open = false;
                };
                reader.readAsDataURL(data);
            })
        },
        // 实时预览
        realTime(data) {
            this.previews = data;
        },
        // 打开弹出层结束时的回调
        modalOpened() {
            this.visible = true;
        },
        // 编辑头像
        editCropper() {
            this.open = true;
        },
        /** 查询友链列表 */
        getList() {
            this.loading = true;
            listLink(this.queryParams).then(response => {
                const rows = Array.isArray(response.rows) ? response.rows : response.rows?.records || response.data?.records || [];
                this.linkList = rows;
                this.total = response.total || response.rows?.total || response.data?.total || rows.length;
                this.loading = false;
            }).catch(() => {
                this.linkList = [];
                this.total = 0;
                this.loading = false;
            });
        },
        // 取消按钮
        cancel() {
            this.editDialog = false;
            this.reset();
        },
        // 表单重置
        reset() {
            this.form = {
                id: null,
                img: null,
                title: null,
                url: null,
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
            this.single = selection.length !== 1
            this.multiple = !selection.length
        },
        /** 新增按钮操作 */
        handleAdd() {
            this.reset();
            this.editDialog = true;
            this.title = "添加友链";
        },
        /** 留言管理 */
        handleMessageManage() {
            this.$router.push('/blog/friendlink/message');
        },
        /** 修改按钮操作 */
        handleUpdate(row) {
            this.reset();
            const id = row.id || this.ids
            getLink(id).then(response => {
                this.form = response.data;
                this.editDialog = true;
                this.title = "修改友链";
            });
        },
        /** 提交按钮 */
        submitForm() {
            this.$refs["form"].validate(valid => {
                if (valid) {
                    if (this.form.id != null) {
                        updateLink(this.form).then(response => {
                            this.msgSuccess("修改成功");
                            this.editDialog = false;
                            this.getList();
                        });
                    } else {
                        addLink(this.form).then(response => {
                            this.msgSuccess("新增成功");
                            this.editDialog = false;
                            this.getList();
                        });
                    }
                }
            });
        },
        /** 删除按钮操作 */
        handleDelete(row) {
            const ids = row.id || this.ids;
            this.$confirm('是否确认删除友链编号为"' + ids + '"的数据项?', "警告", {
                confirmButtonText: "确定",
                cancelButtonText: "取消",
                type: "warning"
            }).then(function () {
                return delLink(ids);
            }).then(() => {
                this.getList();
                this.msgSuccess("删除成功");
            })
        },
        /** 导出按钮操作 */
        handleExport() {
            const queryParams = this.queryParams;
            this.$confirm('是否确认导出所有友链数据项?', "警告", {
                confirmButtonText: "确定",
                cancelButtonText: "取消",
                type: "warning"
            }).then(function () {
                return exportLink(queryParams);
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
    padding: var(--space-6);
    background: var(--bg-body);
}

.photo-one {
    width: 6rem;
    height: 6rem;
    border-radius: var(--radius-lg);
    overflow: hidden;
    transition: transform var(--duration-fast) var(--ease-default),
                box-shadow var(--duration-fast) var(--ease-default);

    &:hover {
        transform: scale(1.05);
        box-shadow: var(--shadow-md);
    }

    img {
        width: 100%;
        height: 100%;
    }
}

.img {
    width: 6rem;
    height: 6rem;
    border: 1px solid var(--border-primary);
    border-radius: var(--radius-md);
    padding: var(--space-2);
    margin-right: var(--space-4);
    transition: border-color var(--duration-fast) var(--ease-default),
                box-shadow var(--duration-fast) var(--ease-default);

    &:hover {
        border-color: var(--color-primary);
        box-shadow: var(--shadow-sm);
    }
}

::v-deep a[href] {
    color: var(--text-link);
    transition: color var(--duration-fast) var(--ease-default);

    &:hover {
        color: var(--text-link-hover);
    }
}
</style>
