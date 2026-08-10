<template>
    <div class="app-container">
        <el-form :model="queryParams" ref="queryForm" :inline="true" v-show="showSearch" label-width="8rem">
            <el-form-item label="文章标题" prop="articleTitle">
                <el-input
                    v-model="queryParams.articleTitle"
                    placeholder="请输入文章标题"
                    clearable
                    size="small"
                    @keyup.enter.native="handleQuery"
                />
            </el-form-item>
            <el-form-item label="留言人名称" prop="authorName">
                <el-input
                    v-model="queryParams.authorName"
                    placeholder="请输入留言人名称"
                    clearable
                    size="small"
                    @keyup.enter.native="handleQuery"
                />
            </el-form-item>
            <el-form-item label="留言类型" prop="messageBoard">
                <el-select v-model="queryParams.messageBoard" placeholder="请选择留言类型" clearable size="small">
                    <el-option
                        v-for="(dict, index) in messageTypeList"
                        :key="dict.dictValue + index"
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

        <div class="table-main-area">
            <el-table v-loading="loading" :data="messageList" row-key="id" height="100%">
                <el-table-column label="留言类型" align="center">
                    <template v-slot="scope">
                        <span>{{ articleTitle(scope.row, true) }}</span>
                    </template>
                </el-table-column>
                <el-table-column label="文章标题" align="center">
                    <template v-slot="scope">
                        <span class="theme-blue-text" @click="goToArticle(scope.row)">{{
                                articleTitle(scope.row, false)
                            }}</span>
                    </template>
                </el-table-column>
                <el-table-column label="留言内容" align="center" show-overflow-tooltip prop="content">
                    <template v-slot="scope">
                        <div class="content" v-html="sanitizeHtml(scope.row.content)"></div>
                    </template>
                </el-table-column>
                <el-table-column label="留言人名称" align="center">
                    <template v-slot="scope">
                        <span>{{ scope.row.authorName ? scope.row.authorName : scope.row.nickName }}</span>
                    </template>
                </el-table-column>
                <el-table-column label="留言人头像图片" align="center">
                    <template v-slot="scope">
                        <div class="header-photo">
                            <el-image
                                class="header-picture"
                                :src="scope.row.avatar"
                                fit="scale-down">
                                <template #error>
                                    <div class="image-slot invalid-svg">
                                        <svg-icon icon-class="已失效2"/>
                                    </div>
                                </template>
                            </el-image>
                        </div>
                    </template>
                </el-table-column>
                <el-table-column label="留言状态" align="center">
                    <template v-slot="scope">
                        <span>{{ translationDic(scope.row.state, messageStatusList) }}</span>
                    </template>
                </el-table-column>
                <el-table-column label="ip" align="center" prop="ip"/>
                <el-table-column label="国家" align="center" prop="country"/>
                <el-table-column label="省份" align="center" prop="province"/>
                <el-table-column label="城市" align="center" prop="city"/>
                <el-table-column label="操作" align="center" class-name="small-padding fixed-width">
                    <template v-slot="scope">
                        <el-button
                            size="small"
                            type="text"
                            icon="SuccessFilled"
                            v-if="scope.row.state === '0'"
                            @click="updateMessage(scope.row, '1')"
                        >查阅
                        </el-button>
                        <el-button
                            size="small"
                            type="text"
                            icon="CircleCloseFilled"
                            v-if="scope.row.state === '1' || scope.row.state === '2'"
                            @click="updateMessage(scope.row, '3')"
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

    </div>
</template>

<script>
import {getMessageExamine, updateMessage} from "@/api/px/blog/message";
import {sanitizeHtml} from '@/utils/sanitizeHtml';

export default {
    name: "Message",
    data() {
        return {
            //留言类型列表
            messageTypeList: [],
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
            // 留言表格数据
            messageList: [],
            // 弹出层标题
            title: "",
            // 是否显示弹出层
            open: false,
            // 查询参数
            queryParams: {
                pageNum: 1,
                pageSize: 10,
                version: null,
                articleId: null,
                content: null,
                state: null,
                articleTitle: null,
                authorName: null,
                authorMailbox: null,
                authorHeader: null,
                messageBoard: null
            },
            //相册字典项
            albumList: [],
            //留言状态字典项
            messageStatusList: [],
        };
    },
    created() {
        this.getDictsData();
        this.getList();
    },
    methods: {
        /**
         * 获取相册字典项
         */
        getDictsData() {
            this.getDicts('px_message_type').then(response => {
                this.messageTypeList = response.data;
            });
            this.getDicts('px_album_name').then(response => {
                this.albumList = response.data;
            });
            this.getDicts('px_message_status').then(response => {
                this.messageStatusList = response.data;
                // 获取留言详情
                const id = this.$route.params.id;
                if (id) {
                    this.updateMessage({id}, '1');
                }
            });
        },
        /**
         * 文章标题
         */
        articleTitle(row, flag) {
            if (!row || !this.messageTypeList) return '';
            const type = this.messageTypeList.find(item => row.messageBoard === item.dictValue);
            if (!type) return '';

            if (flag) {
                return type.dictLabel + '留言';
            } else {
                if (row.messageBoard === '0') {
                    return row.articleTitle || '';
                } else if (row.messageBoard === '2' && this.albumList) {
                    const album = this.albumList.find(dict => dict.dictValue === row.articleId);
                    return album ? album.dictLabel : (type.dictLabel + '留言');
                } else {
                    return type.dictLabel + '留言';
                }
            }
        },
        /**
         * 跳转到文章详情
         */
        goToArticle(message) {
            this.messageTypeList.forEach(item => {
                if (message.messageBoard === item.dictValue) {
                    this.$router.push({
                        path: item.remark,
                        query: {
                            adminArticleId: message.articleId,
                            type: message.articleId,
                            messageId: message.id,
                            noticeId: message.articleId
                        }
                    });
                }
            });
        },
        /** 查询留言列表 */
        getList() {
            this.loading = true;
            getMessageExamine(this.queryParams).then(response => {
                this.messageList = response.rows || [];
                this.total = response.total;
                this.loading = false;
            });
        },
        sanitizeHtml,
        // 表单重置
        reset() {
            this.form = {
                id: null,
                version: null,
                createBy: null,
                createTime: null,
                updateBy: null,
                updateTime: null,
                articleTitle: null,
                articleId: null,
                content: null,
                authorName: null,
                authorMailbox: null,
                authorHeader: null,
                messageBoard: null
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
        /** 操作按钮操作 */
        updateMessage(row, state) {
            this.$confirm(`是否确认${this.choiceDic(state, this.messageStatusList).dictLabel}?`, "操作", {
                confirmButtonText: "确定",
                cancelButtonText: "取消",
                type: "warning"
            }).then(() => {
                updateMessage({id: row.id, state: state}).then(response => {
                    this.msgSuccess(`${this.choiceDic(state, this.messageStatusList).dictLabel}成功`);
                    this.getList();
                });
            })
        },
    }
};
</script>

<style lang='scss' scoped>
@import "@/assets/styles/mixin.scss";

.app-container {
    @include adaptive-table-layout(130px);
    padding: var(--space-4);
    background: var(--bg-body);
}

.header-photo {
    width: 100%;
    height: 100%;
    display: flex;
    align-items: center;
    justify-content: center;

    .el-image {
        width: 4rem;
        height: auto;
        min-height: 4rem;
        border-radius: var(--radius-full);
        overflow: hidden;
        transition: transform var(--duration-fast) var(--ease-default);

        &:hover {
            transform: scale(1.05);
        }
    }
}

.theme-blue-text {
    color: var(--color-primary);
    cursor: pointer;
    transition: color var(--duration-fast) var(--ease-default);

    &:hover {
        color: var(--color-primary-600);
    }
}

.content {
    color: var(--text-primary);
    font-size: var(--text-sm);
}
</style>
