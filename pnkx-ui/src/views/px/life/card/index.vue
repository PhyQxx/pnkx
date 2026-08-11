<!--
 * @File: manage
 * @Author: PHY
 * @Date: 2022/5/22 10:19
 * @Description: 卡券页面
-->
<template>
    <div class="card-page">
        <el-tabs v-model="activeTab" class="card-tabs">
            <el-tab-pane label="我的卡券" name="mine">
                <div class="card-mine-container">
                    <el-row justify="end" type="flex">
                        <el-button type="text" @click="getMyCards">刷新</el-button>
                    </el-row>
                    <el-row class="page-title">
                        我的卡券
                        <span class="total-value">总价值：￥{{totalValue}}</span>
                    </el-row>
                    <el-row v-loading="loading" class="my-card">
                        <el-col v-for="item in list" :key="item.id" :span="12" class="one-card">
                            <div class="card">
                    <div class="left">
                        <el-image :preview-src-list="[item.logo]"
                                  :src="item.thumbnail || item.logo"
                                  fit="scale-down"
                                  style="width: 5rem; height: 5rem;">
                            <div slot="error" class="image-slot invalid-svg">
                                <svg-icon icon-class="已失效2"/>
                            </div>
                        </el-image>
                        <div class="right">
                            <div class="title">{{ item.title }}</div>
                            <div class="pinyin">{{ item.pinyin }}</div>
                            <el-popover
                                :content="item.describe"
                                placement="bottom-start"
                                trigger="hover">
                                <template #reference>
                                    <div class="describe">{{ item.describe }}</div>
                                </template>
                            </el-popover>
                        </div>
                    </div>
                    <div class="function">
                        <el-button size="small" type="text" @click="handleUseCard(item)">使用卡券</el-button>
                        <div class="number">
                            数量：{{ item.cardNumber }}
                        </div>
                    </div>
                </div>
            </el-col>
            <el-empty v-if="list.length < 1" description="暂无卡券"></el-empty>
                    </el-row>
                </div>
            </el-tab-pane>
            <el-tab-pane label="卡券管理" name="manage">
                <card-manage v-if="activeTab === 'manage'" />
            </el-tab-pane>
            <el-tab-pane label="使用记录" name="record">
                <card-record v-if="activeTab === 'record'" />
            </el-tab-pane>
        </el-tabs>
    </div>
</template>

<script>
import {getCardByUserId, useCard} from "@/api/px/life/card";
import chineseHelper from '@/utils/chineseHelper.js'
import CardManage from './manage.vue'
import CardRecord from './record.vue'

export default {
    name: "MyCard",
    components: { CardManage, CardRecord },
    data() {
        return {
            // 当前激活的 tab
            activeTab: 'mine',
            // 加载标志
            loading: false,
            // 我的卡券列表
            list: [],
            // 卡券总价值
            totalValue: 0
        }
    },
    mounted() {
        this.getMyCards()
    },
    methods: {
        /**
         * 使用卡券
         */
        handleUseCard(item) {
            this.$prompt(`确认使用${item.title}吗？请输入您的要求！`, '提示', {
                confirmButtonText: '确定',
                cancelButtonText: '取消',
                inputPattern: /\S/,
                inputErrorMessage: '请输入命令'
            }).then(({ value }) => {
                useCard({
                    cardId: item.cardId,
                    instructions: value
                }).then(res => {
                    this.$message.success('使用成功');
                    this.getMyCards();
                })
            }).catch(() => {
            });
        },
        /**
         * 获取我的卡券
         */
        getMyCards() {
            this.loading = true;
            getCardByUserId().then(res => {
                this.list = res.data.map(item => {
                    item.pinyin = chineseHelper.ConvertPinyin(item.title)
                    return item;
                });
                this.totalValue = 0;
                this.list.forEach(item => {
                    this.totalValue += Number(item.cardNumber)* Number(item.money)
                })
                this.loading = false;
            })
        }
    }
}
</script>

<style lang='scss' scoped>
.card-page {
    height: calc(100vh - 84px);
    padding: 16px;
    box-sizing: border-box;
    display: flex;
    flex-direction: column;
}

.card-tabs {
    flex: 1;
    display: flex;
    flex-direction: column;
    overflow: hidden;

    :deep(.el-tabs__content) {
        flex: 1;
        overflow: auto;
    }
}

.card-mine-container {
    min-height: 100%;
}

.my-card {
    height: 76vh;
    overflow: auto;
    padding: var(--space-2);
}
.total-value {
    margin-left: var(--space-4);
    font-size: var(--text-lg);
    color: var(--color-primary);
    font-weight: var(--font-semibold);
}
.page-title {
    font-size: var(--text-xl);
    font-weight: var(--font-bold);
    color: var(--text-primary);
    padding: var(--space-4) var(--space-6);
}
.one-card {
    &:nth-child(2n) {
        padding-left: var(--space-4);
    }

    .card {
        display: flex;
        border: 1px solid var(--border-primary);
        margin-bottom: var(--space-4);
        border-radius: var(--radius-md);
        padding: var(--space-4);
        justify-content: space-between;
        background: var(--bg-card);
        box-shadow: var(--shadow-sm);
        transition: box-shadow var(--duration-normal) var(--ease-default), transform var(--duration-normal) var(--ease-default);

        &:hover {
            box-shadow: var(--shadow-md);
            transform: translateY(-2px);
        }

        .left {
            display: flex;

            .svg-icon {

            }

            .right {
                display: flex;
                flex-flow: column;
                align-items: center;
                justify-content: center;

                .title {
                    font-family: 隶书;
                    font-size: var(--text-2xl);
                    font-weight: var(--font-bold);
                    color: var(--color-primary);
                    margin-bottom: 0;
                }

                .pinyin {
                    margin: var(--space-2) 0 var(--space-4) 0;
                    color: var(--color-primary-600);
                    font-weight: var(--font-bold);
                    font-size: var(--text-sm);
                }

                .describe {
                    margin-top: var(--space-4);
                    color: var(--text-tertiary);
                    width: 14rem;
                    text-align: center;
                    display: -webkit-box;
                    -webkit-box-orient: vertical;
                    -webkit-line-clamp: 3;
                    overflow: hidden;
                    font-size: var(--text-sm);
                }
            }
        }

        .function {
            display: flex;
            flex-flow: column;
            align-items: end;
            justify-content: space-between;

            .number {
                color: var(--color-danger, #f56c6c);
                font-weight: var(--font-semibold);
                font-size: var(--text-sm);
            }
        }
    }
}
</style>
