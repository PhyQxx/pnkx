<template>
    <div class="add-record">
        <!-- 顶部操作栏：类型 Tab + AI + 删除 -->
        <div class="add-record__header-row">
            <div class="add-record__type-tabs">
                <div
                    v-for="tab in typeTabs"
                    :key="tab.value"
                    class="add-record__tab-item"
                    :class="{ active: active === tab.value }"
                    @click="handleTabChange(tab.value)"
                >
                    {{ tab.label }}
                </div>
                <div class="add-record__tab-indicator" :style="indicatorStyle" />
            </div>

            <div class="add-record__ai-toggle" :class="{ active: showAiInput }" @click="showAiInput = !showAiInput">
                <img v-if="icons.ai" class="add-record__ai-icon" :src="icons.ai" alt="">
                <van-icon v-else name="magic-star-o" />
                <span class="add-record__ai-toggle-text">AI</span>
            </div>

            <div v-if="recordForm.id" class="add-record__delete-btn" @click="handleDelete">
                <van-icon name="delete-o" />
            </div>
        </div>

        <!-- 中间内容区域（可滚动） -->
        <div class="add-record__content">
            <!-- AI 记账输入 -->
            <div class="add-record__ai-section" v-if="showAiInput">
                <div class="add-record__ai-row">
                    <input
                        v-model="aiInputText"
                        placeholder="说点什么，如：午餐花了25元"
                        class="add-record__ai-input"
                        @keyup.enter="handleAiParse"
                    >
                    <div class="add-record__ai-parse-btn" :class="{ disabled: aiParsing }" @click="handleAiParse">
                        {{ aiParsing ? '解析中...' : '解析' }}
                    </div>
                </div>
            </div>

            <!-- 分类宫格（转账无分类） -->
            <div class="add-record__section" v-if="active !== '2'">
                <div class="add-record__category-grid">
                    <div
                        v-for="(category, index) in currentCategoryList"
                        :key="category.id"
                        class="add-record__category-item"
                        :class="{ selected: temporaryForm.primaryType === category.id || temporaryForm.primaryType === category.typeName }"
                        @click="handleSelectPrimaryType(category)"
                    >
                        <div class="add-record__category-icon" :style="{ backgroundColor: getCategoryColor(index) }">
                            <img v-if="iconOf(category.typeIcon)" :src="iconOf(category.typeIcon)" alt="">
                            <van-icon v-else name="bill-o" />
                        </div>
                        <div class="add-record__category-label">{{ category.typeName }}</div>
                    </div>
                </div>
            </div>

            <!-- 金额与账户 -->
            <div class="add-record__section">
                <div class="add-record__current-category" v-if="active !== '2'" @click="openSecondaryPicker">
                    <van-icon name="apps-o" />
                    <span class="add-record__chip-text">{{ currentCategoryName || '选择分类' }}</span>
                    <van-icon name="arrow" />
                </div>

                <div class="add-record__amount-display">
                    <span class="add-record__amount-symbol">¥</span>
                    <span class="add-record__amount-value" :class="'add-record__amount-value--' + active">{{ formatAmount(recordForm.money) }}</span>
                    <span class="add-record__amount-process">{{ money }}</span>
                </div>

                <div class="add-record__account-select" v-if="active !== '2'" @click="openAccountPicker('account')">
                    <van-icon name="balance-o" />
                    <span class="add-record__chip-text">{{ temporaryForm.account || '选择账户' }}</span>
                    <van-icon name="arrow" />
                </div>

                <template v-if="active === '2'">
                    <div class="add-record__account-select" @click="openAccountPicker('out')">
                        <van-icon name="balance-o" />
                        <span class="add-record__chip-text">{{ temporaryForm.account || '选择转出账户' }}</span>
                        <van-icon name="arrow" />
                    </div>
                    <div class="add-record__account-select" @click="openAccountPicker('in')">
                        <van-icon name="exchange" />
                        <span class="add-record__chip-text">{{ temporaryForm.otherAccountName || '选择转入账户' }}</span>
                        <van-icon name="arrow" />
                    </div>
                </template>

                <div class="add-record__account-select" v-if="active !== '2'" @click="showCommemorationPicker = true">
                    <van-icon name="gift-o" />
                    <span class="add-record__chip-text">{{ selectedCommemorationName }}</span>
                    <van-icon name="arrow" />
                </div>

                <div class="add-record__note-section">
                    <input
                        v-model="recordForm.remark"
                        class="add-record__note-input"
                        placeholder="点击填写备注信息"
                        maxlength="200"
                    >
                    <div class="add-record__image-attachments">
                        <div class="add-record__attachment-btn" @click="handleCamera">
                            <van-icon name="photograph" />
                        </div>
                        <div v-if="imageList.length > 0" class="add-record__image-preview">
                            <img
                                v-for="(img, index) in imageList"
                                :key="index"
                                :src="img"
                                class="add-record__preview-image"
                                alt=""
                                @click="handlePreviewImage(index)"
                            >
                        </div>
                    </div>
                </div>
            </div>
        </div>

        <!-- 数字键盘 -->
        <div class="add-record__calculator">
            <calculator @getInputValue="handleCalculatorInput" @confirm="handleSave" />
        </div>

        <!-- 账户选择弹层 -->
        <van-popup v-model:show="showAccountPopup" position="bottom" round teleport="body">
            <div class="add-record__picker">
                <div class="add-record__picker-header">
                    <span class="add-record__picker-cancel" @click="showAccountPopup = false">取消</span>
                    <span class="add-record__picker-title">选择账户</span>
                    <span class="add-record__picker-confirm" @click="confirmAccount">确定</span>
                </div>
                <div class="add-record__picker-content">
                    <div v-for="(group, gIndex) in accountColumns" :key="gIndex" class="add-record__account-group">
                        <div class="add-record__group-label">{{ group.accountName || group.typeName }}</div>
                        <div
                            v-for="account in group.children || []"
                            :key="account.id"
                            class="add-record__account-item"
                            :class="{ selected: temporaryAccountId === account.id }"
                            @click="selectAccountItem(account)"
                        >
                            <div class="add-record__account-item-name">{{ account.accountName }}</div>
                            <div class="add-record__account-item-amount">¥{{ account.balance || 0 }}</div>
                            <van-icon v-if="temporaryAccountId === account.id" name="success" />
                        </div>
                    </div>
                </div>
            </div>
        </van-popup>

        <!-- 二级分类选择弹层 -->
        <van-popup v-model:show="showSecondaryPopup" position="bottom" round teleport="body">
            <div class="add-record__picker">
                <div class="add-record__picker-header">
                    <span class="add-record__picker-cancel" @click="showSecondaryPopup = false">取消</span>
                    <span class="add-record__picker-title">选择分类</span>
                    <span class="add-record__picker-confirm" @click="confirmSecondaryType">确定</span>
                </div>
                <div class="add-record__picker-content">
                    <div
                        v-for="item in currentSecondaryTypeList"
                        :key="item.id"
                        class="add-record__picker-item"
                        :class="{ selected: temporarySecondaryType === item.id }"
                        @click="selectSecondaryItem(item)"
                    >
                        {{ item.typeName }}
                        <van-icon v-if="temporarySecondaryType === item.id" name="success" />
                    </div>
                </div>
            </div>
        </van-popup>

        <!-- 关联纪念日选择弹层 -->
        <van-popup v-model:show="showCommemorationPicker" position="bottom" round teleport="body">
            <van-picker
                title="关联纪念日"
                :columns="commemorationColumns"
                @confirm="onCommemorationConfirm"
                @cancel="showCommemorationPicker = false"
            />
        </van-popup>

        <!-- 图片选择 -->
        <input
            ref="fileInput"
            type="file"
            accept="image/*"
            multiple
            style="display: none"
            @change="onFileChange"
        >
    </div>
</template>

<script>
import { getClassificationList } from '@/api/px/life/bookkeeping/classification'
import { getAccountList } from '@/api/px/life/bookkeeping/account'
import { addRecord, aiParse, delRecord, getRecord, updateRecord } from '@/api/px/life/bookkeeping/record'
import { listDay } from '@/api/px/life/commemorationDay'
import { uploadImage } from '@/api/system/image'
import { showImagePreview } from 'vant'
import 'vant/es/image-preview/style'
import { iconUrl } from '@/mobile/utils/icons'
import { mConfirm, mError, mLoading, mSuccess, closeToast, showToast } from '@/mobile/utils/ui'
import Calculator from './components/Calculator.vue'

export default {
    name: 'MBookkeepingRecordAdd',
    components: { Calculator },
    data() {
        return {
            // 记账类型标签
            typeTabs: [
                { label: '支出', value: '1' },
                { label: '收入', value: '0' },
                { label: '转账', value: '2' }
            ],
            // 当前记账类型
            active: '1',
            // 输入中的算式
            money: '',
            // 展示用的临时表单
            temporaryForm: {
                primaryType: '',
                secondaryType: '',
                account: '',
                otherAccountName: ''
            },
            // 提交表单
            recordForm: {
                id: null,
                money: 0,
                payTime: new Date(),
                remark: '',
                type: '',
                account: '',
                otherAccount: '',
                commemorationDayId: null,
                typeDifference: '1'
            },
            // 账户弹窗目标：account=普通账户 out=转出 in=转入
            accountPickerMode: 'account',
            typeColumns: [],
            accountColumns: [],
            imageList: [],
            temporaryAccountId: '',
            temporarySecondaryType: '',
            categoryColors: ['#DDEAFF', '#FFE4E4', '#DCF8E8', '#FFF1C9', '#EEE7FF', '#FCE7F3', '#DDF7EE', '#E1F0FF'],
            isInitializing: false,
            // AI 记账
            showAiInput: false,
            aiInputText: '',
            aiParsing: false,
            // 纪念日
            commemorationDays: [],
            commemorationIndex: 0,
            // 弹层显隐
            showAccountPopup: false,
            showSecondaryPopup: false,
            showCommemorationPicker: false,
            icons: {
                ai: iconUrl('ai')
            }
        }
    },
    computed: {
        currentCategoryList() {
            return this.typeColumns
        },
        currentSecondaryTypeList() {
            if (this.temporaryForm.primaryType) {
                const parent = this.typeColumns.find(
                    item => item.id === this.temporaryForm.primaryType || item.typeName === this.temporaryForm.primaryType
                )
                return parent ? parent.children : []
            }
            return []
        },
        currentCategoryName() {
            for (const parent of this.typeColumns) {
                if (parent.children) {
                    const child = parent.children.find(c => c.id === this.recordForm.type)
                    if (child) {
                        return child.typeName
                    }
                }
            }
            if (this.recordForm.typeObject) {
                return this.recordForm.typeObject.typeName
            }
            const primary = this.typeColumns.find(item => item.id === this.recordForm.type)
            if (primary) {
                return primary.typeName
            }
            return ''
        },
        indicatorStyle() {
            const index = this.typeTabs.findIndex(tab => tab.value === this.active)
            const step = 100 / this.typeTabs.length
            return `left: ${index * step}%;`
        },
        commemorationColumns() {
            return this.commemorationLabels.map((text, index) => ({ text, value: index }))
        },
        commemorationLabels() {
            return ['不关联纪念日'].concat(this.commemorationDays.map(item => item.name))
        },
        selectedCommemorationName() {
            return this.commemorationLabels[this.commemorationIndex] || '不关联纪念日'
        }
    },
    watch: {
        /**
         * 切换记账类型：重置分类与账户，避免不同类型间串数据
         */
        active(newAction) {
            if (this.isInitializing) return
            this.recordForm.typeDifference = newAction
            this.temporaryForm.primaryType = ''
            this.temporaryForm.secondaryType = ''
            this.temporaryForm.account = ''
            this.temporaryForm.otherAccountName = ''
            this.recordForm.account = ''
            this.recordForm.otherAccount = ''
            this.temporaryAccountId = ''
            if (newAction === '2') {
                // 转账：无分类，type 固定为 0（type=0 的记录不关联分类）
                this.recordForm.type = 0
            } else {
                this.recordForm.type = ''
            }
            this.loadListsAndSetDefault()
        }
    },
    mounted() {
        // query 预填：recordId=编辑、date=预填日期、text=AI 解析文本
        const query = this.$route.query || {}
        if (query.recordId) {
            this.recordForm.id = query.recordId
        }
        if (query.date && !query.recordId) {
            this.recordForm.payTime = new Date(query.date + 'T12:00:00')
        }
        this.initData().then(() => {
            if (query.text) {
                this.showAiInput = true
                this.aiInputText = String(query.text)
                this.handleAiParse()
            }
        })
    },
    methods: {
        iconOf(name) {
            return iconUrl(name)
        },
        getCategoryColor(index) {
            return this.categoryColors[index % this.categoryColors.length]
        },
        async initData() {
            this.isInitializing = true
            try {
                await this.loadCommemorationDays()
                if (this.recordForm.id) {
                    const res = await getRecord(this.recordForm.id)
                    // 转账记录 typeObject 为 null，靠 otherAccount 兜底识别
                    this.active = (res.data.typeObject && res.data.typeObject.typeDifference)
                        || (res.data.otherAccount ? '2' : '1')
                    this.recordForm = {
                        ...res.data,
                        payTime: new Date(res.data.payTime || new Date())
                    }
                    this.money = String(this.recordForm.money || 0)
                    this.recordForm.typeDifference = this.active
                    this.syncCommemorationIndex()
                    this.imageList = res.data.images ? res.data.images.split(',').filter(item => item) : []
                    if (res.data.typeObject) {
                        this.temporaryForm.primaryType = res.data.typeObject.typeParentId || res.data.typeObject.id
                        this.temporaryForm.secondaryType = res.data.typeObject.id
                        this.temporarySecondaryType = res.data.typeObject.id
                    }
                    if (res.data.account) {
                        this.temporaryForm.account = res.data.accountObject ? res.data.accountObject.accountName : res.data.accountName
                        this.recordForm.account = res.data.account
                        this.temporaryAccountId = res.data.account
                    }
                    if (res.data.otherAccount) {
                        this.recordForm.otherAccount = res.data.otherAccount
                        this.temporaryForm.otherAccountName = res.data.otherAccountObject
                            ? res.data.otherAccountObject.accountName : ''
                    }
                    await this.loadListsAndSetDefault()
                } else {
                    await this.loadListsAndSetDefault()
                }
            } catch (error) {
                console.error('初始化数据失败:', error)
            } finally {
                this.isInitializing = false
            }
        },
        async loadCommemorationDays() {
            try {
                const res = await listDay({ pageNum: 1, pageSize: 100 })
                this.commemorationDays = res.rows || res.data || []
                this.syncCommemorationIndex()
            } catch (e) {
                this.commemorationDays = []
            }
        },
        syncCommemorationIndex() {
            const index = this.commemorationDays.findIndex(item => item.id === this.recordForm.commemorationDayId)
            this.commemorationIndex = index >= 0 ? index + 1 : 0
        },
        onCommemorationConfirm({ selectedOptions }) {
            const option = selectedOptions[0]
            this.commemorationIndex = option ? option.value : 0
            this.recordForm.commemorationDayId = this.commemorationIndex === 0
                ? null
                : this.commemorationDays[this.commemorationIndex - 1].id
            this.showCommemorationPicker = false
        },
        /**
         * 设置默认选中（最近使用的第一个二级分类 + 默认账户）
         */
        setDefaultSelection() {
            if (this.active === '2') {
                this.setDefaultTransferAccounts()
                return
            }
            if (!this.recordForm.type && this.typeColumns.length > 0) {
                const firstType = this.typeColumns[0]
                this.temporaryForm.primaryType = firstType.id
                if (firstType.children && firstType.children.length > 0) {
                    const recentItem = firstType.children[0]
                    this.recordForm.type = recentItem.id
                    this.temporaryForm.secondaryType = recentItem.id
                    this.temporarySecondaryType = recentItem.id
                } else {
                    this.recordForm.type = firstType.id
                    this.temporaryForm.secondaryType = firstType.id
                }
            }

            if (!this.recordForm.account && this.accountColumns.length > 0) {
                const firstGroup = this.accountColumns[0]
                if (firstGroup.children && firstGroup.children.length > 0) {
                    const firstAccount = firstGroup.children[0]
                    this.temporaryForm.account = firstAccount.accountName
                    this.recordForm.account = firstAccount.id
                    this.temporaryAccountId = firstAccount.id
                } else if (firstGroup.id) {
                    this.temporaryForm.account = firstGroup.accountName
                    this.recordForm.account = firstGroup.id
                    this.temporaryAccountId = firstGroup.id
                }
            } else if (this.recordForm.account && !this.temporaryForm.account) {
                for (const group of this.accountColumns) {
                    if (group.children) {
                        const account = group.children.find(a => a.id === this.recordForm.account)
                        if (account) {
                            this.temporaryForm.account = account.accountName
                            break
                        }
                    } else if (group.id === this.recordForm.account) {
                        this.temporaryForm.account = group.accountName
                        break
                    }
                }
            }
        },
        async getTypeList() {
            if (this.active === '2') {
                this.typeColumns = []
                return
            }
            const response = await getClassificationList({ typeDifference: this.active })
            this.typeColumns = response.data || []
            localStorage.setItem('typeColumns', JSON.stringify(this.typeColumns))
        },
        async getAccountColumns() {
            // 账户不区分收支/转账，转账时取全量账户
            const params = this.active === '2' ? {} : { typeDifference: this.active }
            const response = await getAccountList(params)
            this.accountColumns = response.data || []
            localStorage.setItem('accountColumns', JSON.stringify(this.accountColumns))
        },
        /**
         * 转账默认账户：转出=第一个，转入=第二个（确保不同账户）
         */
        setDefaultTransferAccounts() {
            const allAccounts = []
            for (const group of this.accountColumns) {
                if (group.children && group.children.length) {
                    allAccounts.push(...group.children)
                } else if (group.id) {
                    allAccounts.push(group)
                }
            }
            if (allAccounts.length === 0) return
            if (!this.recordForm.account) {
                const out = allAccounts[0]
                this.recordForm.account = out.id
                this.temporaryForm.account = out.accountName
            }
            if (!this.recordForm.otherAccount) {
                const inAcc = allAccounts.find(a => a.id !== this.recordForm.account) || allAccounts[0]
                this.recordForm.otherAccount = inAcc.id
                this.temporaryForm.otherAccountName = inAcc.accountName
            }
        },
        async loadListsAndSetDefault() {
            await Promise.all([
                this.getTypeList(),
                this.getAccountColumns()
            ])
            this.setDefaultSelection()
        },
        handleTabChange(value) {
            this.active = value
            this.recordForm.typeDifference = value
        },
        /**
         * AI 解析自然语言记账
         */
        async handleAiParse() {
            if (!this.aiInputText.trim()) {
                showToast('请输入记账描述')
                return
            }
            if (this.aiParsing) return
            this.aiParsing = true
            try {
                const res = await aiParse(this.aiInputText)
                const data = res.data
                if (!data || !data.money) {
                    showToast('未能识别，请重新描述')
                    return
                }

                if (data.type !== undefined && data.type !== null) {
                    const newActive = String(data.type)
                    if (newActive !== this.active) {
                        this.active = newActive
                        this.recordForm.typeDifference = newActive
                        await this.loadListsAndSetDefault()
                    }
                }

                this.recordForm.money = parseFloat(data.money) || 0
                this.money = String(this.recordForm.money)

                if (data.typeId) {
                    this.recordForm.type = data.typeId
                    for (const parent of this.typeColumns) {
                        if (parent.id === data.typeId || (parent.children && parent.children.find(c => c.id === data.typeId))) {
                            this.temporaryForm.primaryType = parent.id
                            break
                        }
                    }
                    for (const parent of this.typeColumns) {
                        if (parent.children) {
                            const child = parent.children.find(c => c.id === data.typeId)
                            if (child) {
                                this.temporaryForm.primaryType = parent.id
                                this.temporaryForm.secondaryType = child.id
                                this.temporarySecondaryType = child.id
                                this.recordForm.type = child.id
                                break
                            }
                        }
                    }
                }

                if (data.accountId) {
                    this.recordForm.account = data.accountId
                    this.temporaryAccountId = data.accountId
                    for (const group of this.accountColumns) {
                        if (group.children) {
                            const account = group.children.find(a => a.id === data.accountId)
                            if (account) {
                                this.temporaryForm.account = account.accountName
                                break
                            }
                        } else if (group.id === data.accountId) {
                            this.temporaryForm.account = group.accountName
                            break
                        }
                    }
                } else if (data.account) {
                    this.temporaryForm.account = data.account
                }

                if (data.remark) {
                    this.recordForm.remark = data.remark
                }

                mSuccess('AI识别成功')
            } catch (error) {
                console.error('AI解析失败:', error)
                showToast('解析失败，请重试')
            } finally {
                this.aiParsing = false
            }
        },
        /**
         * 打开二级分类选择器
         */
        openSecondaryPicker() {
            for (const parent of this.typeColumns) {
                if (parent.children) {
                    const child = parent.children.find(c => c.id === this.recordForm.type)
                    if (child) {
                        this.temporaryForm.primaryType = parent.id || parent.typeName
                        this.temporarySecondaryType = child.id
                        this.showSecondaryPopup = true
                        return
                    }
                }
            }
            if (this.typeColumns.length > 0 && this.typeColumns[0].children) {
                this.temporaryForm.primaryType = this.typeColumns[0].id || this.typeColumns[0].typeName
                this.temporarySecondaryType = this.typeColumns[0].children[0].id
                this.showSecondaryPopup = true
            }
        },
        /**
         * 选择一级分类
         */
        handleSelectPrimaryType(category) {
            this.temporaryForm.primaryType = category.id || category.typeName
            this.recordForm.type = this.temporaryForm.primaryType
            this.temporaryForm.secondaryType = ''

            if (category.children && category.children.length > 0) {
                this.temporarySecondaryType = category.children[0].id
                this.showSecondaryPopup = true
            }
        },
        selectSecondaryItem(item) {
            this.temporarySecondaryType = item.id
            this.temporaryForm.secondaryType = item.id
            this.recordForm.type = item.id
            this.showSecondaryPopup = false
        },
        confirmSecondaryType() {
            if (this.temporarySecondaryType) {
                this.temporaryForm.secondaryType = this.temporarySecondaryType
                this.recordForm.type = this.temporarySecondaryType
            }
            this.showSecondaryPopup = false
        },
        /**
         * 打开账户选择弹层（mode: account=普通账户 out=转出 in=转入）
         */
        openAccountPicker(mode) {
            this.accountPickerMode = mode
            this.temporaryAccountId = mode === 'in'
                ? (this.recordForm.otherAccount || '')
                : (this.recordForm.account || '')
            this.showAccountPopup = true
        },
        findAccountById(id) {
            for (const group of this.accountColumns) {
                if (group.children) {
                    const a = group.children.find(x => x.id === id)
                    if (a) return a
                } else if (group.id === id) {
                    return group
                }
            }
            return null
        },
        applyAccountSelection(account) {
            if (!account) return
            if (this.accountPickerMode === 'in') {
                this.temporaryForm.otherAccountName = account.accountName
                this.recordForm.otherAccount = account.id
            } else {
                this.temporaryForm.account = account.accountName
                this.recordForm.account = account.id
            }
        },
        selectAccountItem(account) {
            this.temporaryAccountId = account.id
            this.applyAccountSelection(account)
            this.showAccountPopup = false
        },
        confirmAccount() {
            if (this.temporaryAccountId) {
                this.applyAccountSelection(this.findAccountById(this.temporaryAccountId))
            }
            this.showAccountPopup = false
        },
        /**
         * 计算器输入回调
         */
        handleCalculatorInput(text, result) {
            this.money = text
            this.recordForm.money = parseFloat(result) || 0
        },
        formatAmount(amount) {
            if (amount === 0 || amount === '0') return '0.00'
            const num = parseFloat(amount)
            if (isNaN(num)) return '0.00'
            return num.toFixed(2)
        },
        /**
         * 图片上传
         */
        handleCamera() {
            this.$refs.fileInput.click()
        },
        async onFileChange(e) {
            const files = Array.from(e.target.files || [])
            e.target.value = ''
            if (!files.length) return
            mLoading('上传中...')
            try {
                for (const file of files) {
                    const uploadResult = await this.uploadImage(file)
                    this.imageList.push(uploadResult.url)
                }
                closeToast()
            } catch (error) {
                closeToast()
                mError('上传失败')
            }
        },
        uploadImage(file) {
            const formData = new FormData()
            formData.append('file', file)
            formData.append('fileType', 'bookkeeping')
            return uploadImage(formData)
        },
        handlePreviewImage(index) {
            showImagePreview({
                images: this.imageList,
                startPosition: index,
                loop: true
            })
        },
        /**
         * 删除记录（编辑模式）
         */
        async handleDelete() {
            try {
                await mConfirm('删除提示', '是否删除该条记录？')
            } catch (e) {
                return
            }
            await delRecord(this.recordForm.id)
            mSuccess('删除成功')
            this.$router.back()
        },
        /**
         * 保存记录
         */
        async handleSave() {
            if (this.active === '2') {
                if (!this.recordForm.account) {
                    showToast('请选择转出账户')
                    return
                }
                if (!this.recordForm.otherAccount) {
                    showToast('请选择转入账户')
                    return
                }
                if (this.recordForm.account === this.recordForm.otherAccount) {
                    showToast('转出账户与转入账户不能相同')
                    return
                }
            } else {
                if (!this.recordForm.type) {
                    if (this.temporaryForm.primaryType) {
                        this.recordForm.type = this.temporaryForm.primaryType
                    } else {
                        showToast('请选择分类')
                        return
                    }
                }
                if (!this.recordForm.account) {
                    showToast('请选择账户')
                    return
                }
            }

            if (!this.recordForm.money || this.recordForm.money === 0) {
                showToast('请输入金额')
                return
            }

            mLoading('保存中')

            try {
                this.recordForm.payTime = this.parseTime(this.recordForm.payTime)
                if (!this.recordForm.payTime) {
                    this.recordForm.payTime = this.parseTime(new Date())
                }
                this.recordForm.images = this.imageList.join(',')

                if (this.recordForm.id) {
                    await updateRecord(this.recordForm)
                    mSuccess('修改成功')
                } else {
                    await addRecord(this.recordForm)
                    mSuccess('新增成功')
                }
                this.$router.back()
            } finally {
                closeToast()
            }
        }
    }
}
</script>

<style lang="scss" scoped>
.add-record {
    height: calc(100vh - 46px);
    display: flex;
    flex-direction: column;
    background: var(--pnkx-bg);
    overflow: hidden;

    &__header-row {
        display: flex;
        align-items: center;
        background: var(--pnkx-surface);
        padding: 4px 10px 4px 0;
        flex-shrink: 0;
        border-bottom: 1px solid var(--pnkx-border-soft);
    }

    &__type-tabs {
        display: flex;
        position: relative;
        flex: 1;
        padding: 0 4px;
    }

    &__tab-item {
        flex: 1;
        text-align: center;
        padding: 10px 0;
        font-size: 14px;
        color: var(--pnkx-text-secondary);
        transition: color 0.2s ease;

        &.active {
            color: var(--pnkx-primary);
            font-weight: 600;
        }
    }

    &__tab-indicator {
        position: absolute;
        bottom: 0;
        left: 0;
        width: calc(100% / 3);
        height: 3px;
        border-radius: 2px;
        background: var(--pnkx-primary);
        transition: left 0.2s ease;
    }

    &__ai-toggle {
        display: flex;
        align-items: center;
        gap: 3px;
        padding: 5px 10px;
        border-radius: 999px;
        background: var(--pnkx-primary-soft);
        color: var(--pnkx-primary);
        font-size: 13px;
        transition: all 0.15s ease;

        &:active {
            transform: scale(0.95);
        }

        &.active {
            background: var(--pnkx-primary);
            color: #fff;
        }
    }

    &__ai-icon {
        width: 15px;
        height: 15px;
        object-fit: contain;
    }

    &__delete-btn {
        width: 32px;
        height: 32px;
        margin-left: 8px;
        display: flex;
        align-items: center;
        justify-content: center;
        border-radius: 50%;
        background: var(--pnkx-danger-soft);
        color: var(--pnkx-danger);
        font-size: 16px;

        &:active {
            transform: scale(0.95);
        }
    }

    &__content {
        flex: 1;
        min-height: 0;
        overflow-y: auto;
        display: flex;
        flex-direction: column;
    }

    &__ai-section {
        background: var(--pnkx-surface);
        padding: 8px 12px;
        border-bottom: 1px solid var(--pnkx-border-soft);
    }

    &__ai-row {
        display: flex;
        align-items: center;
        gap: 8px;
    }

    &__ai-input {
        flex: 1;
        height: 34px;
        font-size: 14px;
        color: var(--pnkx-text);
        background: var(--pnkx-surface-muted);
        border: none;
        outline: none;
        border-radius: 999px;
        padding: 0 14px;
    }

    &__ai-parse-btn {
        padding: 6px 16px;
        border-radius: 999px;
        background: var(--pnkx-primary);
        color: #fff;
        font-size: 12px;
        font-weight: 500;
        flex-shrink: 0;

        &:active {
            transform: scale(0.95);
        }

        &.disabled {
            opacity: 0.6;
        }
    }

    &__section {
        background: var(--pnkx-surface);
        margin: 10px 12px 0;
        border: 1px solid var(--pnkx-border-soft);
        border-radius: 14px;
        padding: 12px;

        &:last-child {
            margin-bottom: 10px;
        }
    }

    &__category-grid {
        display: flex;
        flex-wrap: wrap;
        gap: 8px;
        justify-content: center;
    }

    &__category-item {
        display: flex;
        flex-direction: column;
        align-items: center;
        justify-content: center;
        width: calc(25% - 6px);
        padding: 6px 2px;
        border-radius: 10px;
        transition: all 0.15s ease;

        &:active {
            transform: scale(0.95);
        }

        &.selected {
            background: var(--pnkx-primary-soft);

            .add-record__category-label {
                color: var(--pnkx-primary);
                font-weight: 500;
            }
        }
    }

    &__category-icon {
        width: 38px;
        height: 38px;
        border-radius: 50%;
        display: flex;
        align-items: center;
        justify-content: center;
        margin-bottom: 4px;
        color: var(--pnkx-text);
        font-size: 18px;
        overflow: hidden;

        img {
            width: 22px;
            height: 22px;
            object-fit: contain;
        }
    }

    &__category-label {
        font-size: 11px;
        color: var(--pnkx-text);
        text-align: center;
        overflow: hidden;
        text-overflow: ellipsis;
        white-space: nowrap;
        max-width: 100%;
    }

    &__current-category,
    &__account-select {
        display: flex;
        align-items: center;
        gap: 8px;
        padding: 10px 12px;
        border-radius: 10px;
        background: var(--pnkx-surface-muted);
        border: 1px solid var(--pnkx-border-soft);
        margin-bottom: 8px;
        color: var(--pnkx-primary);
        font-size: 15px;

        .van-icon:first-child {
            font-size: 16px;
        }

        &:active {
            background: var(--pnkx-primary-soft);
        }

        .van-icon:last-child {
            color: var(--pnkx-text-muted);
            font-size: 14px;
        }
    }

    &__chip-text {
        flex: 1;
        font-size: 13px;
        color: var(--pnkx-text);
        overflow: hidden;
        text-overflow: ellipsis;
        white-space: nowrap;
    }

    &__amount-display {
        display: flex;
        align-items: baseline;
        padding: 8px 12px 12px;
    }

    &__amount-symbol {
        font-size: 18px;
        color: var(--pnkx-text);
        font-weight: 500;
        margin-right: 4px;
    }

    &__amount-value {
        font-size: 32px;
        color: var(--pnkx-text);
        font-weight: 600;
        line-height: 1.1;
        word-break: break-all;

        &--1 {
            color: var(--pnkx-danger);
        }

        &--0 {
            color: var(--pnkx-success);
        }

        &--2 {
            color: var(--pnkx-primary);
        }
    }

    &__amount-process {
        font-size: 12px;
        color: var(--pnkx-text-muted);
        margin-left: 8px;
    }

    &__note-section {
        padding-top: 10px;
        border-top: 1px solid var(--pnkx-border-soft);
    }

    &__note-input {
        width: 100%;
        height: 34px;
        box-sizing: border-box;
        font-size: 14px;
        color: var(--pnkx-text);
        background: var(--pnkx-surface-muted);
        border: none;
        outline: none;
        border-radius: 10px;
        padding: 0 12px;
        margin-bottom: 8px;
    }

    &__image-attachments {
        display: flex;
        gap: 8px;
        align-items: center;
    }

    &__attachment-btn {
        width: 34px;
        height: 34px;
        border-radius: 10px;
        background: var(--pnkx-surface-muted);
        display: flex;
        align-items: center;
        justify-content: center;
        color: var(--pnkx-text-secondary);
        font-size: 16px;

        &:active {
            transform: scale(0.95);
        }
    }

    &__image-preview {
        display: flex;
        gap: 8px;
        overflow-x: auto;
    }

    &__preview-image {
        width: 34px;
        height: 34px;
        border-radius: 10px;
        object-fit: cover;
        flex-shrink: 0;
    }

    &__calculator {
        flex-shrink: 0;
        padding: 6px 12px calc(8px + env(safe-area-inset-bottom));
        background: var(--pnkx-surface);
        border-top: 1px solid var(--pnkx-border-soft);
    }

    &__picker {
        background: var(--pnkx-surface);
        border-radius: 16px 16px 0 0;
        overflow: hidden;
        padding-bottom: env(safe-area-inset-bottom);
    }

    &__picker-header {
        display: flex;
        align-items: center;
        justify-content: space-between;
        padding: 12px 16px;
        border-bottom: 1px solid var(--pnkx-border-soft);
    }

    &__picker-cancel {
        font-size: 14px;
        color: var(--pnkx-text-secondary);
        padding: 2px 8px;
    }

    &__picker-title {
        font-size: 15px;
        font-weight: 600;
        color: var(--pnkx-text);
    }

    &__picker-confirm {
        font-size: 14px;
        color: var(--pnkx-primary);
        padding: 2px 8px;
    }

    &__picker-content {
        max-height: 340px;
        overflow-y: auto;
    }

    &__account-group {
        .add-record__group-label {
            padding: 10px 16px 4px;
            font-size: 12px;
            color: var(--pnkx-text-muted);
        }
    }

    &__account-item {
        display: flex;
        align-items: center;
        gap: 8px;
        padding: 12px 16px;
        border-bottom: 1px solid var(--pnkx-border-soft);
        color: var(--pnkx-primary);

        &:last-child {
            border-bottom: none;
        }

        &.selected {
            background: var(--pnkx-primary-soft);
        }

        &:active {
            background: var(--pnkx-surface-muted);
        }
    }

    &__account-item-name {
        flex: 1;
        font-size: 14px;
        color: var(--pnkx-text);
    }

    &__account-item-amount {
        font-size: 13px;
        color: var(--pnkx-text-secondary);
    }

    &__picker-item {
        display: flex;
        align-items: center;
        justify-content: space-between;
        padding: 13px 16px;
        font-size: 14px;
        color: var(--pnkx-text);
        border-bottom: 1px solid var(--pnkx-border-soft);

        &:last-child {
            border-bottom: none;
        }

        &.selected {
            color: var(--pnkx-primary);
            background: var(--pnkx-primary-soft);
        }

        &:active {
            background: var(--pnkx-surface-muted);
        }
    }
}
</style>
