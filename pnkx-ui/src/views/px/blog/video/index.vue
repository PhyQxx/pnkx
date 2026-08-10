<template>
    <div class="app-container">
        <el-form :model="queryParams" ref="queryForm" :inline="true" v-show="showSearch" label-width="68px">
            <el-form-item label="标题" prop="title">
                <el-input
                    v-model="queryParams.title"
                    placeholder="请输入标题"
                    clearable
                    size="small"
                    @keyup.enter.native="handleQuery"
                />
            </el-form-item>
            <el-form-item label="分类" prop="type">
                <el-select v-model="queryParams.type" placeholder="请选择分类" clearable size="small">
                    <el-option
                        v-for="dict in typeOptions"
                        :key="dict.dictValue"
                        :label="dict.dictLabel"
                        :value="dict.dictValue"
                    />
                </el-select>
            </el-form-item>
            <el-form-item label="标签" prop="label">
                <el-select v-model="queryParams.label" placeholder="请选择标签" clearable size="small">
                    <el-option
                        v-for="dict in labelOptions"
                        :key="dict"
                        :label="dict"
                        :value="dict"
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
                    v-hasPermi="['px:video:add']"
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
                    v-hasPermi="['px:video:edit']"
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
                    v-hasPermi="['px:video:remove']"
                >删除
                </el-button>
            </el-col>
            <el-col :span="1.5">
                <el-button
                    type="warning"
                                        icon="Download"
                    size="small"
                    @click="handleExport"
                    v-hasPermi="['px:video:export']"
                >导出
                </el-button>
            </el-col>
            <el-col :span="1.5">
                <el-button
                    type="success"
                    icon="ChatDotRound"
                    size="small"
                    @click="handleMessageManage"
                >评论管理
                </el-button>
            </el-col>
            <right-toolbar :showSearch="showSearch" @queryTable="getList"></right-toolbar>
        </el-row>

        <div class="table-main-area">
            <el-table v-loading="loading" :data="videoList" @selection-change="handleSelectionChange">
                <el-table-column type="selection" width="55" align="center"/>
                <el-table-column label="标题" align="center" prop="title"/>
                <el-table-column label="封面" align="center" prop="cover">
                    <template v-slot="scope">
                        <el-image :preview-src-list="[scope.row.cover]"
                                  :src="scope.row.cover"
                                  fit="scale-down"
                                  style="width: 5rem; height: 5rem;">
                            <template #error>
                                <div class="image-slot invalid-svg">
                                    <svg-icon icon-class="已失效2"/>
                                </div>
                            </template>
                        </el-image>
                    </template>
                </el-table-column>
                <el-table-column label="视频地址" align="center" prop="url">
                    <template v-slot="scope">
                        <span @click="handlePreviewVideo(scope.row)" class="theme-blue-text">{{ scope.row.url }}</span>
                    </template>
                </el-table-column>
                <el-table-column label="标签" align="center" prop="label"/>
                <el-table-column label="备注" align="center" prop="remark"/>
                <el-table-column label="操作" align="center" class-name="small-padding fixed-width">
                    <template v-slot="scope">
                        <el-button
                            size="small"
                            type="text"
                            icon="Share"
                            @click="$copyText('https://pnkx.top/videos/' + scope.row.id)"
                        >分享
                        </el-button>
                        <el-button
                            size="small"
                            type="text"
                            icon="Edit"
                            @click="handleUpdate(scope.row)"
                            v-hasPermi="['px:video:edit']"
                        >修改
                        </el-button>
                        <el-button
                            size="small"
                            type="text"
                            icon="Delete"
                            @click="handleDelete(scope.row)"
                            v-hasPermi="['px:video:remove']"
                        >删除
                        </el-button>
                    </template>
                </el-table-column>
            </el-table>
        </div>

        <pagination
            v-if="total>0"
            :total="total"
            v-model:page="queryParams.pageNum"
            v-model:limit="queryParams.pageSize"
            @pagination="getList"
        />

        <!-- 添加或修改视频模块对话框 -->
        <el-dialog :title="title"
                   v-model="open"
                   width="80vw"
                   destroy-on-close
                   append-to-body>
            <el-form ref="form" :model="form" :rules="rules" label-width="80px">
                <el-form-item label="标题" prop="title">
                    <el-input v-model="form.title" placeholder="请输入标题"/>
                </el-form-item>
                <el-form-item label="封面" prop="cover">
                    <imageUpload v-model="form.cover" image-type="spfm"/>
                </el-form-item>
                <el-form-item label="视频" prop="url">
                    <video-preview :url="form.url" v-if="form.url"/>
                    <upload :type="['mp4']" @change="getFileInfo" v-else/>
                </el-form-item>
                <el-form-item label="分类" prop="type">
                    <el-select v-model="form.type" placeholder="请选择分类">
                        <el-option
                            v-for="dict in typeOptions"
                            :key="dict.dictValue"
                            :label="dict.dictLabel"
                            :value="dict.dictValue"
                        ></el-option>
                    </el-select>
                </el-form-item>
                <el-form-item label="标签">
                    <el-tag
                        :key="tag"
                        style="margin-right: 1rem;"
                        :type="['', 'success', 'info', 'danger', 'warning'][Math.floor(Math.random()*6)]"
                        v-for="(tag, index) in form.label"
                        closable
                        :disable-transitions="false"
                        @close="handleDeleteLabel(tag)">
                        {{ tag }}
                    </el-tag>
                    <el-select v-model="newLabel"
                               @change="handleChangeLabel"
                               filterable
                               allow-create
                               placeholder="请选择标签">
                        <el-option
                            v-for="item in labelOptions"
                            :key="item"
                            :label="item"
                            :value="item">
                        </el-option>
                    </el-select>
                </el-form-item>
                <el-form-item label="备注" prop="remark">
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

        <!-- 预览视频 -->
        <el-dialog title="预览视频"
                   top="1vh"
                   v-model="preview"
                   width="80vw"
                   destroy-on-close
                   append-to-body>
            <video-preview @play="handlePlay" :url="previewUrl"/>
            <v-barrage v-if="play" class="barrage-model" :barrageList="barrageList"/>
            <div class="barrage">
                <div class="barrage-content">
                    <div class="name">
                        <el-input v-model="barrageForm.content"
                                  @keyup.enter.native="handleSendBarrage"
                                  placeholder="请输入弹幕内容">
                            <template #append>
                                <el-button @click="handleSendBarrage">发送</el-button>
                            </template>
                        </el-input>
                    </div>
                </div>
            </div>
        </el-dialog>
    </div>
</template>

<script>
import {
    listVideo,
    getVideo,
    delVideo,
    addVideo,
    updateVideo,
    exportVideo,
    getLabelList
} from "@/api/px/blog/video";
import {addMessage, getMessageList} from '@/api/px/blog/message';
import ImageUpload from '@/components/ImageUpload/index.vue';
import Upload from '@/components/Upload/index.vue'
import VideoPreview from '@/components/Video/index.vue'
import VBarrage from '@/components/Vbarrage/index.vue'
import {BLOG_URL} from "@/assets/js/common";

export default {
    name: "Video",
    components: {
        ImageUpload,
        VideoPreview,
        Upload,
        VBarrage
    },
    data() {
        return {
            // 新增标签
            newLabel: '',
            // 待选择标签
            labelOptions: [],
            // 上传文件类型
            fileType: ["swf", "flv", "mp3", "wav", "wma", "wmv", "mid", "avi", "mpg", "asf", "rm", "rmvb", "mp4"],
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
            // 视频模块表格数据
            videoList: [],
            // 分类字典
            typeOptions: [],
            // 弹出层标题
            title: "",
            // 是否显示弹出层
            open: false,
            // 是否显示预览弹出层
            preview: false,
            // 预览视频的url
            previewUrl: '',
            // 查询参数
            queryParams: {
                pageNum: 1,
                pageSize: 10,
                title: null,
                label: null,
            },
            // 表单参数
            form: {},
            // 表单校验
            rules: {
                title: [
                    {required: true, message: "标题不能为空", trigger: "blur"}
                ],
                url: [
                    {required: true, message: "请上传视频", trigger: "blur"}
                ],
            },
            // 播放标志
            play: false,
            // 弹幕列表
            barrageList: [],
            // 弹幕表单
            barrageForm: {
                //留言内容
                content: '',
                //游客姓名
                authorName: '',
                //头像的URL
                authorHeader: ``,
                messageBoard: '8',
                state: '1'
            }
        };
    },
    created() {
        this.getList();
        this.getLabelList();
        this.getDicts("px_video_type").then(response => {
            this.typeOptions = response.data;
        });
    },
    methods: {
        /**
         * 发送弹幕
         */
        handleSendBarrage() {
            this.barrageForm.articleId = this.form.id;
            addMessage(this.barrageForm).then(res => {
                if (res.data === 1) {
                    this.$notify.success('发送弹幕成功');
                    this.barrageForm.content = '';
                    this.handlePlay(null, this.form);
                }
            })
        },
        /**
         * 播放视频
         */
        handlePlay() {
            getMessageList({messageBoard: 8, articleId: this.form.id}).then(res => {
                this.barrageList = res.rows;
                this.play = true;
            })
        },
        /**
         * 获取待办标签
         */
        getLabelList() {
            getLabelList().then(res => {
                this.labelOptions = res.data;
            })
        },
        /**
         * 预览视频
         */
        handlePreviewVideo(row) {
            this.previewUrl = row.url;
            this.form = row;
            this.preview = true;
        },
        /**
         * 新加待办标签
         */
        handleChangeLabel(value) {
            if (!this.form.label) this.form.label = [];
            this.form.label.push(value);
        },
        /**
         * 删除待办标签
         */
        handleDeleteLabel(tag) {
            this.form.label.splice(this.form.label.indexOf(tag), 1);
        },
        /**
         * 获取视频信息
         */
        getFileInfo(data) {
            this.form.url = data.url;
        },
        /** 查询视频模块列表 */
        getList() {
            this.loading = true;
            listVideo(this.queryParams).then(response => {
                this.videoList = response.rows;
                this.total = response.total;
                this.loading = false;
            });
        },
        // 分类字典翻译
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
                title: null,
                cover: null,
                url: null,
                label: null,
                delFlag: null,
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
            this.open = true;
            this.title = "添加视频模块";
        },
        /** 修改按钮操作 */
        handleUpdate(row) {
            this.reset();
            const id = row.id || this.ids
            getVideo(id).then(response => {
                this.form = response.data;
                this.form.label = this.form.label && this.form.label.split(',');
                this.open = true;
                this.title = "修改视频模块";
            });
        },
        /** 提交按钮 */
        submitForm() {
            this.$refs["form"].validate(valid => {
                if (valid) {
                    if (Array.isArray(this.form.label) && this.form.label.length > 0) {
                        this.form.label = this.form.label.join(',');
                    }
                    if (!this.form.cover) {
                        this.form.cover = 'https://pnkx.cloud:8866/ftp/2022/04/02/4b65d9b6-bb8e-406d-ab3e-4ec587bee809.png';
                    }
                    if (this.form.id != null) {
                        updateVideo(this.form).then(response => {
                            this.msgSuccess("修改成功");
                            this.open = false;
                            this.getList();
                        });
                    } else {
                        addVideo(this.form).then(response => {
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
            this.$confirm('是否确认删除视频模块编号为"' + ids + '"的数据项?', "警告", {
                confirmButtonText: "确定",
                cancelButtonText: "取消",
                type: "warning"
            }).then(function () {
                return delVideo(ids);
            }).then(() => {
                this.getList();
                this.msgSuccess("删除成功");
            })
        },
        /** 评论管理 */
        handleMessageManage() {
            this.$router.push('/blog/video/message');
        },
        /** 导出按钮操作 */
        handleExport() {
            const queryParams = this.queryParams;
            this.$confirm('是否确认导出所有视频模块数据项?', "警告", {
                confirmButtonText: "确定",
                cancelButtonText: "取消",
                type: "warning"
            }).then(function () {
                return exportVideo(queryParams);
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
    padding: var(--space-4);
    background: var(--bg-body);
}

.barrage-content {
    margin-top: var(--space-4);
}

::v-deep .vue-danmaku {
    width: 100%;
    height: 30rem;
    margin-top: 10rem;
    position: absolute;
    top: 0;
}

.header-photo {
    width: 100%;
    height: 100%;
    display: flex;
    align-items: center;
    justify-content: center;

    .el-image {
        border-radius: var(--radius-md);
        overflow: hidden;
        box-shadow: var(--shadow-sm);
    }
}

.theme-blue-text {
    color: var(--color-primary);
    cursor: pointer;
    text-decoration: underline;
    
    &:hover {
        color: var(--color-primary-600);
    }
}
</style>
