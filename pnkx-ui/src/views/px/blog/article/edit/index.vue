<template>
    <div class="edit-page" v-loading="loading">
        <el-form ref="article" :model="article" :rules="rules" label-width="80px">
            <div class="form-item">
                <el-form-item label="文章封面" prop="cover">
                    <imageUpload v-model="article.cover" image-type="wzfm"/>
                </el-form-item>
                <div class="">
                    <div class="form-item">
                        <el-form-item label="文章标题" prop="title">
                            <el-input v-model="article.title" placeholder="请输入文章标题"/>
                        </el-form-item>
                        <el-form-item label="文章分类" prop="type">
                            <div style="display:flex;align-items:center;gap:8px;">
                                <el-select placeholder="请选择文章分类" v-model="article.type">
                                    <el-option
                                        :key="dict.dictValue"
                                        :label="dict.dictLabel"
                                        :value="dict.dictValue"
                                        v-for="dict in typeOptions"
                                    />
                                </el-select>
                                <el-button
                                    type="text"
                                    icon="CirclePlus"
                                    @click="openAddTypeDialog"
                                    title="新增文章类型"
                                >新增类型</el-button>
                            </div>
                        </el-form-item>

                        <!-- 新增文章类型对话框 -->
                        <el-dialog
                            title="新增文章类型"
                            v-model="addTypeDialogVisible"
                            width="400px"
                            append-to-body
                            @close="resetAddTypeForm"
                        >
                            <el-form ref="addTypeForm" :model="addTypeForm" :rules="addTypeRules" label-width="80px">
                                <el-form-item label="类型名称" prop="dictLabel">
                                    <el-input v-model="addTypeForm.dictLabel" placeholder="请输入类型名称（如：技术、生活）"/>
                                </el-form-item>
                                <el-form-item label="类型键值" prop="dictValue">
                                    <el-input v-model="addTypeForm.dictValue" placeholder="请输入唯一键值（如：tech、life）"/>
                                </el-form-item>
                                <el-form-item label="排序" prop="dictSort">
                                    <el-input-number v-model="addTypeForm.dictSort" :min="0" :max="999" style="width:100%"/>
                                </el-form-item>
                                <el-form-item label="备注">
                                    <el-input v-model="addTypeForm.remark" type="textarea" :rows="2" placeholder="可选备注"/>
                                </el-form-item>
                            </el-form>
                            <template #footer>
                                <span>
                                    <el-button @click="addTypeDialogVisible = false">取 消</el-button>
                                    <el-button type="primary" :loading="addTypeLoading" @click="submitAddType">确 定</el-button>
                                </span>
                            </template>
                        </el-dialog>
                        <el-form-item label="文章标签" prop="tag">
                        <el-select v-model="article.tag"
                                   multiple
                                   filterable
                                   allow-create
                                   placeholder="请选择标签">
                            <el-option
                                v-for="item in tagList"
                                :key="item"
                                :label="item"
                                :value="item">
                            </el-option>
                        </el-select>
                        </el-form-item>
                        <el-form-item label="文章状态" prop="open">
                            <el-select placeholder="请选择文章状态" v-model="article.open">
                                <el-option label="公开" value="1"/>
                                <el-option label="隐藏" value="0"/>
                            </el-select>
                        </el-form-item>
                    </div>
                    <div class="form-item">
                        <el-form-item label="备注" prop="remark">
                            <el-input placeholder="请输入内容" type="textarea" v-model="article.remark"/>
                        </el-form-item>
                        <div class="button">
                            <el-button @click="resetForm()">重 置</el-button>
                            <el-button @click="submitForm('1')" type="primary">发 布</el-button>
                        </div>
                    </div>
                </div>
            </div>
            <el-form-item class="article-content" label="文章内容" prop="richText">
                <CherryMarkdownEditor ref="CherryMarkdown" v-if="!loading" height="67.5vh"
                                      v-model="article.richText"></CherryMarkdownEditor>
            </el-form-item>
        </el-form>
    </div>
</template>

<script>
import {getDictsByLogin, addData} from "@/api/system/dict/data";
import {addArticle, getArticle, getLabelList, updateArticle} from "@/api/px/blog/article";
import ImageUpload from "@/components/ImageUpload/index.vue";

export default {
    name: "ArticleEdit",
    components: {ImageUpload},
    data() {
        return {
            // 定时保存
            scheduledSave: undefined,
            //全局加载框
            loading: false,
            //文章类型列表
            typeOptions: [],
            //文章表单
            article: {
                title: '',
                richText: '',
                type: '',
                tag: [],
                remark: ''
            },
            // 标签列表
            tagList: [],
            // 新增文章类型对话框
            addTypeDialogVisible: false,
            addTypeLoading: false,
            addTypeForm: {
                dictLabel: '',
                dictValue: '',
                dictSort: 0,
                dictType: 'px_article_type',
                status: '0',
                remark: ''
            },
            addTypeRules: {
                dictLabel: [
                    {required: true, message: '类型名称不能为空', trigger: 'blur'}
                ],
                dictValue: [
                    {required: true, message: '类型键值不能为空', trigger: 'blur'}
                ]
            },
            // 表单校验
            rules: {
                title: [
                    {required: true, message: "文章标题不能为空", trigger: "blur"}
                ],
                richText: [
                    {required: true, message: "文章内容不能为空", trigger: "blur"}
                ],
                type: [
                    {required: true, message: "文章分类不能为空", trigger: "change"}
                ],
                open: [
                    {required: true, message: "文章状态不能为空", trigger: "change"}
                ],
            }
        }
    },
    mounted() {
        this.getLabelList();
        this.getDictList();
        if (this.$route.query.id) {
            this.getArticle(this.$route.query.id);
        } else {
            this.loading = true;
            this.article = this.getLocal('articleForm') || {
                title: '',
                richText: '',
                type: '',
                remark: ''
            };
            this.loading = false;
        }
        this.scheduledSave = setInterval(() => {
            if (this.article.title || this.article.richText) {
                this.setLocal('articleForm', this.article);
                this.$notify.success('本地保存成功')
            }
        }, 30000)
    },
    methods: {
        /**
         * 打开新增文章类型对话框
         */
        openAddTypeDialog() {
            this.addTypeDialogVisible = true
        },
        /**
         * 重置新增类型表单
         */
        resetAddTypeForm() {
            this.$refs['addTypeForm'] && this.$refs['addTypeForm'].resetFields()
            this.addTypeForm = {
                dictLabel: '',
                dictValue: '',
                dictSort: 0,
                dictType: 'px_article_type',
                status: '0',
                remark: ''
            }
        },
        /**
         * 提交新增文章类型
         */
        submitAddType() {
            this.$refs['addTypeForm'].validate(valid => {
                if (valid) {
                    this.addTypeLoading = true
                    addData(this.addTypeForm).then(res => {
                        if (res.code === 200) {
                            this.$notify.success('新增类型成功')
                            this.addTypeDialogVisible = false
                            // 刷新类型列表，并自动选中新类型
                            this.getDictList()
                            this.article.type = this.addTypeForm.dictValue
                        }
                        this.addTypeLoading = false
                    }).catch(() => {
                        this.addTypeLoading = false
                    })
                }
            })
        },
        /**
         * 获取待办标签
         */
        getLabelList() {
            getLabelList().then(res => {
                this.tagList = res.data;
            })
        },
        /**
         * 获取文章内容
         */
        getArticle(id) {
            this.loading = true;
            getArticle(id).then(res => {
                this.article = res.data;
                if (this.article.tag) {
                    this.article.tag = this.article.tag.split(',');
                } else {
                    this.article.tag = []
                }
                this.loading = false;
            });
        },
        /**
         * 获取文章类型列表
         */
        getDictList() {
            getDictsByLogin('px_article_type').then(res => {
                this.typeOptions = res.data;
            })
        },
        /**
         * 重置
         */
        resetForm() {
            this.article = {};
            this.removeLocal('articleForm')
        },
        /**
         * 保存文章
         */
        submitForm(state) {
            this.$refs["article"].validate(valid => {
                if (valid) {
                    this.loading = true;
                    this.article.content = this.$refs.CherryMarkdown.getData();
                    this.article.state = state;
                    this.article.tag = this.article.tag.join(',');
                    if (this.article.id != null) {
                        updateArticle(this.article).then(response => {
                            if (response.code === 200) {
                                this.$notify({
                                    type: 'success',
                                    message: '修改成功'
                                });
                                this.article.id = response.data;
                                this.removeLocal('articleForm')
                                this.loading = false;
                                this.$router.push({path: '/blog/articlemanager'});
                                this.open = false;
                            }
                        });
                    } else {
                        addArticle(this.article).then(response => {
                            if (response.code === 200) {
                                this.$notify({
                                    type: 'success',
                                    message: '新增成功'
                                });
                                this.removeLocal('articleForm')
                                this.loading = false;
                                this.$router.push({path: '/blog/articlemanager'});
                                this.open = false;
                            }
                        });
                    }
                }
            });
        },
        /**
         * 取消按钮
         */
        cancel() {

        }
    },
    beforeUnmount() {
        clearInterval(this.scheduledSave);
    }
}
</script>

<style lang="scss" scoped>
.edit-page {
    padding: var(--space-6);
    background: var(--bg-body);
    min-height: 100%;

    ::v-deep .auto-textarea-input {
        min-height: 65vh;
    }

    .form-item {
        display: flex;
        align-items: center;
        justify-content: flex-start;

        .button {
            display: flex;
            justify-content: center;
            align-items: center;
        }

        .el-form-item {
            margin-right: var(--space-8);
        }
    }
}
</style>
