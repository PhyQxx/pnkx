<!--
 * @File: record
 * @Author: PHY
 * @Date: 2022/5/23 9:56
 * @Description: 描述
-->
<template>
    <div class="app-container">
        <el-form ref="queryForm" :inline="true" :model="queryParams" label-width="8rem">
            <el-form-item label="卡券名称" prop="cardId">
                <el-input
                    v-model="queryParams.cardName"
                    clearable
                    placeholder="请输入卡券名称"
                    size="small"
                    @keyup.enter.native="handleQuery"
                />
            </el-form-item>
            <el-form-item label="使用用户" prop="userId">
                <el-select v-model="queryParams.userId"
                           placeholder="请选择使用用户">
                    <el-option
                        v-for="item in userList"
                        :key="item.userId"
                        :label="item.nickName"
                        :value="item.userId">
                    </el-option>
                </el-select>
            </el-form-item>
            <el-form-item>
                <el-button icon="Search" size="small" type="primary" @click="handleQuery">搜索</el-button>
                <el-button icon="Refresh" size="small" @click="resetQuery">重置</el-button>
            </el-form-item>
        </el-form>

        <el-table v-loading="loading" :data="recordList">
            <el-table-column align="center" class-name="allowDrag" label="序号" min-width="5%" type="index"/>
            <el-table-column align="center" label="卡券名称" prop="cardName"/>
            <el-table-column align="center" label="使用用户" prop="userName"/>
            <el-table-column align="center" label="使用说明" prop="instructions"/>
            <el-table-column align="center" label="使用时间" prop="createTime" width="180">
                <template v-slot="scope">
                    <span>{{ parseTime(scope.row.createTime) || '---' }}</span>
                </template>
            </el-table-column>
            <el-table-column align="center" label="确认状态" prop="confirm">
                <template v-slot="scope">
                    <span>{{ scope.row.confirm ? '已确认' : '待确认' }}</span>
                </template>
            </el-table-column>
            <el-table-column align="center" label="确认时间" prop="confirmTime" width="180">
                <template v-slot="scope">
                    <span>{{ parseTime(scope.row.confirmTime) || '---' }}</span>
                </template>
            </el-table-column>
            <el-table-column align="center" label="评分" prop="score">
                <template v-slot="scope">
                    <el-rate v-model="scope.row.score" disabled></el-rate>
                </template>
            </el-table-column>
            <el-table-column align="center" label="评分时间" prop="scoreTime" width="180">
                <template v-slot="scope">
                    <span>{{ parseTime(scope.row.scoreTime) || '---' }}</span>
                </template>
            </el-table-column>
            <el-table-column align="center" label="备注" prop="remark"/>
            <el-table-column align="center" class-name="small-padding fixed-width" label="操作">
                <template v-slot="scope">
                    <el-button
                        v-if="scope.row.userId !== $store.getters.id && !scope.row.confirm"
                        icon="CircleCheck"
                        size="small"
                        type="text"
                        @click="handleConfirm(scope.row)"
                    >确认
                    </el-button>
                    <el-button
                        v-if="scope.row.userId === $store.getters.id && scope.row.confirm && scope.row.score === 0"
                        icon="Star"
                        size="small"
                        type="text"
                        @click="handleScore(scope.row)"
                    >评分
                    </el-button>
                </template>
            </el-table-column>
        </el-table>

        <pagination
            v-show="total>0"
            v-model:limit="queryParams.pageSize"
            v-model:page="queryParams.pageNum"
            :total="total"
            @pagination="getList"
        />

        <el-dialog
            v-model="scoreVisible"
            center
            title="评分"
            top="30vh"
            custom-class="score-form"
            width="40vh">
            <el-row justify="center" type="flex">
                卡券名称：{{ row.cardName }}
            </el-row>
            <el-row justify="center" type="flex" style="margin: 1rem 0;">
                使用说明：{{ row.instructions }}
            </el-row>
            <el-row justify="center" type="flex" style="margin: 1rem 0;">
                <el-rate v-model="row.score"></el-rate>
            </el-row>
            <el-row justify="center" type="flex">
                <el-input v-model="row.remark" placeholder="请输入备注" type="textarea"></el-input>
            </el-row>
            <template #footer>
                <span class="dialog-footer">
                    <el-button @click="scoreVisible = false">取 消</el-button>
                    <el-button type="primary" @click="handleScoreConfirm">确 定</el-button>
                </span>
            </template>
        </el-dialog>
    </div>
</template>

<script>
import { confirmCard, getCardRecord, listRecord, scoreCard } from '@/api/px/life/card'
import { listUser } from '@/api/system/user'

export default {
    name: "Record",
    components: {},
    data() {
        return {
            // 评分标志
            scoreVisible: false,
            // 选中数据
            row: {},
            //用户列表
            userList: [],
            // 遮罩层
            loading: true,
            // 总条数
            total: 0,
            // 情侣卡使用记录表格数据
            recordList: [],
            // 查询参数
            queryParams: {
                pageNum: 1,
                pageSize: 10,
                cardName: null,
                userId: null,
            },
        };
    },
    created() {
        this.listUser();
        this.getList();
    },
    mounted() {
        // 获取使用记录
        const id = this.$route.params.id;
        if (id) {
            // 获取详情
            getCardRecord(id).then(res => {
               if (res.data?.confirm) {
                   // 评分
                   this.handleScore(res.data)
               } else {
                   // 确认
                   this.handleConfirm(res.data)
               }
            })
        }
    },
    methods: {
        /**
         * 确认
         * @param row
         */
        handleConfirm(row) {
            this.$confirm(`《${row.cardName}》 内容：${row.instructions}`, '卡券确认', {
                confirmButtonText: '确定',
                cancelButtonText: '取消',
                type: 'warning'
            }).then(() => {
                confirmCard(row).then(res => {
                    this.$message.success('确认成功');
                    this.handleQuery();
                })
            }).catch(() => {
            });
        },
        /**
         * 评分
         * @param row
         */
        handleScore(row) {
            this.scoreVisible = true;
            this.row = JSON.parse(JSON.stringify(row));
        },
        /**
         * 评分提交
         */
        handleScoreConfirm() {
            scoreCard(this.row).then(res => {
                this.scoreVisible = false;
                this.$message.success('评分成功');
                this.handleQuery();
            })
        },
        /**
         * 获取人员列表
         */
        listUser() {
            listUser().then(res => {
                this.userList = res.rows
            })
        },
        /** 查询情侣卡使用记录列表 */
        getList() {
            this.loading = true;
            listRecord(this.queryParams).then(response => {
                this.recordList = response.rows;
                this.total = response.total;
                this.loading = false;
            });
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
.app-container {
    padding: var(--space-6);
    background: var(--bg-body);
    min-height: calc(100vh - 84px);

    ::v-deep .el-form {
        .el-form-item__label {
            font-size: var(--text-sm);
            color: var(--text-secondary);
        }
    }

    ::v-deep .el-table {
        border-radius: var(--radius-md);
        overflow: hidden;
        border: 1px solid var(--border-primary);

        th {
            background: var(--bg-card);
            color: var(--text-primary);
            font-weight: var(--font-semibold);
            font-size: var(--text-sm);
            border-bottom: 1px solid var(--border-primary);
        }

        td {
            color: var(--text-primary);
            font-size: var(--text-sm);
            border-bottom: 1px solid var(--border-primary);
        }

        tr:hover > td {
            background: var(--bg-hover);
        }
    }

    ::v-deep .el-button {
        border-radius: var(--radius-sm);
        transition: all var(--duration-normal) var(--ease-default);
    }

    ::v-deep .el-dialog {
        border-radius: var(--radius-lg);
        overflow: hidden;

        .el-dialog__header {
            border-bottom: 1px solid var(--border-primary);
            padding: var(--space-4) var(--space-6);

            .el-dialog__title {
                font-size: var(--text-lg);
                font-weight: var(--font-semibold);
                color: var(--text-primary);
            }
        }

        .el-dialog__body {
            padding: var(--space-6);
        }

        .el-dialog__footer {
            border-top: 1px solid var(--border-primary);
            padding: var(--space-4) var(--space-6);
            background: var(--bg-body);
        }
    }

    .dialog-footer {
        display: flex;
        justify-content: flex-end;
        gap: var(--space-3);
    }
}
</style>
