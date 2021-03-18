<template>
    <div class="edit-page" v-loading="loading">
        <el-form ref="article" :model="article" :rules="rules" label-width="80px">
            <el-form-item label="文章标题" prop="title">
                <el-input v-model="article.title" placeholder="请输入文章标题" />
            </el-form-item>
            <el-form-item label="文章内容" prop="richText">
                <editor ref="editor" v-model="article.richText" :value="article.richText" :minHeight="300"/>
            </el-form-item>
            <el-form-item label="文章分类" prop="type">
                <el-select v-model="article.type" placeholder="请选择文章分类">
                    <el-option
                        v-for="dict in typeOptions"
                        :key="dict.dictValue"
                        :label="dict.dictLabel"
                        :value="dict.dictValue"
                    />
                </el-select>
            </el-form-item>
            <el-form-item label="备注" prop="remark">
                <el-input v-model="article.remark" type="textarea" placeholder="请输入内容" />
            </el-form-item>
        </el-form>
        <div slot="footer" class="dialog-footer">
            <el-button type="primary" @click="submitForm">确 定</el-button>
            <el-button @click="cancel">取 消</el-button>
        </div>
    </div>
</template>

<script>
    import { getDictsByLogin } from "@/api/system/dict/data";
    import { getArticle, addArticle, updateArticle } from "@/api/px/admin/blog/article";
    import editor from '@/components/Editor/index.vue'
    export default {
        name: "Articleedit",
        components: {
            editor
        },
        data() {
            return {
                //全局加载框
                loading: true,
                //文章类型列表
                typeOptions: [],
                //文章表单
                article: {
                    title: '',
                    richText: '',
                    type: '',
                    remark: ''
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
                }
            }
        },
        mounted() {
            this.getDictList();
            if (sessionStorage.getItem('articleId')) {
                this.getArticle(sessionStorage.getItem('articleId'));
            }
        },
        methods: {
            /**
             * 获取文章内容
             */
            getArticle(id) {
                this.loading = true;
                getArticle(id).then(res => {
                    this.article = res.data;
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
             * 保存文章
             */
            submitForm() {
                this.$refs["article"].validate(valid => {
                    if (valid) {
                        if (this.article.id != null) {
                            updateArticle(this.article).then(response => {
                                this.msgSuccess("修改成功");
                                this.$router.push({name: '/articlemanager'})
                                this.open = false;
                                this.getList();
                            });
                        } else {
                            addArticle(this.article).then(response => {
                                this.msgSuccess("新增成功");
                                this.$router.push({name: '/articlemanager'})
                                this.open = false;
                                this.getList();
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
        destroyed() {
            sessionStorage.removeItem('articleId')
        }
    }
</script>

<style lang="scss" scoped>
    .edit-page{
        padding: 1rem;
    }
</style>
