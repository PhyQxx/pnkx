<template>
    <div class="app-container">
        <el-form :model="queryParams" ref="queryForm" :inline="true" v-show="showSearch" label-width="68px">
            <el-form-item label="文章标题" prop="title">
                <el-input
                    v-model="queryParams.title"
                    placeholder="请输入文章标题"
                    clearable
                    size="small"
                    @keyup.enter.native="handleQuery"
                />
            </el-form-item>
            <el-form-item label="文章分类" prop="type">
                <el-select v-model="queryParams.type" placeholder="请选择文章分类" clearable size="small">
                    <el-option
                        v-for="dict in typeOptions"
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

        <div class="article-list">
            <div class="article-one" v-for="article in articleList" :key="article.id">
                <i class="el-icon-cherry article-icon"></i>
                <div class="title pointer" @click="goToArticlePage(article)">{{article.title}}</div>
                By
                <div class="author pointer">{{article.nickName}}</div>
                <div class="type pointer" @click="goToArticlePageByType(article.type)">[{{typeFormat(article)}}]</div>
                <div class="time">{{article.createTime}}</div>
            </div>
        </div>

        <pagination
            class="pagination"
            v-show="total>0"
            :total="total"
            :page.sync="queryParams.pageNum"
            :limit.sync="queryParams.pageSize"
            @pagination="getList"
        />

    </div>
</template>

<script>
    import { listArticle, getArticleTypeList } from '@/api/px/customer/article';

    export default {
        name: "Article",
        components: {
        },
        data() {
            return {
                // 遮罩层
                loading: true,
                // 显示搜索条件
                showSearch: true,
                // 总条数
                total: 0,
                // 文章表格数据
                articleList: [],
                // 文章分类字典
                typeOptions: [],
                // 查询参数
                queryParams: {
                    pageNum: 1,
                    pageSize: 10,
                    title: null,
                    richText: null,
                    type: null,
                    createBy: null,
                    createTime: null,
                },
            };
        },
        created() {
            this.getList();
            getArticleTypeList({dictType: 'px_article_type'}).then(response => {
                this.typeOptions = response.data;
            });
        },
        methods: {
            /**
             * 点击文章类型
             */
            goToArticlePageByType(type) {
                this.queryParams.type = type;
                this.getList();
            },
            /**
             * 跳转文章详情
             */
            goToArticlePage(article) {
                sessionStorage.setItem('articleId', article.id);
                this.$router.push({name: 'article'});
                console.log(this.$route);
            },
            /** 查询文章列表 */
            getList() {
                this.loading = true;
                listArticle(this.queryParams).then(response => {
                    console.log('文章列表', response);
                    this.articleList = response.rows;
                    this.total = response.total;
                    this.loading = false;
                });
            },
            // 文章分类字典翻译
            typeFormat(row, column) {
                return this.selectDictLabel(this.typeOptions, row.type);
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
        }
    };
</script>
<style lang="scss" scoped>
    .pagination{

    }
    .top{
        display: flex;
        align-items: center;
        margin-bottom: 1rem;
    }
    .app-container ::v-deep .el-table .cell{
        max-height: 3rem;
        display: -webkit-box;
        -webkit-box-orient: vertical;
        -webkit-line-clamp: 2;
        overflow: hidden;
    }
    .article-list{
        .article-one{
            display: flex;
            align-items: center;
            margin-bottom: 0.5rem;
            font-size: 0.9rem;
            .article-icon{
                color: #d29a83;
                margin-right: 0.5rem;
            }
            .title{
                margin-right: 0.5rem;
            }
            .author{
                margin-left: 0.5rem;
            }
            .type{
                margin-left: 0.5rem;
                color: #999;
            }
            .time{
                margin-left: 0.5rem;
            }
        }
    }
</style>
