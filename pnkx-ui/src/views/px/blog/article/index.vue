<template>
    <div class="app-container">
        <el-form :model="queryParams" ref="queryForm" :inline="true" v-show="showSearch" label-width="68px">
            <el-form-item label="关键字" prop="title">
                <el-input
                    v-model="queryParams.searchValue"
                    placeholder="请输入关键字"
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
                    v-hasPermi="['px:article:add']"
                >新增
                </el-button>
            </el-col>
            <right-toolbar :showSearch="showSearch" @queryTable="getList"/>
        </el-row>

        <div class="table-main-area">
            <el-table v-loading="loading" :data="articleList" @selection-change="handleSelectionChange">
                <el-table-column label="序号" width="80" align="center">
                    <template v-slot="scope">
                        <span>{{ scope.$index + 1 }}</span>
                    </template>
                </el-table-column>
                <el-table-column class-name="article-title" label="文章标题" align="center">
                    <template v-slot="scope">
                        <span class="theme-blue-text" @click="goToArticle(scope.row)">{{ scope.row.title }}</span>
                    </template>
                </el-table-column>
                <el-table-column label="文章分类" align="center" prop="type" :formatter="typeFormat"/>
                <el-table-column align="center" label="阅读量" prop="visitsNumber"/>
                <el-table-column label="状态" align="center">
                    <template v-slot="scope">
                        <span>{{ scope.row.state === '0' ? '暂存' : '已发布' }}</span>
                    </template>
                </el-table-column>
                <el-table-column align="center" label="是否公开">
                    <template v-slot="scope">
                        <span>{{ scope.row.open === '1' ? '是' : '否' }}</span>
                    </template>
                </el-table-column>
                <el-table-column label="创建人" align="center" prop="nickName"/>
                <el-table-column label="创建时间" align="center" prop="createTime"/>
                <el-table-column label="备注" align="center" prop="remark"/>
                <el-table-column label="操作" align="center" class-name="small-padding fixed-width">
                    <template #default="scope">
                        <template v-if="scope.row.createBy == $store.getters.id">
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

    </div>
</template>

<script>
import {listArticleNotContent, delArticle, exportArticle} from "@/api/px/blog/article";

export default {
    name: "Articlemanager",
    components: {},
    data() {
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
            // 文章表格数据
            articleList: [],
            // 弹出层标题
            title: "",
            // 是否显示弹出层
            open: false,
            // 文章分类字典
            typeOptions: [],
            // 查询参数
            queryParams: {
                pageNum: 1,
                pageSize: 10,
                searchValue: null,
                title: null,
                richText: null,
                type: null,
                createBy: null,
                createTime: null,
            },
            // 表单参数
            form: {},
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
        };
    },
    created() {
        if (this.$route.query.type) {
            this.queryParams.type = this.$route.query.type
        }
        this.getList();
        this.getDicts("px_article_type").then(response => {
            this.typeOptions = response.data;
        });
    },
    methods: {
        /**
         * 跳转到文章详情
         */
        goToArticle(article) {
            this.$router.push('/blog/articledetails?adminArticleId='+article.id);
        },
        /** 查询文章列表 */
        getList(pagination) {
            if (pagination) {
                this.queryParams.pageNum = pagination.page;
                this.queryParams.pageSize = pagination.limit;
            }
            this.loading = true;
            listArticleNotContent(this.queryParams).then(response => {
                this.articleList = response.rows;
                this.total = response.total;
                this.loading = false;
            });
        },
        // 文章分类字典翻译
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
                content: null,
                richText: null,
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
            this.$router.push('/blog/articleedit')
        },
        /** 修改按钮操作 */
        handleUpdate(row) {
            this.$router.push({
                path: '/blog/articleedit',
                query: {
                    id: row.id
                }
            })
        },
        /** 提交按钮 */
        submitForm() {

        },
        /** 删除按钮操作 */
        handleDelete(row) {
            const ids = row.id || this.ids;
            this.$confirm(`是否确认删除文章《${row.title}》?`, "警告", {
                confirmButtonText: "确定",
                cancelButtonText: "取消",
                type: "warning"
            }).then(function () {
                return delArticle(ids);
            }).then(() => {
                this.getList();
                this.msgSuccess("删除成功");
            })
        },
        /** 导出按钮操作 */
        handleExport() {
            const queryParams = this.queryParams;
            this.$confirm('是否确认导出所有文章数据项?', "警告", {
                confirmButtonText: "确定",
                cancelButtonText: "取消",
                type: "warning"
            }).then(function () {
                return exportArticle(queryParams);
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

::v-deep .el-form {
    background: var(--bg-card);
    padding: var(--space-5);
    border-radius: var(--radius-lg);
    box-shadow: var(--shadow-sm);
    margin-bottom: var(--space-4);
}

.mb8 {
    margin-bottom: var(--space-2);
}

.app-container ::v-deep .el-table .cell {
    display: -webkit-box;
    -webkit-box-orient: vertical;
    -webkit-line-clamp: 2;
    overflow: hidden;
}

.theme-blue-text {
    color: var(--color-primary);
    cursor: pointer;
    transition: color var(--duration-fast) var(--ease-default);

    &:hover {
        color: var(--color-primary-600);
    }
}
</style>
