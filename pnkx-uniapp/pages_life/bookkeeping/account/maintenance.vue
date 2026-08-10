<template>
  <view class="maintenance">
    <uni-forms ref="form" :model="form" :rules="rules" class="form">
      <view class="form-section">
        <text class="form-section__title">基本信息</text>
        <uni-forms-item label="账户名称" name="accountName" required>
          <uni-easyinput v-model="form.accountName" placeholder="请输入账户名称"/>
        </uni-forms-item>
        <uni-forms-item label="账户余额" name="balance" required>
          <uni-easyinput v-model="form.balance" type="number" placeholder="请输入账户余额"/>
        </uni-forms-item>
        <uni-forms-item label="账户类型" name="accountType" required>
          <uni-data-select
            v-model="form.accountType"
            :localdata="accountTypeOptions"
            placeholder="请选择账户类型"
          />
        </uni-forms-item>
      </view>

      <view class="form-section">
        <text class="form-section__title">个性化</text>
        <uni-forms-item label="账户图标" name="accountIcon">
          <view class="icon-picker" @click="showIconPicker = true">
            <view class="icon-preview">
              <svg-icon :icon-class="form.accountIcon || '默认'" size="48rpx"/>
            </view>
            <text class="icon-text">{{ form.accountIcon || '选择图标' }}</text>
            <uni-icons type="arrowright" size="16"/>
          </view>
        </uni-forms-item>
      </view>

      <view class="form-section">
        <text class="form-section__title">其他</text>
        <uni-forms-item label="备注" name="remark">
          <uni-easyinput
            v-model="form.remark"
            type="textarea"
            placeholder="请输入备注"
            :maxlength="200"
          />
        </uni-forms-item>
      </view>
    </uni-forms>

    <view class="actions">
      <button v-if="form.id" class="btn-delete" @click="handleDelete">删除账户</button>
      <button class="btn-save" @click="handleSave">保存</button>
    </view>

    <uni-popup ref="iconPopup" type="bottom" background-color="#fff">
      <view class="icon-picker-popup">
        <view class="popup-header">
          <view class="popup-cancel" @click="$refs.iconPopup.close()">取消</view>
          <view class="popup-title">选择图标</view>
          <view class="popup-confirm" @click="confirmIcon">确定</view>
        </view>
        <scroll-view class="icon-list" scroll-y>
          <view
            v-for="icon in iconList"
            :key="icon"
            class="icon-item"
            :class="{ selected: tempIcon === icon }"
            @click="selectIcon(icon)"
          >
            <svg-icon :icon-class="icon" size="64rpx"/>
            <text class="icon-name">{{ icon }}</text>
          </view>
        </scroll-view>
      </view>
    </uni-popup>
  </view>
</template>

<script>
import { getAccount, addAccount, updateAccount, delAccount } from "@/api/px/life/bookkeeping/account";
import { getDicts } from "@/api/system/dict/data";
import uniPopup from '@/uni_modules/uni-popup/components/uni-popup/uni-popup.vue'

export default {
  name: "AccountMaintenance",
  components: {
    uniPopup
  },
  data() {
    return {
      form: {
        id: null,
        accountName: '',
        balance: 0,
        accountType: '',
        accountIcon: '',
        remark: ''
      },
      rules: {
        accountName: { rules: [{ required: true, errorMessage: '请输入账户名称' }] },
        balance: { rules: [{ required: true, errorMessage: '请输入账户余额' }] },
        accountType: { rules: [{ required: true, errorMessage: '请选择账户类型' }] }
      },
      accountTypeOptions: [],
      iconList: [
        '微信', '支付宝', '银行卡', '信用卡', '现金',
        '京东', '花呗', '白条', '储蓄卡', '理财',
        '股票', '基金', '默认'
      ],
      tempIcon: '',
      showIconPicker: false
    };
  },
  onLoad(options) {
    if (options.accountId) {
      this.form.id = options.accountId;
      this.getAccountInfo();
    }
    this.getAccountTypes();
  },
  onNavigationBarButtonTap(e) {
    if (e.index === 0) {
      this.handleSave();
    }
  },
  methods: {
    async getAccountTypes() {
      try {
        const res = await getDicts('px_bookkeeping_account_type');
        this.accountTypeOptions = res.data.map(item => ({
          value: item.dictValue,
          text: item.dictLabel
        }));
      } catch (e) {
        console.error('获取账户类型失败', e);
      }
    },
    async getAccountInfo() {
      try {
        const res = await getAccount(this.form.id);
        this.form = res.data;
      } catch (e) {
        console.error('获取账户信息失败', e);
      }
    },
    selectIcon(icon) {
      this.tempIcon = icon;
    },
    confirmIcon() {
      this.form.accountIcon = this.tempIcon;
      this.$refs.iconPopup.close();
    },
    async handleSave() {
      try {
        const valid = await this.$refs.form.validate();
        if (!valid) return;

        uni.showLoading({ title: '保存中' });

        if (this.form.id) {
          await updateAccount(this.form);
          uni.showToast({ title: '修改成功', icon: 'success' });
        } else {
          await addAccount(this.form);
          uni.showToast({ title: '新增成功', icon: 'success' });
        }

        setTimeout(() => {
          uni.navigateBack();
        }, 1500);
      } catch (e) {
        console.error('保存失败', e);
      } finally {
        uni.hideLoading();
      }
    },
    handleDelete() {
      uni.showModal({
        title: '删除确认',
        content: '删除该账户也会删除其下流水记录，确定删除？',
        confirmColor: '#FF6B6B',
        success: async (res) => {
          if (res.confirm) {
            try {
              await delAccount(this.form.id);
              uni.showToast({ title: '删除成功', icon: 'success' });
              setTimeout(() => {
                uni.navigateBack();
              }, 1500);
            } catch (e) {
              console.error('删除失败', e);
            }
          }
        }
      });
    }
  },
  watch: {
    showIconPicker(val) {
      if (val) {
        this.tempIcon = this.form.accountIcon;
        this.$refs.iconPopup.open();
      }
    }
  }
};
</script>

<style lang="scss" scoped>
.maintenance {
  min-height: 100vh;
  background-color: $bg-page;
  padding: $section-gap;

  .form {
    padding: 0;
    background: transparent;
    border-radius: 0;
    box-shadow: none;
  }

  .form-section {
    background: $bg-card;
    border-radius: $radius-lg;
    padding: $spacing-lg;
    margin-bottom: $spacing-md;
    box-shadow: $shadow-card;

    &__title {
      display: block;
      font-size: $font-body;
      font-weight: $font-weight-medium;
      color: $text-secondary;
      padding-bottom: $spacing-sm;
      margin-bottom: $spacing-sm;
      border-bottom: 1rpx solid $border-light;
    }
  }

  .icon-picker {
    display: flex;
    align-items: center;
    padding: $spacing-sm 0;

    .icon-preview {
      width: 72rpx;
      height: 72rpx;
      border-radius: $radius-md;
      background: $gray-50;
      display: flex;
      align-items: center;
      justify-content: center;
      margin-right: $spacing-sm;
    }

    .icon-text {
      flex: 1;
      font-size: $font-body;
      color: $text-primary;
    }
  }

  .actions {
    margin-top: $spacing-xl;
    padding: 0 $section-gap;

    .btn-delete {
      background: rgba($danger, 0.08);
      color: $danger;
      border: none;
      margin-bottom: $section-gap;
      transition: transform $duration-fast $ease-default;

      &:active {
        transform: scale(0.96);
      }
    }

    .btn-save {
      background: linear-gradient(135deg, $primary 0%, $primary-dark 100%);
      color: $text-inverse;
      border: none;
      transition: transform $duration-fast $ease-default;

      &:active {
        transform: scale(0.96);
      }
    }
  }

  .icon-picker-popup {
    border-radius: $radius-2xl $radius-2xl 0 0;

    .popup-header {
      display: flex;
      align-items: center;
      justify-content: space-between;
      padding: $spacing-lg;
      border-bottom: 1rpx solid $border-color;

      .popup-title {
        font-size: $font-h2;
        font-weight: $font-weight-medium;
        color: $text-primary;
      }

      .popup-cancel,
      .popup-confirm {
        font-size: $font-body;
        padding: $spacing-sm;
      }

      .popup-cancel {
        color: $text-secondary;
      }

      .popup-confirm {
        color: $primary;
      }
    }

    .icon-list {
      max-height: 600rpx;
      padding: $section-gap;
      display: flex;
      flex-wrap: wrap;

      .icon-item {
        width: calc(25% - 18rpx);
        display: flex;
        flex-direction: column;
        align-items: center;
        padding: $section-gap 0;
        margin: $spacing-2xs;
        border-radius: $radius-md;
        background: $gray-50;
        transition: all $duration-fast $ease-default;

        &:active {
          transform: scale(0.95);
        }

        &.selected {
          background: $bookkeeping-light;
        }

        .icon-name {
          font-size: $font-small;
          color: $text-secondary;
          margin-top: $spacing-xs;
        }
      }
    }
  }
}
</style>
