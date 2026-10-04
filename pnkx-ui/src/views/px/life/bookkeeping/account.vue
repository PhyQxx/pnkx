<!--
 * @File: account
 * @Author: PHY
 * @Date: 2021-11-05 21:05
 * @Description: 账户管理 - Modern UI Refactored
-->
<template>
    <div class="bookkeeping-account-container">
        <!-- 左侧列表面板 -->
        <account-sidebar
            :all="all"
            :account-type-list="accountTypeList"
            :list-loading="listLoading"
            @select="selectAccount"
            @contextmenu="handleContextMenu"
        />

        <!-- 右侧详情面板 -->
        <account-detail-panel
            :account-type-list="accountTypeList"
            :current-account="currentAccount"
            :overview="overview"
            :account-list="accountList"
            :loading="loading"
            @add="addNewAccount"
            @edit="editAccount"
            @delete="deleteAccount"
            @contextmenu="handleContextMenu"
        />

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
import AccountSidebar from './components/AccountSidebar.vue'
import AccountDetailPanel from './components/AccountDetailPanel.vue'

export default {
    name: 'Account',
    components: {IconSelect, AccountSidebar, AccountDetailPanel},
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
    height: 100%;
    background: var(--bg-body);
    font-family: var(--font-family-base);
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
