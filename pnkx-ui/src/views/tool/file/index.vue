<template>
    <div class="app-container">
        <el-form :inline="true" :model="queryParams" label-width="68px" ref="queryForm" v-show="showSearch">
            <el-form-item label="上传端口" prop="port">
                <el-select placeholder="请选择上传端口" v-model="queryParams.port" @change="handleQuery">
                    <el-option
                        :key="item"
                        :label="item"
                        :value="item"
                        v-for="item in ['博客管理端', '博客客户端', '外部接口端']">
                    </el-option>
                </el-select>
            </el-form-item>
            <el-form-item label="文件名称" prop="name">
                <el-input
                    @keyup.enter.native="handleQuery"
                    clearable
                    placeholder="请输入文件名称"
                    size="small"
                    v-model="queryParams.name"
                />
            </el-form-item>
            <el-form-item label="分类" prop="type">
                <el-select v-model="queryParams.type" placeholder="分类" clearable @change="handleQuery">
                    <el-option
                        v-for="dict in typeOptions"
                        :key="dict.dictValue"
                        :label="dict.dictLabel"
                        :value="dict.dictValue"
                    />
                </el-select>
            </el-form-item>
            <el-form-item label="排序字段" prop="orderByColumn">
                <el-select placeholder="请选择排序字段" v-model="queryParams.orderByColumn" clearable @change="handleQuery">
                    <el-option
                        :key="item.value"
                        :label="item.label"
                        :value="item.value"
                        v-for="item in [{label: '创建时间', value: 'createTime'},{label: '点赞', value: 'thumb'},{label: '浏览', value: 'browse'}]">
                    </el-option>
                </el-select>
            </el-form-item>
            <el-form-item label="排序类型" prop="isAsc">
                <el-select placeholder="请选择排序类型" v-model="queryParams.isAsc" clearable @change="handleQuery">
                    <el-option
                        :key="item.value"
                        :label="item.label"
                        :value="item.value"
                        v-for="item in [{label: '升序', value: 'asc'},{label: '降序', value: 'desc'}]">
                    </el-option>
                </el-select>
            </el-form-item>
            <el-form-item>
                <el-button @click="handleQuery" icon="Search" size="small" type="primary">搜索</el-button>
                <el-button @click="resetQuery" icon="Refresh" size="small">重置</el-button>
            </el-form-item>
        </el-form>

        <el-row :gutter="10" class="mb8">
            <el-col :span="1.5">
                <el-button
                    icon="Plus"
                                        size="small"
                    type="primary"
                    @click="handleAdd"
                >新增
                </el-button>
            </el-col>
            <el-col :span="1.5">
                <el-button
                    :disabled="single"
                    icon="Edit"
                                        size="small"
                    type="success"
                    @click="handleUpdate"
                >修改
                </el-button>
            </el-col>
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
            <right-toolbar :showSearch="showSearch" @queryTable="getList"/>
        </el-row>

        <el-table :data="fileList" @selection-change="handleSelectionChange" v-loading="loading">
            <el-table-column align="center" type="selection" width="55"/>
            <el-table-column align="center" label="文件缩略图">
                <template v-slot="scope">
                    <el-image :preview-src-list="imageList"
                              v-if="scope.row.isPicture"
                              :src="scope.row.thumbnail ? scope.row.thumbnail : scope.row.url"
                              fit="scale-down"
                              style="width: 5rem; height: 5rem;">
                        <div slot="error" class="image-slot invalid-svg">
                            <svg-icon icon-class="已失效2"/>
                        </div>
                    </el-image>
                    <div v-else class="format">
                        {{ scope.row.name.slice(scope.row.name.lastIndexOf('.') + 1) }}
                    </div>
                </template>
            </el-table-column>
            <el-table-column align="center" label="文件名称" show-overflow-tooltip>
                <template v-slot="scope">
                    <span
                        @click="copyCode(scope.row.url)"
                        class="theme-blue-text">{{ scope.row.name }}</span>
                </template>
            </el-table-column>
            <el-table-column align="center" label="文件路径" prop="path" show-overflow-tooltip/>
            <el-table-column align="center" label="上传端口" prop="port"/>
            <el-table-column align="center" label="分类" prop="type" :formatter="typeFormat"/>
            <el-table-column align="center" label="上传时间" prop="createTime"/>
            <el-table-column align="center" label="浏览" prop="browse" width="50"/>
            <el-table-column align="center" label="点赞" prop="thumb" width="50"/>
            <el-table-column align="center" class-name="small-padding fixed-width" label="操作" width="100">
                <template v-slot="scope">
                    <el-button icon="Edit"
                               size="small"
                               type="text"
                               @click="handleDownload(scope.row)"
                    >下载
                    </el-button>
                    <el-button size="small"
                               type="text"
                               icon="Edit"
                               @click="handleUpdate(scope.row)"
                    >修改
                    </el-button>
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

        <!-- 修改文件记录对话框 -->
        <el-dialog :title="title" v-model="open" append-to-body width="500px">
            <el-form :model="form" :rules="rules" label-width="80px" ref="form">
                <el-form-item label="文件名称" prop="name">
                    <el-input placeholder="请输入文件名称" v-model="form.name"/>
                </el-form-item>
                <el-form-item label="文件路径" prop="path">
                    <el-input placeholder="请输入文件路径" v-model="form.path"/>
                </el-form-item>
                <el-form-item label="文件类型">
                    <el-select v-model="form.type" placeholder="分类">
                        <el-option
                            v-for="dict in typeOptions"
                            :key="dict.dictValue"
                            :label="dict.dictLabel"
                            :value="dict.dictValue"
                        />
                    </el-select>
                </el-form-item>
                <el-form-item label="浏览" prop="browse">
                    <el-input placeholder="请输入浏览数" type="number" v-model="form.browse"/>
                </el-form-item>
                <el-form-item label="点赞" prop="thumb">
                    <el-input placeholder="请输入点赞数" type="number" v-model="form.thumb"/>
                </el-form-item>
                <el-form-item label="版本号" prop="version">
                    <el-input placeholder="请输入版本号" v-model="form.version"/>
                </el-form-item>
                <el-form-item label="备注" prop="remark">
                    <el-input placeholder="请输入内容" type="textarea" v-model="form.remark"/>
                </el-form-item>
            </el-form>
            <div class="dialog-footer" slot="footer">
                <el-button @click="submitForm" type="primary">确 定</el-button>
                <el-button @click="cancel">取 消</el-button>
            </div>
        </el-dialog>

        <el-dialog
            v-model="addPicture"
            destroy-on-close
            title="提示"
            width="60%">
            <el-form label-width="8rem">
                <el-form-item label="文件类型">
                    <el-select v-model="uploadType" placeholder="文件类型">
                        <el-option
                            v-for="dict in typeOptions"
                            :key="dict.dictValue"
                            :label="dict.dictLabel"
                            :value="dict.dictValue"
                        />
                    </el-select>
                </el-form-item>
                <el-form-item label="缩略图">
                    <el-radio-group v-model="isThumbnail" size="small">
                        <el-radio-button :label="true">允许</el-radio-button>
                        <el-radio-button :label="false">禁止</el-radio-button>
                    </el-radio-group>
                </el-form-item>
                <el-form-item label="上传路径">
                    <el-input placeholder="请输入上传路径" v-model="uploadPath"/>
                </el-form-item>
                <upload :file-type="uploadType" :is-thumbnail="isThumbnail" :upload-path="uploadPath" @change="handleUploadChange"/>
                <div v-if="pictureList.length > 0" class="uploaded-files">
                    <p class="uploaded-title">已上传文件 ({{ pictureList.length }})</p>
                    <div v-for="(item, index) in pictureList" :key="index" class="uploaded-item">
                        <el-icon><Document /></el-icon>
                        <span class="uploaded-name" :title="item.name">{{ item.name }}</span>
                        <el-link :href="item.url" target="_blank" type="primary">查看</el-link>
                        <el-button link type="danger" size="small" @click="removeUploaded(index)">删除</el-button>
                    </div>
                </div>
            </el-form>
            <span slot="footer" class="dialog-footer">
                <el-button type="primary" @click="addPictureConFirm">确 定</el-button>
            </span>
        </el-dialog>
    </div>
</template>

<script>
import {listFile, getFile, delFile, addFile, updateFile, exportFile} from "@/api/tool/file";
import Upload from '@/components/Upload'

export default {
    name: "File",
    components: {
        Upload
    },
    data() {
        return {
            // 图片格式
            photoFormat: ["bmp", "gif", "jpg", "jpeg", "png", "webp"],
            // 新增图片地址
            pictureList: [],
            //图片列表
            imageList: '',
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
            // 文件记录表格数据
            fileList: [],
            // 弹出层标题
            title: "",
            // 是否显示弹出层
            open: false,
            // 上传图片
            addPicture: false,
            // 查询参数
            queryParams: {
                pageNum: 1,
                pageSize: 10,
                orderByColumn: 'createTime',
                isAsc: 'desc',
                type: '',
                port: null,
                name: null,
                path: null,
                version: null,
            },
            // 表单参数
            form: {},
            // 表单校验
            rules: {},
            // 类型列表
            typeOptions: [],
            // 上传类型
            uploadType: '',
            // 是否生成缩略图
            isThumbnail: true,
            // 指定上传路径
            uploadPath: ''
        };
    },
    created() {
        this.getList();
        this.getDicts("sys_file_type").then(response => {
            this.typeOptions = response.data;
        });
    },
    methods: {
        /**
         * 关闭弹框
         */
        addPictureConFirm() {
            this.addPicture = false;
            this.pictureList = [];
            this.resetQuery();
        },
        /**
         * 上传成功回调
         */
        handleUploadChange({ url, name }) {
            // 防止重复添加
            if (this.pictureList.some(item => item.url === url)) return
            this.pictureList.push({ url, name })
        },
        /**
         * 删除已上传文件
         */
        removeUploaded(index) {
            this.pictureList.splice(index, 1)
        },
        // 菜单状态字典翻译
        typeFormat(row, column) {
            if (!row.type) {
                return '暂未分类'
            }
            return this.selectDictLabel(this.typeOptions, row.type);
        },
        /**
         * 单击复制到粘贴板
         */
        copyCode(content) {
            this.$copyText(content).then(res => {
                    this.$notify.success("已成功复制，可直接去粘贴");
                },
                err => {
                    this.$notify.error("复制失败");
                })
        },
        /** 查询文件记录列表 */
        getList() {
            this.loading = true;
            listFile(this.queryParams).then(response => {
                this.fileList = response.rows;
                this.imageList = [];
                this.fileList.forEach(item => {
                    if (this.judgePicture(item.name)) {
                        item.isPicture = true;
                        this.imageList.push(item.url);
                    }
                })
                this.total = response.total;
                this.loading = false;
            });
        },
        /**
         * 判断是否是图片
         * @param name
         */
        judgePicture(name) {
            let result = false;
            this.photoFormat.forEach(item => {
                if (name.endsWith(item)) {
                    result = true
                }
            })
            return result
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
                path: null,
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
            this.single = selection.length !== 1
            this.multiple = !selection.length
        },
        /** 新增按钮操作 */
        handleAdd() {
            this.reset();
            this.pictureList = [];
            this.addPicture = true;
            this.title = "添加文件记录";
        },
        /** 修改按钮操作 */
        handleDownload(row) {
            window.open(row.url);
        },
        /** 修改按钮操作 */
        handleUpdate(row) {
            this.reset();
            const id = row.id || this.ids
            getFile(id).then(response => {
                this.form = response.data;
                this.open = true;
                this.title = "修改文件记录";
            });
        },
        /** 提交按钮 */
        submitForm() {
            this.$refs["form"].validate(valid => {
                if (valid) {
                    if (this.form.id != null) {
                        updateFile(this.form).then(response => {
                            this.msgSuccess("修改成功");
                            this.open = false;
                            this.getList();
                        });
                    } else {
                        addFile(this.form).then(response => {
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
            const ids = row.id || this.ids;
            this.$confirm('是否确认删除文件记录编号为"' + ids + '"的数据项?', "警告", {
                confirmButtonText: "确定",
                cancelButtonText: "取消",
                type: "warning"
            }).then(function () {
                return delFile(ids);
            }).then(() => {
                this.getList();
                this.msgSuccess("删除成功");
            })
        },
        /** 导出按钮操作 */
        handleExport() {
            const queryParams = this.queryParams;
            this.$confirm('是否确认导出所有文件记录数据项?', "警告", {
                confirmButtonText: "确定",
                cancelButtonText: "取消",
                type: "warning"
            }).then(function () {
                return exportFile(queryParams);
            }).then(response => {
                this.download(response.msg);
            })
        }
    }
};
</script>
<style lang="scss" scoped>
.format {
    background-color: var(--color-primary);
    padding: var(--space-4);
    color: #fff;
    border-radius: var(--radius-md);
    font-size: var(--text-xs);
    font-weight: var(--font-semibold);
    text-align: center;
}

.img-list {
    display: flex;
    flex-wrap: wrap;
    margin-top: var(--space-4);

    .image {
        width: 10rem;
        margin: 0 var(--space-4) var(--space-4) 0;

        .image-name {
            margin-top: var(--space-2);
            font-size: var(--text-sm);
            color: var(--text-secondary);
        }
    }
}

.theme-blue-text {
    color: var(--color-primary);
    cursor: pointer;
    transition: opacity var(--duration-fast) var(--ease-default);

    &:hover {
        opacity: 0.8;
    }
}

.invalid-svg {
    display: flex;
    align-items: center;
    justify-content: center;
    width: 100%;
    height: 100%;
}

.dialog-footer {
    display: flex;
    justify-content: flex-end;
    gap: var(--space-2);
}

.uploaded-files {
    margin-top: var(--space-4);
    padding: var(--space-4);
    background-color: var(--bg-secondary);
    border-radius: var(--radius-md);

    .uploaded-title {
        font-size: var(--text-sm);
        font-weight: var(--font-semibold);
        color: var(--text-primary);
        margin-bottom: var(--space-3);
    }

    .uploaded-item {
        display: flex;
        align-items: center;
        gap: var(--space-3);
        padding: var(--space-2) 0;
        border-bottom: 1px solid var(--border-color);

        &:last-child {
            border-bottom: none;
        }

        .uploaded-name {
            flex: 1;
            overflow: hidden;
            text-overflow: ellipsis;
            white-space: nowrap;
            font-size: var(--text-sm);
            color: var(--text-secondary);
        }
    }
}
</style>
