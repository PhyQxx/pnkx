<!--
 * @File: add
 * @Author: PHY
 * @Date: 2021/11/19 16:58
 * @Description: 新增记录
-->
<template>
  <view class="add-record">
    <!-- 顶部操作栏 -->
    <view class="header-row">
      <!-- 记账类型标签栏 -->
      <view class="type-tabs">
        <view
          v-for="tab in typeTabs"
          :key="tab.value"
          class="tab-item"
          :class="{ active: active === tab.value }"
          @click="handleTabChange(tab.value)"
        >
          {{ tab.label }}
        </view>
        <view class="tab-indicator" :style="indicatorStyle"></view>
      </view>

      <!-- AI记账按钮 -->
      <view class="ai-toggle" :class="{ active: showAiInput }" @click="showAiInput = !showAiInput">
        <svg-icon icon-class="ai" size="16px"/>
        <text class="ai-toggle-text">AI</text>
      </view>

      <!-- 删除按钮（编辑模式显示） -->
      <view v-if="recordForm.id" class="delete-btn" @click="handleDelete">
        <uni-icons type="trash" size="18" color="#FF6B6B"/>
      </view>
    </view>

    <!-- 中间内容区域（可滚动） -->
    <view class="content-area">
      <!-- AI记账输入区域 -->
      <view class="ai-input-section" v-if="showAiInput">
        <view class="ai-input-row">
          <input v-model="aiInputText" placeholder="说点什么，如：午餐花了25元" class="ai-input" confirm-type="done" @confirm="handleAiParse" />
          <view class="ai-parse-btn" :class="{ disabled: aiParsing }" @click="handleAiParse">
            <text>{{ aiParsing ? "解析中..." : "解析" }}</text>
          </view>
        </view>
      </view>

      <!-- 分类图标区域 -->
      <view class="category-section">
        <view class="category-grid">
          <view
            v-for="(category, index) in currentCategoryList"
            :key="category.id"
            class="category-item"
            :class="{ selected: temporaryForm.primaryType === category.id || temporaryForm.primaryType === category.typeName }"
            @click="handleSelectPrimaryType(category)"
          >
            <view class="category-icon" :style="{ backgroundColor: getCategoryColor(index) }">
              <svg-icon :icon-class="category.typeIcon" size="32rpx"/>
            </view>
            <view class="category-label">{{ category.typeName }}</view>
            <view v-if="category.children && category.children.length > 0"
                  class="category-arrow"></view>
          </view>
        </view>
      </view>

      <!-- 金额和备注区域 -->
      <view class="input-section">
        <!-- 当前分类显示 -->
        <view class="current-category" @click="openSecondaryPicker">
          <svg-icon icon-class="type-icon" size="14px"/>
          <text class="category-text">{{ currentCategoryName || '选择分类' }}</text>
          <uni-icons type="arrowright" size="12"/>
        </view>

        <!-- 金额显示 -->
        <view class="amount-display">
          <text class="amount-symbol">¥</text>
          <text class="amount-value" :class="{ 'amount--expense': active === '1', 'amount--income': active === '0' }">{{ formatAmount(recordForm.money) }}</text>
          <text class="amount-process">{{ money }}</text>
        </view>

        <!-- 账户选择 -->
        <view class="account-select" @click="$refs.accountPopup.open()">
          <view class="account-icon">
            <svg-icon icon-class="银行卡" size="14px"/>
          </view>
          <view class="account-label">{{ temporaryForm.account || '选择账户' }}</view>
          <view class="account-arrow">
            <uni-icons type="arrowright" size="12"/>
          </view>
        </view>

        <!-- 备注输入 -->
        <view class="note-section">
          <input
            v-model="recordForm.remark"
            class="note-input"
            placeholder="点击填写备注信息"
            maxlength="200"
          />

          <!-- 图片附件按钮 -->
          <view class="image-attachments">
            <view class="attachment-btn" @click="handleCamera">
              <uni-icons type="camera" size="14"/>
            </view>
            <view v-if="imageList.length > 0" class="image-preview">
              <image
                v-for="(img, index) in imageList"
                :key="index"
                :src="img"
                class="preview-image"
                mode="aspectFill"
                @click="handlePreviewImage(index)"
              />
            </view>
          </view>
        </view>
      </view>
    </view>

    <!-- 数字键盘 -->
    <Calculator class="calculator" @getInputValue="handleCalculatorInput" @confirm="handleSave"/>

    <!-- 账户选择弹窗 -->
    <uni-popup ref="accountPopup" type="bottom">
      <view class="account-picker">
        <view class="picker-header">
          <view class="picker-cancel" @click="closePopup('accountPopup')">取消</view>
          <view class="picker-title">选择账户</view>
          <view class="picker-confirm" @click="confirmAccount">确定</view>
        </view>
        <view class="picker-content">
          <view
            v-for="(group, gIndex) in accountColumns"
            :key="gIndex"
            class="account-group"
          >
            <view class="group-label">{{ group.accountName || group.typeName }}</view>
            <view class="group-items">
              <view
                v-for="(account, aIndex) in group.children || []"
                :key="account.id"
                class="account-item"
                :class="{ selected: temporaryAccountId === account.id }"
                @click="selectAccountItem(account)"
              >
                <view class="account-item-name">{{ account.accountName }}</view>
                <view class="account-item-amount">¥{{ account.balance || 0 }}</view>
                <uni-icons v-if="temporaryAccountId === account.id" type="checkmarkempty" size="18"
                           color="#4A7ADB"/>
              </view>
            </view>
          </view>
        </view>
      </view>
    </uni-popup>

    <!-- 二级分类选择弹窗 -->
    <uni-popup ref="secondaryPopup" type="bottom">
      <view class="secondary-picker">
        <view class="picker-header">
          <view class="picker-cancel" @click="closePopup('secondaryPopup')">取消</view>
          <view class="picker-title">选择分类</view>
          <view class="picker-confirm" @click="confirmSecondaryType">确定</view>
        </view>
        <view class="picker-content">
          <view
            v-for="item in currentSecondaryTypeList"
            :key="item.id"
            class="picker-item"
            :class="{ selected: temporarySecondaryType === item.id }"
            @click="selectSecondaryItem(item)"
          >
            {{ item.typeName }}
            <uni-icons v-if="temporarySecondaryType === item.id" type="checkmarkempty" size="18"
                       color="#4A7ADB"/>
          </view>
        </view>
      </view>
    </uni-popup>
  </view>
</template>

<script>
import {getClassificationList} from "@/api/px/life/bookkeeping/classification";
import {getAccountList} from "@/api/px/life/bookkeeping/account";
import {addRecord, aiParse, delRecord, getRecord, updateRecord} from "@/api/px/life/bookkeeping/record";
import upload from "@/utils/upload";
import Calculator from "@/pages_life/components/Calculator/index.vue";
import uniPopup from '@/uni_modules/uni-popup/components/uni-popup/uni-popup.vue';

export default {
  name: "addRecord",
  components: {
    Calculator,
    uniPopup
  },
  data() {
    return {
      // 记账类型标签（不含转账）
      typeTabs: [
        {label: '支出', value: '1'},
        {label: '收入', value: '0'},
      ],
      // 当前选择的记账类型
      active: '1',
      // 暂时计算
      money: '',
      // 暂时表单
      temporaryForm: {
        primaryType: '',
        secondaryType: '',
        account: '',
      },
      // 记录表单
      recordForm: {
        id: null,
        money: 0,
        payTime: new Date(),
        remark: '',
        type: '',
        account: '',
        typeDifference: '1',
      },
      // 类型列表
      typeColumns: [],
      // 账户列表
      accountColumns: [],
      // 图片列表
      imageList: [],
      // 临时选中的账户ID
      temporaryAccountId: '',
      // 临时选中的二级分类
      temporarySecondaryType: '',
      // 分类图标背景颜色
      categoryColors: ['#6C9EFF', '#FF6B6B', '#4ADE80', '#FBBF24', '#A78BFA', '#F472B6', '#34D399', '#60A5FA'],
      // 是否正在初始化
      isInitializing: false,
      // AI记账相关
      showAiInput: false,
      aiInputText: '',
      aiParsing: false,
    }
  },
  computed: {
    // 当前分类列表
    currentCategoryList() {
      return this.typeColumns;
    },
    // 当前二级分类
    currentSecondaryTypeList() {
      if (this.temporaryForm.primaryType) {
        const parent = this.typeColumns.find(
          item => item.id === this.temporaryForm.primaryType || item.typeName === this.temporaryForm.primaryType
        );
        return parent ? parent.children : [];
      }
      return [];
    },
    // 当前选中的二级分类名称
    currentCategoryName() {
      // 优先从 typeColumns 中查找（包含最近使用的子分类）
      for (const parent of this.typeColumns) {
        if (parent.children) {
          const child = parent.children.find(c => c.id === this.recordForm.type);
          if (child) {
            return child.typeName;
          }
        }
      }
      // 如果没找到，尝试从 recordForm 中的 typeObject 获取（编辑模式）
      if (this.recordForm.typeObject) {
        return this.recordForm.typeObject.typeName;
      }
      // 如果还是没找到，检查是否是一级分类
      const primary = this.typeColumns.find(item => item.id === this.recordForm.type);
      if (primary) {
        return primary.typeName;
      }
      return '';
    },
    // 标签指示器样式
    indicatorStyle() {
      const index = this.typeTabs.findIndex(tab => tab.value === this.active);
      return `left: ${index * 50}%;`;
    }
  },
  onLoad(option) {
    // 获取编辑记录 ID
    if (option && option.recordId) {
      this.recordForm.id = option.recordId;
    }
    this.initData();
  },
  watch: {
    /**
     * 监听当前记账类型
     */
    active(newAction) {
      if (this.isInitializing) return;
      this.temporaryForm.primaryType = '';
      this.temporaryForm.secondaryType = '';
      this.recordForm.type = '';
      this.recordForm.typeDifference = newAction;
      this.loadListsAndSetDefault();
    },
    'recordForm.payTime': {
      handler(newTime) {
        // 时间格式化在保存时处理
      },
      immediate: true
    }
  },
  methods: {
    /**
     * 获取分类图标背景颜色
     */
    getCategoryColor(index) {
      return this.categoryColors[index % this.categoryColors.length]
    },
    /**
     * 初始化数据
     */
    async initData() {
      this.isInitializing = true;
      try {
        // 获取记录信息（编辑模式）
        if (this.recordForm.id) {
          const res = await getRecord(this.recordForm.id);
          this.active = res.data.typeObject.typeDifference;
          this.recordForm = {
            ...res.data,
            payTime: new Date(res.data.payTime || new Date()),
          };
          this.money = String(this.recordForm.money || 0);
          this.recordForm.typeDifference = this.active;
          // 设置图片列表
          this.imageList = res.data.images ? res.data.images.split(',').filter(item => item) : [];
          // 设置分类选中
          if (res.data.typeObject) {
            this.temporaryForm.primaryType = res.data.typeObject.typeParentId || res.data.typeObject.id;
            this.temporaryForm.secondaryType = res.data.typeObject.id;
            this.temporarySecondaryType = res.data.typeObject.id;
          }
          // 设置账户
          if (res.data.account) {
            this.temporaryForm.account = res.data.accountObject ? res.data.accountObject.accountName : res.data.accountName;
            this.recordForm.account = res.data.account;
            this.temporaryAccountId = res.data.account;
          }
          // 加载列表数据
          await this.loadListsAndSetDefault();
        } else {
          // 新增模式，加载列表并设置默认选中
          await this.loadListsAndSetDefault();
        }
      } catch (error) {
        console.error('初始化数据失败:', error);
      } finally {
        this.isInitializing = false;
      }
    },
    /**
     * 设置默认选中
     */
    setDefaultSelection() {
      // 设置默认分类为最近使用的第一个二级类型
      if (!this.recordForm.type && this.typeColumns.length > 0) {
        const firstType = this.typeColumns[0];
        this.temporaryForm.primaryType = firstType.id;
        if (firstType.children && firstType.children.length > 0) {
          const recentItem = firstType.children[0];
          this.recordForm.type = recentItem.id;
          this.temporaryForm.secondaryType = recentItem.id;
          this.temporarySecondaryType = recentItem.id;
        } else {
          this.recordForm.type = firstType.id;
          this.temporaryForm.secondaryType = firstType.id;
        }
      }

      // 设置默认账户
      if (!this.recordForm.account && this.accountColumns.length > 0) {
        const firstGroup = this.accountColumns[0];
        if (firstGroup.children && firstGroup.children.length > 0) {
          const firstAccount = firstGroup.children[0];
          this.temporaryForm.account = firstAccount.accountName;
          this.recordForm.account = firstAccount.id;
          this.temporaryAccountId = firstAccount.id;
        } else if (firstGroup.id) {
          this.temporaryForm.account = firstGroup.accountName;
          this.recordForm.account = firstGroup.id;
          this.temporaryAccountId = firstGroup.id;
        }
      } else if (this.recordForm.account && !this.temporaryForm.account) {
        // 如果有账户ID但没有账户名称，从列表中查找
        for (const group of this.accountColumns) {
          if (group.children) {
            const account = group.children.find(a => a.id === this.recordForm.account);
            if (account) {
              this.temporaryForm.account = account.accountName;
              break;
            }
          } else if (group.id === this.recordForm.account) {
            this.temporaryForm.account = group.accountName;
            break;
          }
        }
      }
    },
    /**
     * 获取分类列表
     */
    async getTypeList() {
      const response = await getClassificationList({typeDifference: this.active});
      this.typeColumns = response.data || [];
      uni.setStorage({
        key: 'typeColumns',
        data: this.typeColumns
      });
    },
    /**
     * 获取账户列表
     */
    async getAccountList() {
      const response = await getAccountList({typeDifference: this.active});
      this.accountColumns = response.data || [];
      uni.setStorage({
        key: 'accountColumns',
        data: this.accountColumns
      });
    },
    /**
     * 获取列表数据并设置默认选中
     */
    async loadListsAndSetDefault() {
      await Promise.all([
        this.getTypeList(),
        this.getAccountList()
      ]);
      this.setDefaultSelection();
    },
    /**
     * 切换标签
     */
    handleTabChange(value) {
      this.active = value;
      this.recordForm.typeDifference = value;
    },
    /**
     * AI解析自然语言记账
     */
    async handleAiParse() {
      if (!this.aiInputText.trim()) {
        uni.showToast({title: "请输入记账描述", icon: "none"});
        return;
      }
      if (this.aiParsing) return;
      this.aiParsing = true;
      try {
        const res = await aiParse(this.aiInputText);
        const data = res.data;
        if (!data || !data.money) {
          uni.showToast({title: "未能识别，请重新描述", icon: "none"});
          return;
        }

        // 设置类型（支出/收入）
        if (data.type !== undefined && data.type !== null) {
          const newActive = String(data.type);
          if (newActive !== this.active) {
            this.active = newActive;
            this.recordForm.typeDifference = newActive;
            // 切换类型后需要重新加载分类和账户列表
            await this.loadListsAndSetDefault();
          }
        }

        // 设置金额
        this.recordForm.money = parseFloat(data.money) || 0;
        this.money = String(this.recordForm.money);

        // 设置分类
        if (data.typeId) {
          this.recordForm.type = data.typeId;
          // 查找并设置一级分类
          for (const parent of this.typeColumns) {
            if (parent.id === data.typeId || (parent.children && parent.children.find(c => c.id === data.typeId))) {
              this.temporaryForm.primaryType = parent.id;
              break;
            }
          }
          // 检查是否是二级分类
          for (const parent of this.typeColumns) {
            if (parent.children) {
              const child = parent.children.find(c => c.id === data.typeId);
              if (child) {
                this.temporaryForm.primaryType = parent.id;
                this.temporaryForm.secondaryType = child.id;
                this.temporarySecondaryType = child.id;
                this.recordForm.type = child.id;
                break;
              }
            }
          }
        }

        // 设置账户
        if (data.accountId) {
          this.recordForm.account = data.accountId;
          this.temporaryAccountId = data.accountId;
          for (const group of this.accountColumns) {
            if (group.children) {
              const account = group.children.find(a => a.id === data.accountId);
              if (account) {
                this.temporaryForm.account = account.accountName;
                break;
              }
            } else if (group.id === data.accountId) {
              this.temporaryForm.account = group.accountName;
              break;
            }
          }
        } else if (data.account) {
          this.temporaryForm.account = data.account;
        }

        // 设置备注
        if (data.remark) {
          this.recordForm.remark = data.remark;
        }

        uni.showToast({title: "AI识别成功", icon: "success"});
      } catch (error) {
        console.error("AI解析失败:", error);
        uni.showToast({title: "解析失败，请重试", icon: "none"});
      } finally {
        this.aiParsing = false;
      }
    },
    /**
     * 打开二级分类选择器
     */
    openSecondaryPicker() {
      // 找到包含当前选中二级分类的父分类
      for (const parent of this.typeColumns) {
        if (parent.children) {
          const child = parent.children.find(c => c.id === this.recordForm.type);
          if (child) {
            this.temporaryForm.primaryType = parent.id || parent.typeName;
            this.temporarySecondaryType = child.id;
            this.$refs.secondaryPopup.open();
            return;
          }
        }
      }
      // 如果没找到，使用第一个有子分类的分类
      if (this.typeColumns.length > 0 && this.typeColumns[0].children) {
        this.temporaryForm.primaryType = this.typeColumns[0].id || this.typeColumns[0].typeName;
        this.temporarySecondaryType = this.typeColumns[0].children[0].id;
        this.$refs.secondaryPopup.open();
      }
    },
    /**
     * 选择一级分类
     */
    handleSelectPrimaryType(category) {
      this.temporaryForm.primaryType = category.id || category.typeName;
      this.recordForm.type = this.temporaryForm.primaryType;
      // 选择后重置二级分类
      this.temporaryForm.secondaryType = '';

      // 如果有二级分类，打开二级分类选择器
      if (category.children && category.children.length > 0) {
        this.temporarySecondaryType = category.children[0].id;
        this.$refs.secondaryPopup.open();
      }
    },
    /**
     * 关闭弹窗（带兜底逻辑，解决正式包弹窗不关闭的问题）
     */
    closePopup(refName) {
      const popup = this.$refs[refName];
      if (!popup) return;
      popup.close();
      // 兜底：动画完成后强制关闭，防止 animation 为 null 时 close 被跳过
      setTimeout(() => {
        if (popup.showPopup) {
          popup.showPopup = false;
          popup.showTrans = false;
        }
      }, 350);
    },
    /**
     * 选择二级分类项
     */
    selectSecondaryItem(item) {
      this.temporarySecondaryType = item.id;
      this.temporaryForm.secondaryType = item.id;
      this.recordForm.type = item.id;
      this.closePopup('secondaryPopup');
    },
    /**
     * 确认二级分类选择
     */
    confirmSecondaryType() {
      if (this.temporarySecondaryType) {
        this.temporaryForm.secondaryType = this.temporarySecondaryType;
        this.recordForm.type = this.temporarySecondaryType;
      }
      this.closePopup('secondaryPopup');
    },
    /**
     * 选择二级分类
     */
    handleSelectSecondaryType(category) {
      this.temporaryForm.secondaryType = category.id;
      this.recordForm.type = category.id;
    },
    /**
     * 选择账户项
     */
    selectAccountItem(account) {
      this.temporaryAccountId = account.id;
      this.temporaryForm.account = account.accountName;
      this.recordForm.account = account.id;
      this.closePopup('accountPopup');
    },
    /**
     * 确认账户选择
     */
    confirmAccount() {
      if (this.temporaryAccountId) {
        // 查找选中的账户
        for (const group of this.accountColumns) {
          if (group.children) {
            const account = group.children.find(a => a.id === this.temporaryAccountId);
            if (account) {
              this.temporaryForm.account = account.accountName;
              this.recordForm.account = account.id;
              break;
            }
          }
        }
      }
      this.closePopup('accountPopup');
    },

    /**
     * 获取计算器的内容
     */
    handleCalculatorInput(text, result) {
      this.money = text;
      this.recordForm.money = parseFloat(result) || 0;
    },
    /**
     * 格式化金额显示
     */
    formatAmount(amount) {
      if (amount === 0 || amount === '0') return '0.00';
      const num = parseFloat(amount);
      if (isNaN(num)) return '0.00';
      return num.toFixed(2);
    },
    /**
     * 相机拍照
     */
    handleCamera() {
      uni.chooseImage({
        count: 9,
        sourceType: ['camera', 'album'],
        success: async (res) => {
          uni.showLoading({title: '上传中...'});
          try {
            for (let tempFilePath of res.tempFilePaths) {
              const uploadResult = await upload({url: '/common/upload', filePath: tempFilePath, params: {fileType: 'bookkeeping'}})
              this.imageList.push(uploadResult.url);
            }
            uni.hideLoading();
          } catch (error) {
            uni.hideLoading();
            uni.showToast({title: '上传失败', icon: 'none'});
          }
        }
      });
    },
    /**
     * 预览图片
     */
    handlePreviewImage(index) {
      uni.previewImage({
        urls: this.imageList,
        current: index,
        indicator: 'number',
        loop: true
      });
    },
    /**
     * 删除记录
     */
    handleDelete() {
      uni.showModal({
        title: '删除提示',
        content: '是否删除该条记录？',
        confirmText: '删除',
        confirmColor: '#FF6B6B',
        success: async (res) => {
          if (res.confirm) {
            await delRecord(this.recordForm.id);
            uni.showToast({title: '删除成功', icon: 'success'});
            uni.navigateBack();
          }
        }
      });
    },
    /**
     * 保存记录
     */
    async handleSave() {
      // 验证分类
      if (!this.recordForm.type) {
        if (this.temporaryForm.primaryType) {
          this.recordForm.type = this.temporaryForm.primaryType;
        } else {
          uni.showToast({title: '请选择分类', icon: 'none'});
          return;
        }
      }

      // 验证账户
      if (!this.recordForm.account && this.active !== '2') {
        uni.showToast({title: '请选择账户', icon: 'none'});
        return;
      }

      // 验证金额
      if (!this.recordForm.money || this.recordForm.money === 0) {
        uni.showToast({title: '请输入金额', icon: 'none'});
        return;
      }

      uni.showLoading({title: '保存中'})

      try {
        // 格式化支付时间
        this.recordForm.payTime = this.$parseTime(this.recordForm.payTime);
        if (!this.recordForm.payTime) {
          this.recordForm.payTime = this.$parseTime(new Date());
        }

        // 处理图片
        this.recordForm.images = this.imageList.join(',');

        // 保存记录
        if (this.recordForm.id) {
          await updateRecord(this.recordForm);
          uni.showToast({title: '修改成功', icon: 'success'});
        } else {
          await addRecord(this.recordForm);
          uni.showToast({title: '新增成功', icon: 'success'});
        }
        uni.navigateBack();
      } finally {
        uni.hideLoading();
      }
    },
  },
}
</script>

<style lang="scss" scoped>
uni-page-body {
  height: 100%;
}

.add-record {
  width: 100%;
  height: 100%;
  background-color: $bg-page;
  display: flex;
  flex-direction: column;
  overflow: hidden;

  .content-area {
    flex: 1;
    display: flex;
    flex-direction: column;
    overflow: hidden;
  }

  .calculator {
    padding: $section-gap;
  }

  .header-row {
    display: flex;
    align-items: center;
    justify-content: space-between;
    background-color: $bg-card;
    padding: 0 $spacing-sm 0 0;
    flex-shrink: 0;
  }

  .ai-toggle {
    display: flex;
    align-items: center;
    justify-content: center;
    gap: $spacing-2xs;
    padding: $spacing-xs $spacing-sm;
    border-radius: $radius-full;
    background-color: rgba($primary, 0.08);
    transition: all $duration-fast $ease-default;

    .ai-toggle-text {
      font-size: $font-caption;
      color: $primary;
      font-weight: $font-weight-medium;
    }

    &.active {
      background-color: $primary;

      .ai-toggle-text {
        color: $text-inverse;
      }
    }

    &:active {
      transform: scale(0.95);
    }
  }

  .ai-input-section {
    background-color: $bg-card;
    padding: $spacing-sm $spacing-md;
    border-bottom: 1rpx solid $border-light;

    .ai-input-row {
      display: flex;
      align-items: center;
      gap: $spacing-sm;
    }

    .ai-input {
      flex: 1;
      height: 64rpx;
      font-size: $font-body;
      color: $text-primary;
      background-color: $gray-50;
      border-radius: $radius-full;
      padding: 0 $spacing-md;
    }

    .ai-parse-btn {
      padding: $spacing-xs $spacing-lg;
      border-radius: $radius-full;
      background: linear-gradient(135deg, $primary 0%, $primary-dark 100%);
      color: $text-inverse;
      font-size: $font-caption;
      font-weight: $font-weight-medium;
      transition: all $duration-fast $ease-default;
      flex-shrink: 0;

      &:active {
        transform: scale(0.95);
      }

      &.disabled {
        opacity: 0.6;
      }
    }
  }

  .delete-btn {
    width: 72rpx;
    height: 72rpx;
    display: flex;
    align-items: center;
    justify-content: center;
    border-radius: $radius-full;
    background-color: rgba($danger, 0.1);
    transition: all $duration-fast $ease-default;

    &:active {
      background-color: rgba($danger, 0.2);
      transform: scale(0.95);
    }
  }

  .type-tabs {
    display: flex;
    background-color: $bg-card;
    padding: 0 $spacing-sm;
    position: relative;
    flex: 1;

    .tab-item {
      flex: 1;
      text-align: center;
      padding: $spacing-sm 0;
      font-size: $font-body;
      color: $text-secondary;
      position: relative;
      transition: color $duration-normal $ease-default;

      &.active {
        color: $primary-dark;
        font-weight: $font-weight-medium;
      }
    }

    .tab-indicator {
      position: absolute;
      bottom: 0;
      left: 0;
      width: 50%;
      height: $spacing-2xs;
      background-color: $primary;
      transition: left $duration-normal $ease-default;
    }
  }

  .category-section {
    background-color: $bg-card;
    margin-top: 1rpx solid $border-light;
    padding: $spacing-sm $spacing-md;
    flex-shrink: 0;

    .category-grid {
      display: flex;
      flex-wrap: wrap;
      gap: $spacing-sm;
      justify-content: center;
    }

    .category-item {
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
      width: calc(25% - 8rpx);
      padding: $spacing-sm $spacing-xs;
      border-radius: $radius-md;
      background-color: $gray-50;
      position: relative;
      transition: all $duration-normal $ease-spring;

      &:active {
        transform: scale(0.95);
      }

      &.selected {
        background-color: $bookkeeping-light;

        .category-label {
          color: $primary-dark;
          font-weight: $font-weight-medium;
        }
      }
    }

    .category-icon {
      width: 80rpx;
      height: 80rpx;
      border-radius: $radius-full;
      display: flex;
      align-items: center;
      justify-content: center;
      margin-bottom: $spacing-2xs;
      box-shadow: $shadow-xs;
    }

    .category-label {
      font-size: $font-small;
      color: $text-primary;
      text-align: center;
      transition: color $duration-fast $ease-default;
    }

    .category-arrow {
      position: absolute;
      top: $spacing-2xs;
      right: $spacing-2xs;
      width: $spacing-md;
      height: $spacing-md;
      border-radius: $radius-full;
      background-color: $gray-300;
      display: flex;
      align-items: center;
      justify-content: center;

      &::after {
        content: '';
        width: 0;
        height: 0;
        border-left: $spacing-2xs solid transparent;
        border-right: $spacing-2xs solid transparent;
        border-top: $spacing-xs solid $bg-card;
        margin-top: $spacing-2xs;
      }
    }
  }

  .sub-category-section {
    background-color: $bg-card;
    margin-top: 1rpx solid $border-light;
    padding: $spacing-sm;

    .sub-category-grid {
      display: flex;
      flex-wrap: wrap;
      gap: $spacing-xs;
    }

    .sub-category-item {
      padding: $spacing-xs $spacing-sm;
      border-radius: $radius-xl;
      background-color: $gray-50;
      font-size: $font-body;
      color: $text-primary;
      transition: all $duration-fast $ease-default;

      &.selected {
        background-color: $bookkeeping-light;
        color: $primary-dark;
        font-weight: $font-weight-medium;
      }
    }

    .sub-category-label {
      padding: 0;
    }
  }

  .input-section {
    background-color: $bg-card;
    margin-top: 1rpx solid $border-light;
    padding: $spacing-sm $spacing-md;
    flex: 1;
    display: flex;
    flex-direction: column;
    min-height: 0;

    .current-category {
      display: flex;
      align-items: center;
      padding: $spacing-sm $spacing-md;
      border-radius: $radius-md;
      background-color: $gray-50;
      margin-bottom: $spacing-sm;
      transition: all $duration-fast $ease-default;

      &:active {
        background-color: $bookkeeping-light;
      }

      .category-text {
        flex: 1;
        margin-left: $spacing-sm;
        font-size: $font-body;
        color: $text-primary;
      }

      .arrow {
        color: $text-tertiary;
      }
    }

    .amount-display {
      display: flex;
      align-items: baseline;
      padding: $spacing-sm $spacing-md;

      .amount-symbol {
        font-size: $font-h2;
        color: $text-primary;
        font-weight: $font-weight-medium;
      }

      .amount-process {
        font-size: $font-caption;
        color: $text-tertiary;
        margin-left: $spacing-xs;
      }

      .amount-value {
        font-size: $font-display;
        color: $text-primary;
        font-weight: $font-weight-semibold;
        margin-left: $spacing-xs;
      }

      .amount--expense {
        color: #FF6B6B;
      }

      .amount--income {
        color: #22C55E;
      }
    }

    .account-select {
      display: flex;
      align-items: center;
      padding: $spacing-sm $spacing-md;
      border-radius: $radius-md;
      background-color: $gray-50;
      margin-bottom: $spacing-sm;
      transition: all $duration-fast $ease-default;

      &:active {
        background-color: $bookkeeping-light;
      }

      .account-icon {
        margin-right: $spacing-sm;
      }

      .account-label {
        flex: 1;
        font-size: $font-body;
        color: $text-primary;
      }

      .account-arrow {
        color: $text-tertiary;
      }
    }

    .note-section {
      padding-top: $spacing-sm;
      border-top: 1rpx solid $border-light;
      margin-bottom: $spacing-2xs;

      .note-input {
        width: 100%;
        height: 60rpx;
        font-size: $font-body;
        color: $text-primary;
        background-color: $gray-50;
        border-radius: $radius-md;
        padding: 0 $spacing-md;
        margin-bottom: $spacing-sm;
      }

      .image-attachments {
        display: flex;
        gap: $spacing-sm;

        .attachment-btn {
          width: 60rpx;
          height: 60rpx;
          border-radius: $radius-md;
          background-color: $gray-50;
          display: flex;
          align-items: center;
          justify-content: center;
          color: $text-secondary;
          transition: all $duration-fast $ease-default;

          &:active {
            transform: scale(0.95);
          }
        }

        .image-preview {
          display: flex;
          gap: $spacing-sm;

          .preview-image {
            width: 80rpx;
            height: 80rpx;
            border-radius: $radius-md;
          }
        }
      }
    }
  }

  .account-picker {
    background-color: $bg-card;
    border-radius: $radius-2xl $radius-2xl 0 0;
    overflow: hidden;

    .picker-header {
      display: flex;
      align-items: center;
      justify-content: space-between;
      padding: $spacing-sm;
      border-bottom: 1rpx solid $border-light;

      .picker-cancel {
        font-size: $font-body;
        color: $text-secondary;
        padding: $spacing-xs $spacing-sm;
      }

      .picker-title {
        font-size: $font-body;
        font-weight: $font-weight-medium;
        color: $text-primary;
      }

      .picker-confirm {
        font-size: $font-body;
        color: $primary;
        padding: $spacing-xs $spacing-sm;
      }
    }

    .picker-content {
      max-height: 320px;
      overflow-y: auto;

      .account-group {
        .group-label {
          padding: $spacing-sm $spacing-lg $spacing-xs;
          font-size: $font-caption;
          color: $text-tertiary;
        }

        .group-items {
          background-color: $bg-card;
        }

        .account-item {
          display: flex;
          align-items: center;
          justify-content: space-between;
          padding: $spacing-md $spacing-lg;
          border-bottom: 1rpx solid $border-light;
          transition: background-color $duration-fast $ease-default;

          &:last-child {
            border-bottom: none;
          }

          &.selected {
            background-color: $bookkeeping-light;
          }

          &:active {
            background-color: $gray-50;
          }

          .account-item-name {
            font-size: $font-body;
            color: $text-primary;
          }

          .account-item-amount {
            font-size: $font-body;
            color: $text-secondary;
            margin-right: $spacing-xs;
          }
        }
      }
    }
  }

  .secondary-picker {
    background-color: $bg-card;
    border-radius: $radius-2xl $radius-2xl 0 0;
    overflow: hidden;

    .picker-header {
      display: flex;
      align-items: center;
      justify-content: space-between;
      padding: $spacing-sm;
      border-bottom: 1rpx solid $border-light;

      .picker-cancel {
        font-size: $font-body;
        color: $text-secondary;
        padding: $spacing-xs $spacing-sm;
      }

      .picker-title {
        font-size: $font-body;
        font-weight: $font-weight-medium;
        color: $text-primary;
      }

      .picker-confirm {
        font-size: $font-body;
        color: $primary;
        padding: $spacing-xs $spacing-sm;
      }
    }

    .picker-content {
      max-height: 320px;
      overflow-y: auto;

      .picker-item {
        display: flex;
        align-items: center;
        justify-content: space-between;
        padding: $spacing-md $spacing-lg;
        font-size: $font-body;
        color: $text-primary;
        transition: background-color $duration-fast $ease-default;

        &.selected {
          color: $primary-dark;
          background-color: $bookkeeping-light;
        }

        &:active {
          background-color: $gray-50;
        }
      }
    }
  }
}
</style>
