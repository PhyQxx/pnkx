<!--
 * @File: account
 * @Author: PHY
 * @Date: 2021-11-05 21:05
 * @Description: 账户管理 - Modern UI Refactored
-->
<template>
    <div class="bookkeeping-account-container">
        <!-- 左侧列表面板 -->
        <aside class="sidebar">
            <!-- 总览区域 -->
            <div class="overview-section">
                <div class="overview-item">
                    <span class="overview-label">总资产</span>
                    <span class="overview-value income">{{ moneyFilter(all.money) }}</span>
                </div>
                <div class="overview-divider"/>
                <div class="overview-item">
                    <span class="overview-label">总负债</span>
                    <span class="overview-value expenditure">{{ moneyFilter(all.debt) }}</span>
                </div>
                <div class="overview-divider"/>
                <div class="overview-item">
                    <span class="overview-label">净资产</span>
                    <span class="overview-value">{{ moneyFilter(all.money - all.debt) }}</span>
                </div>
            </div>

            <!-- 账户类型列表 -->
            <div v-loading="listLoading" class="type-list">
                <div v-if="accountTypeList.length < 1" class="empty-state">
                    <svg-icon icon-class="账本" class="empty-icon"/>
                    <p>暂无账户类型</p>
                </div>

                <transition-group v-else name="item-list" tag="div" class="type-items">
                    <div
                        v-for="(item, index) in accountTypeList"
                        :key="item.dictCode"
                        class="type-card"
                        :class="{ active: item.isActive }"
                        :style="{ animationDelay: `${index * 0.05}s` }"
                        @click="selectAccount(item)"
                        @contextmenu.prevent.stop="handleContextMenu($event, null)"
                    >
                        <div class="card-icon-wrapper">
                            <svg-icon :icon-class="item.remark" class="card-icon"/>
                        </div>
                        <div class="card-info">
                            <div class="card-name">{{ item.dictLabel }}</div>
                        </div>
                    </div>
                </transition-group>
            </div>
        </aside>

        <!-- 右侧详情面板 -->
        <main v-loading="loading" class="detail-area">
            <!-- 空状态 -->
            <div v-if="accountTypeList.length < 1" class="empty-detail">
                <svg-icon icon-class="账本" class="empty-detail-icon"/>
                <p>选择一个账户类型查看详情</p>
            </div>

            <!-- 账户详情 -->
            <div v-else class="account-detail">
                <!-- 头部信息 -->
                <div class="detail-header">
                    <div class="detail-title-section">
                        <h2 class="detail-name">{{ currentAccount.dictLabel }}</h2>
                        <div class="detail-stats">
                            <div class="stat-item">
                                <span class="stat-label">余额</span>
                                <span class="stat-value">{{ moneyFilter(overview.balance) }}</span>
                            </div>
                            <div class="stat-divider"/>
                            <div class="stat-item">
                                <span class="stat-label">流入</span>
                                <span class="stat-value income">{{ moneyFilter(overview.inflow) }}</span>
                            </div>
                            <div class="stat-divider"/>
                            <div class="stat-item">
                                <span class="stat-label">流出</span>
                                <span class="stat-value expenditure">{{ moneyFilter(overview.flowOut) }}</span>
                            </div>
                        </div>
                    </div>
                    <el-button type="primary" size="small" class="add-account-btn" @click="addNewAccount">
                        <el-icon>
                            <Plus/>
                        </el-icon>
                        添加账户
                    </el-button>
                </div>

                <!-- 账户卡片列表 -->
                <div class="account-list">
                    <div v-if="accountList.length < 1" class="empty-accounts">
                        <svg-icon icon-class="账本" class="empty-icon-sm"/>
                        <p>暂无账户，点击上方按钮添加</p>
                    </div>

                    <div v-else class="account-grid">
                        <div
                            v-for="(item, index) in accountList"
                            :key="index"
                            class="account-card"
                            :style="{ animationDelay: `${index * 0.05}s` }"
                            @contextmenu.prevent.stop="handleContextMenu($event, item)"
                        >
                            <div class="account-card-header">
                                <div class="account-icon-wrapper">
                                    <svg-icon :icon-class="item.accountIcon" class="account-icon"/>
                                </div>
                                <div class="account-header-info">
                                    <div class="account-name">{{ item.accountName }}</div>
                                    <div class="account-text">{{ item.accountText }}</div>
                                </div>
                            </div>
                            <div class="account-card-body">
                                <div class="balance-section">
                                    <span class="currency">CNY</span>
                                    <span class="balance-value">{{ moneyFilter(item.balance) }}</span>
                                </div>
                                <div class="flow-section">
                                    <span class="flow-label">流入</span>
                                    <span class="flow-value income">{{ moneyFilter(item.inflow) }}</span>
                                    <span class="flow-label">流出</span>
                                    <span class="flow-value expenditure">{{ moneyFilter(item.flowOut) }}</span>
                                </div>
                            </div>
                            <div v-if="item.remark" class="account-card-footer">
                                <span class="remark-label">备注：</span>{{ item.remark }}
                            </div>
                            <div class="account-card-actions">
                                <div class="action-btn" @click="editAccount(item.id)">
                                    <el-icon>
                                        <EditPen/>
                                    </el-icon>
                                    编辑
                                </div>
                                <div class="action-btn danger" @click="deleteAccount(item.id)">
                                    <el-icon>
                                        <Delete/>
                                    </el-icon>
                                    删除
                                </div>
                            </div>
                        </div>
                    </div>
                </div>
            </div>
        </main>

        <!-- 浮动新增按钮 -->
        <div class="fab-action" title="新增账户" @click="addNewAccount">
            <el-icon>
                <Plus/>
            </el-icon>
        </div>

        <!-- 新增/编辑账户对话框 -->
        <el-dialog
            :title="accountTitle + currentAccount.dictLabel"
            v-model="addAccountDialog"
            width="520px"
            custom-class="modern-dialog"
            :modal-append-to-body="true"
            :close-on-click-modal="false"
            @closed="clearAccountData"
        >
            <el-form
                ref="addForm"
                :model="addData"
                :rules="accountRules"
                label-position="top"
                class="modern-form"
            >
                <el-form-item label="账户名称" prop="accountName">
                    <el-input v-model="addData.accountName" placeholder="请输入账户名称"/>
                </el-form-item>
                <el-form-item label="账户图标">
                    <el-popover
                        placement="bottom-start"
                        width="460"
                        trigger="click"
                        @show="$refs['iconSelect'].reset()"
                    >
                        <icon-select ref="iconSelect" prefix="a-" @selected="selected"/>
                        <template #reference>
                            <el-input
                                :value="addData.accountIcon ? addData.accountIcon.slice(2) : ''"
                                placeholder="点击选择账户图标"
                                readonly
                            >
                                <template #prefix>
                                    <svg-icon
                                        v-if="addData.accountIcon"
                                        :icon-class="addData.accountIcon"
                                        class="el-input__icon"
                                        style="height: 32px;width: 16px;"
                                    />
                                    <el-icon v-else>
                                        <Search/>
                                    </el-icon>
                                </template>
                            </el-input>
                        </template>
                    </el-popover>
                </el-form-item>
                <el-form-item label="余额" prop="balance">
                    <el-input v-model="addData.balance" placeholder="请输入余额"/>
                </el-form-item>
                <el-form-item label="备注" prop="remark">
                    <el-input v-model="addData.remark" placeholder="请输入备注"/>
                </el-form-item>
            </el-form>
            <template #footer>
                <div class="dialog-footer">
                    <el-button class="btn-cancel" @click="addAccountDialog = false">取消</el-button>
                    <el-button type="primary" class="btn-confirm" @click="saveAccountInfo">确定</el-button>
                </div>
            </template>
        </el-dialog>

        <!-- 右键菜单 -->
        <transition name="context-menu">
            <div
                v-if="contextMenuVisible"
                v-clickOutSide="closeContextMenu"
                class="context-menu"
                :style="contextMenuStyle"
            >
                <div
                    v-for="item in contextMenuItems"
                    :key="item.id"
                    class="menu-item"
                    @click="handleContextAction(item)"
                >
                    <svg-icon :icon-class="item.icon" class="menu-icon"/>
                    <span>{{ item.name }}</span>
                </div>
            </div>
        </transition>
    </div>
</template>

<script>
import IconSelect from '@/components/IconSelect/index.vue'
import {addAccount, delAccount, getAccount, listAccount, updateAccount} from '@/api/px/life/bookkeeping/account'

export default {
    name: 'Account',
    components: {IconSelect},
    data() {
        return {
            // 加载标志
            listLoading: false,
            loading: false,
            // 账户类型列表
            accountTypeList: [],
            // 当前账户类型
            accountType: '',
            // 总资产
            all: {
                money: 0,
                debt: 0
            },
            // 各类账户资产
            overview: {
                balance: 0,
                inflow: 0,
                flowOut: 0
            },
            // 当前账户
            currentAccount: {},
            accountTitle: '',
            // 新增账户弹窗
            addAccountDialog: false,
            // 新增账户表单数据
            addData: {
                accountName: '',
                accountIcon: '',
                balance: '',
                remark: ''
            },
            // 新增账户校验
            accountRules: {
                accountName: {required: true, message: '请输入账户名称', trigger: 'blur'}
            },
            // 账户列表数据
            accountList: [],
            // 右键菜单
            contextMenuVisible: false,
            contextMenuStyle: '',
            contextMenuItems: [],
            contextMenuTarget: null
        }
    },
    mounted() {
        this.getAccountTypeList()
    },
    methods: {
        /**
         * 获取账户类型列表
         */
        getAccountTypeList() {
            this.listLoading = true
            this.getDicts('px_bookkeeping_account_type').then(response => {
                this.accountTypeList = response.data.map((item, index) => {
                    if (index === 0) {
                        this.currentAccount = item
                        this.accountType = item.dictValue
                        this.addData.accountIcon = 'a-现金钱包'
                        this.listAccount()
                        return {
                            ...item,
                            isActive: true
                        }
                    }
                    return {
                        ...item,
                        isActive: false
                    }
                })
                this.listLoading = false
            })
            listAccount().then(res => {
                this.all = {money: 0, debt: 0}
                res.rows.forEach(item => {
                    if (item.balance > 0) {
                        this.all.money += Number(item.balance)
                    } else {
                        this.all.debt += Number(item.balance)
                    }
                })
                this.all.debt = this.all.debt * -1
            })
        },
        /**
         * 获取当前账户类型的账户列表
         */
        listAccount() {
            listAccount({accountType: this.accountType}).then(res => {
                this.accountList = res.rows
                this.overview = {
                    balance: this.arraySum(this.accountList, 'balance'),
                    inflow: this.arraySum(this.accountList, 'inflow'),
                    flowOut: this.arraySum(this.accountList, 'flowOut')
                }
            })
        },
        /**
         * 选择账户类型
         */
        selectAccount(type) {
            this.currentAccount = type
            this.accountType = type.dictValue
            switch (this.accountType) {
                case 'jrzh':
                    this.addData.accountIcon = 'a-银行卡'
                    break
                case 'xnzh':
                    this.addData.accountIcon = 'a-虚拟账户'
                    break
                case 'xyzh':
                    this.addData.accountIcon = 'a-信用卡'
                    break
                case 'fzzh':
                    this.addData.accountIcon = 'a-负债账户'
                    break
                default:
                    this.addData.accountIcon = 'a-现金钱包'
            }
            this.listAccount()
            this.accountTypeList.forEach(item => {
                item.isActive = item.dictCode === type.dictCode
            })
        },
        /**
         * 新增账户弹窗
         */
        addNewAccount() {
            this.accountTitle = '新增'
            this.addData = {
                accountName: '',
                accountIcon: this.addData.accountIcon || 'a-现金钱包',
                balance: '',
                remark: ''
            }
            this.addAccountDialog = true
        },
        /**
         * 选择图标
         */
        selected(name) {
            this.addData.accountIcon = name
        },
        /**
         * 保存账户信息
         */
        saveAccountInfo() {
            this.$refs.addForm.validate(valid => {
                if (valid) {
                    if (this.addData.id) {
                        this.updateAccount()
                    } else {
                        this.addAccount()
                    }
                }
            })
        },
        /**
         * 新增账户
         */
        addAccount() {
            this.addData.accountType = this.currentAccount.dictValue
            addAccount(this.addData).then(res => {
                if (res.code === 200) {
                    this.addAccountDialog = false
                    this.$notify.success('新增账户成功')
                    this.listAccount()
                    this.getAccountTypeList()
                } else {
                    this.$notify.error('新增账户失败')
                }
            })
        },
        /**
         * 编辑账户
         */
        editAccount(id) {
            this.accountTitle = '修改'
            this.addAccountDialog = true
            this.getAccount(id)
        },
        /**
         * 编辑时回显账户信息
         */
        getAccount(id) {
            getAccount(id).then(res => {
                if (res.code === 200) {
                    this.addData = res.data
                }
            })
        },
        /**
         * 编辑账户保存
         */
        updateAccount() {
            updateAccount(this.addData).then(res => {
                if (res.code === 200) {
                    this.addAccountDialog = false
                    this.$notify.success('修改账户成功')
                    this.listAccount()
                    this.getAccountTypeList()
                } else {
                    this.$notify.error('修改账户信息失败')
                }
            })
        },
        /**
         * 删除账户
         */
        deleteAccount(id) {
            this.$confirm('您确定要删除该账户么？', '提示', {
                confirmButtonText: '确定',
                cancelButtonText: '取消'
            }).then(() => {
                return delAccount(id)
            }).then(() => {
                this.$notify.success('删除成功')
                this.listAccount()
                this.getAccountTypeList()
            }).catch(() => {
            })
        },
        /**
         * 关闭新增/修改弹窗时清除数据
         */
        clearAccountData() {
            this.addData = {
                accountName: '',
                accountIcon: this.addData.accountIcon || 'a-现金钱包',
                balance: '',
                remark: ''
            }
            this.$refs.addForm.clearValidate()
        },
        /**
         * 右键菜单
         */
        handleContextMenu(event, item) {
            if (item) {
                this.contextMenuTarget = item
                this.contextMenuItems = [
                    {id: 1, name: '编辑', icon: '编辑'},
                    {id: 2, name: '删除', icon: '删除'}
                ]
            } else {
                this.contextMenuItems = [
                    {id: 3, name: '新增账户', icon: '编辑02'}
                ]
            }
            this.contextMenuVisible = true
            this.contextMenuStyle = `top: ${Math.min(event.y, window.innerHeight - this.contextMenuItems.length * 48)}px; left: ${Math.min(event.x - 180, window.innerWidth - 200)}px;`
        },
        /**
         * 关闭右键菜单
         */
        closeContextMenu() {
            this.contextMenuVisible = false
        },
        /**
         * 右键菜单操作
         */
        handleContextAction(item) {
            this.contextMenuVisible = false
            switch (item.id) {
                case 1:
                    this.editAccount(this.contextMenuTarget.id)
                    break
                case 2:
                    this.deleteAccount(this.contextMenuTarget.id)
                    break
                case 3:
                    this.addNewAccount()
                    break
            }
        }
    }
}
</script>

<style lang="scss" scoped>
@import '@/assets/styles/design-tokens.scss';

$bk-red: $theme-bookkeeping-red;
$bk-green: $theme-bookkeeping-green;

.bookkeeping-account-container {
    display: flex;
    height: calc(100vh - 84px);
    background: var(--bg-body);
    font-family: var(--font-family-base);
}

// 左侧边栏
.sidebar {
    width: 320px;
    background: var(--bg-card);
    backdrop-filter: blur(20px);
    border-right: 1px solid var(--border-primary);
    display: flex;
    flex-direction: column;
    box-shadow: var(--shadow-sm);
    position: relative;
    z-index: 10;
}

// 总览区域
.overview-section {
    padding: var(--space-6) var(--space-5);
    border-bottom: 1px solid var(--border-primary);
    display: flex;
    align-items: center;
    justify-content: space-between;

    .overview-item {
        display: flex;
        flex-direction: column;
        align-items: center;
        gap: var(--space-2);

        .overview-label {
            font-size: var(--text-xs);
            color: var(--text-secondary);
            font-weight: var(--font-medium);
        }

        .overview-value {
            font-size: var(--text-xl);
            font-weight: var(--font-bold);
            color: var(--text-primary);
            font-variant-numeric: tabular-nums;

            &.income {
                color: $bk-red;
            }

            &.expenditure {
                color: $bk-green;
            }
        }
    }

    .overview-divider {
        width: 1px;
        height: 32px;
        background: var(--border-primary);
    }
}

// 账户类型列表
.type-list {
    flex: 1;
    overflow-y: auto;
    padding: var(--space-4);

    &::-webkit-scrollbar {
        width: 6px;
    }

    &::-webkit-scrollbar-track {
        background: transparent;
    }

    &::-webkit-scrollbar-thumb {
        background: var(--color-slate-300);
        border-radius: 3px;

        &:hover {
            background: var(--color-slate-400);
        }
    }
}

.type-items {
    display: flex;
    flex-direction: column;
    gap: var(--space-2);
}

.type-card {
    display: flex;
    align-items: center;
    padding: 14px var(--space-4);
    background: var(--bg-card);
    border-radius: var(--radius-lg);
    cursor: pointer;
    transition: all var(--duration-normal) var(--ease-default);
    box-shadow: var(--shadow-sm);
    border-left: 3px solid transparent;
    animation: fadeSlideIn 0.4s ease forwards;
    opacity: 0;

    &.active {
        background: var(--bg-selected);
        border-left-color: var(--color-primary);
    }

    &:hover {
        transform: translateX(4px);
        box-shadow: var(--shadow-md);
        background: var(--bg-hover);
    }

    .card-icon-wrapper {
        width: 40px;
        height: 40px;
        display: flex;
        align-items: center;
        justify-content: center;
        background: linear-gradient(135deg, #d4fc79 0%, #96e6a1 100%);
        border-radius: var(--radius-md);
        margin-right: var(--space-3);
        flex-shrink: 0;

        .card-icon {
            font-size: var(--text-2xl);
            color: white;
        }
    }

    .card-info {
        flex: 1;
        min-width: 0;

        .card-name {
            font-size: var(--text-base);
            font-weight: var(--font-medium);
            color: var(--text-primary);
        }
    }
}

.empty-state {
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    height: 100%;
    color: var(--text-tertiary);

    .empty-icon {
        font-size: 64px;
        opacity: 0.3;
        margin-bottom: var(--space-4);
    }

    p {
        margin: var(--space-1) 0;
    }
}

@keyframes fadeSlideIn {
    from {
        opacity: 0;
        transform: translateY(10px);
    }
    to {
        opacity: 1;
        transform: translateY(0);
    }
}

// 右侧详情
.detail-area {
    flex: 1;
    display: flex;
    flex-direction: column;
    overflow: hidden;
}

.empty-detail {
    flex: 1;
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    color: var(--text-tertiary);

    .empty-detail-icon {
        font-size: 80px;
        opacity: 0.2;
        margin-bottom: var(--space-5);
    }

    p {
        font-size: var(--text-lg);
    }
}

.account-detail {
    flex: 1;
    display: flex;
    flex-direction: column;
    overflow-y: auto;
}

.detail-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: var(--space-6) var(--space-8) 20px;
    background: var(--bg-card);
    border-bottom: 1px solid var(--border-primary);

    .detail-title-section {
        flex: 1;

        .detail-name {
            font-size: var(--text-3xl);
            font-weight: var(--font-semibold);
            color: var(--text-primary);
            margin: 0 0 var(--space-3) 0;
        }

        .detail-stats {
            display: flex;
            align-items: center;
            gap: var(--space-4);

            .stat-item {
                display: flex;
                flex-direction: column;
                gap: var(--space-1);

                .stat-label {
                    font-size: var(--text-xs);
                    color: var(--text-tertiary);
                }

                .stat-value {
                    font-size: var(--text-xl);
                    font-weight: var(--font-semibold);
                    color: var(--text-primary);
                    font-variant-numeric: tabular-nums;

                    &.income {
                        color: $bk-red;
                    }

                    &.expenditure {
                        color: $bk-green;
                    }
                }
            }

            .stat-divider {
                width: 1px;
                height: 28px;
                background: var(--border-primary);
            }
        }
    }

    .add-account-btn {
        border-radius: var(--radius-sm);
        background: linear-gradient(135deg, var(--color-primary) 0%, var(--color-primary-600) 100%);
        border: none;
        transition: all var(--duration-normal) var(--ease-default);

        &:hover {
            opacity: 0.9;
            transform: translateY(-1px);
        }
    }
}

// 账户列表
.account-list {
    flex: 1;
    padding: var(--space-6) var(--space-8);
    overflow-y: auto;

    &::-webkit-scrollbar {
        width: 6px;
    }

    &::-webkit-scrollbar-track {
        background: transparent;
    }

    &::-webkit-scrollbar-thumb {
        background: var(--color-slate-300);
        border-radius: 3px;

        &:hover {
            background: var(--color-slate-400);
        }
    }
}

.empty-accounts {
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    padding: 60px 0;
    color: var(--text-tertiary);

    .empty-icon-sm {
        font-size: 48px;
        opacity: 0.3;
        margin-bottom: var(--space-3);
    }

    p {
        font-size: var(--text-base);
    }
}

.account-grid {
    display: grid;
    grid-template-columns: repeat(2, 1fr);
    gap: var(--space-5);
}

.account-card {
    background: var(--bg-card);
    border-radius: var(--radius-lg);
    box-shadow: var(--shadow-sm);
    overflow: hidden;
    transition: all var(--duration-normal) var(--ease-default);
    animation: fadeSlideIn 0.4s ease forwards;
    opacity: 0;

    &:hover {
        box-shadow: var(--shadow-md);
        transform: translateY(-2px);
    }

    .account-card-header {
        display: flex;
        align-items: center;
        gap: var(--space-3);
        padding: var(--space-4) var(--space-5) var(--space-3);

        .account-icon-wrapper {
            width: 40px;
            height: 40px;
            display: flex;
            align-items: center;
            justify-content: center;
            background: linear-gradient(135deg, #d4fc79 0%, #96e6a1 100%);
            border-radius: var(--radius-md);
            flex-shrink: 0;

            .account-icon {
                font-size: var(--text-2xl);
                color: white;
            }
        }

        .account-header-info {
            flex: 1;
            min-width: 0;

            .account-name {
                font-size: var(--text-lg);
                font-weight: var(--font-semibold);
                color: var(--text-primary);
            }

            .account-text {
                font-size: var(--text-xs);
                color: var(--text-tertiary);
                margin-top: 2px;
            }
        }
    }

    .account-card-body {
        padding: 0 var(--space-5) var(--space-3);

        .balance-section {
            display: flex;
            align-items: baseline;
            gap: var(--space-2);
            margin-bottom: var(--space-2);

            .currency {
                font-size: var(--text-xs);
                color: var(--text-tertiary);
            }

            .balance-value {
                font-size: var(--text-3xl);
                font-weight: var(--font-semibold);
                color: var(--text-primary);
                font-variant-numeric: tabular-nums;
            }
        }

        .flow-section {
            display: flex;
            align-items: center;
            gap: var(--space-2);
            font-size: var(--text-sm);

            .flow-label {
                color: var(--text-tertiary);
            }

            .flow-value {
                font-weight: var(--font-medium);
                font-variant-numeric: tabular-nums;

                &.income {
                    color: $bk-red;
                }

                &.expenditure {
                    color: $bk-green;
                }
            }
        }
    }

    .account-card-footer {
        padding: var(--space-2) var(--space-5);
        font-size: var(--text-sm);
        color: var(--text-secondary);
        border-top: 1px solid var(--border-primary);

        .remark-label {
            color: var(--text-tertiary);
        }
    }

    .account-card-actions {
        display: flex;
        border-top: 1px solid var(--border-primary);

        .action-btn {
            flex: 1;
            text-align: center;
            padding: 10px 0;
            font-size: var(--text-sm);
            color: var(--text-secondary);
            cursor: pointer;
            transition: all var(--duration-fast) var(--ease-default);

            &:hover {
                color: var(--color-primary);
                background: var(--bg-hover);
            }

            &.danger:hover {
                color: var(--color-danger);
                background: var(--color-danger-light);
            }

            i {
                margin-right: var(--space-1);
            }
        }
    }
}

// FAB
.fab-action {
    position: fixed;
    right: var(--space-8);
    bottom: var(--space-8);
    width: 56px;
    height: 56px;
    border-radius: 50%;
    background: linear-gradient(135deg, var(--color-primary) 0%, var(--color-primary-600) 100%);
    color: white;
    display: flex;
    align-items: center;
    justify-content: center;
    box-shadow: 0 4px 20px rgba(14, 165, 233, 0.4);
    cursor: pointer;
    transition: all var(--duration-normal) var(--ease-default);
    z-index: 100;

    i {
        font-size: var(--text-3xl);
    }

    &:hover {
        transform: scale(1.1) rotate(90deg);
        box-shadow: 0 6px 28px rgba(14, 165, 233, 0.5);
    }

    &:active {
        transform: scale(0.95);
    }
}

// 右键菜单
.context-menu {
    position: fixed;
    background: var(--bg-card);
    border-radius: var(--radius-lg);
    box-shadow: var(--shadow-lg);
    padding: var(--space-2) 0;
    min-width: 180px;
    z-index: 9999;
    border: 1px solid var(--border-primary);

    .menu-item {
        display: flex;
        align-items: center;
        gap: var(--space-3);
        padding: var(--space-3) var(--space-4);
        font-size: var(--text-base);
        color: var(--text-primary);
        cursor: pointer;
        transition: all var(--duration-fast) var(--ease-default);

        .menu-icon {
            font-size: var(--text-lg);
            color: var(--text-secondary);
        }

        &:hover {
            background: var(--bg-hover);
            color: var(--color-primary);

            .menu-icon {
                color: var(--color-primary);
            }
        }
    }
}

.context-menu-enter-active,
.context-menu-leave-active {
    transition: all 0.2s ease;
}

.context-menu-enter,
.context-menu-leave-to {
    opacity: 0;
    transform: scale(0.95) translateY(-8px);
}

.item-list-enter-active,
.item-list-leave-active {
    transition: all 0.3s ease;
}

.item-list-enter,
.item-list-leave-to {
    opacity: 0;
    transform: translateX(-20px);
}

// 对话框样式
::v-deep .modern-dialog {
    border-radius: var(--radius-xl) !important;
    overflow: hidden;
    box-shadow: var(--shadow-xl) !important;

    .el-dialog__header {
        padding: var(--space-5) var(--space-6) var(--space-4);
        border-bottom: 1px solid var(--border-primary);
        background: var(--bg-card);

        .el-dialog__title {
            font-size: var(--text-xl);
            font-weight: var(--font-semibold);
            color: var(--text-primary);
        }
    }

    .el-dialog__body {
        padding: var(--space-6);
    }

    .el-dialog__footer {
        padding: var(--space-4) var(--space-6) var(--space-5);
        border-top: 1px solid var(--border-primary);
        background: var(--bg-body);
    }
}

.modern-form {
    ::v-deep .el-form-item {
        margin-bottom: var(--space-5);

        .el-form-item__label {
            font-size: var(--text-sm);
            font-weight: var(--font-medium);
            color: var(--text-secondary);
            padding-bottom: var(--space-2);
        }

        .el-input__inner {
            border-radius: var(--radius-sm);
            border-color: var(--border-primary);
            transition: all var(--duration-normal) var(--ease-default);

            &:focus {
                border-color: var(--color-primary);
                box-shadow: 0 0 0 3px var(--color-primary-100);
            }
        }
    }
}

.dialog-footer {
    display: flex;
    justify-content: flex-end;
    gap: var(--space-3);

    .btn-cancel {
        border-radius: var(--radius-sm);
        padding: 10px var(--space-5);
        transition: all var(--duration-normal) var(--ease-default);

        &:hover {
            background: var(--bg-hover);
        }
    }

    .btn-confirm {
        border-radius: var(--radius-sm);
        padding: 10px var(--space-6);
        background: linear-gradient(135deg, var(--color-primary) 0%, var(--color-primary-600) 100%);
        border: none;
        transition: all var(--duration-normal) var(--ease-default);

        &:hover {
            opacity: 0.9;
            transform: translateY(-1px);
        }
    }
}

// Loading 美化
::v-deep .el-loading-mask {
    background-color: rgba(255, 255, 255, 0.9);
    backdrop-filter: blur(4px);
}

// 通知美化
::v-deep .el-notification {
    border-radius: var(--radius-lg);
    box-shadow: var(--shadow-lg);
    border: none;
}
</style>
