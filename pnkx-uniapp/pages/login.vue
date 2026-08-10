<template>
  <view class="login-page">
    <!-- Decorative background -->
    <view class="bg-decoration">
      <view class="circle circle-1"></view>
      <view class="circle circle-2"></view>
      <view class="circle circle-3"></view>
    </view>

    <!-- Brand area -->
    <view class="brand-area">
      <image class="brand-logo" :src="globalConfig.appInfo.logo" mode="aspectFit" />
      <text class="brand-name">Pei你看雪</text>
      <text class="brand-slogan">记录生活，陪伴每一刻</text>
    </view>

    <!-- Login form -->
    <view class="login-card">
      <view class="form-area">
        <view class="input-group">
          <view class="input-icon">
            <view class="iconfont icon-user"></view>
          </view>
          <input
            v-model="loginForm.userName"
            class="input-field"
            type="text"
            placeholder="请输入账号"
            placeholder-class="placeholder-text"
            maxlength="30"
          />
        </view>

        <view class="input-group">
          <view class="input-icon">
            <view class="iconfont icon-password"></view>
          </view>
          <input
            v-model="loginForm.password"
            type="password"
            class="input-field"
            placeholder="请输入密码"
            placeholder-class="placeholder-text"
            maxlength="20"
          />
        </view>

        <view class="input-group" v-if="captchaEnabled">
          <view class="input-icon">
            <view class="iconfont icon-code"></view>
          </view>
          <input
            v-model="loginForm.code"
            @keyup.enter.native="handleLogin"
            type="number"
            class="input-field captcha-input"
            placeholder="请输入验证码"
            placeholder-class="placeholder-text"
            maxlength="4"
          />
          <image :src="codeUrl" @click="getCode" class="captcha-img" />
        </view>

        <button @click="handleLogin" class="login-btn">登 录</button>
      </view>
    </view>

    <!-- Footer -->
    <view class="footer">
      <text class="footer-text">v{{ globalConfig.appInfo.version }}</text>
    </view>
  </view>
</template>

<script>
import { getToken } from '@/utils/auth'

export default {
  data() {
    return {
      codeUrl: "",
      captchaEnabled: false,
      globalConfig: getApp().globalData.config,
      loginForm: {
        userName: "",
        password: "",
        code: "",
        uuid: ''
      }
    }
  },
  created() {
    if (getToken()) {
      uni.reLaunch({ url: 'index' })
    }
    if (this.captchaEnabled) {
      this.getCode()
    }
  },
  methods: {
    handlePrivacy() {
      let site = this.globalConfig.appInfo.agreements[0]
      this.$tab.navigateTo(`/pages/common/webview/index?title=${site.title}&url=${site.url}`)
    },
    handleUserAgrement() {
      let site = this.globalConfig.appInfo.agreements[1]
      this.$tab.navigateTo(`/pages/common/webview/index?title=${site.title}&url=${site.url}`)
    },
    getCode() {
      getCodeImg().then(res => {
        this.captchaEnabled = res.captchaEnabled === undefined ? true : res.captchaEnabled
        if (this.captchaEnabled) {
          this.codeUrl = 'data:image/gif;base64,' + res.img
          this.loginForm.uuid = res.uuid
        }
      })
    },
    async handleLogin() {
      if (this.loginForm.userName === "") {
        this.$modal.msgError("请输入您的账号")
      } else if (this.loginForm.password === "") {
        this.$modal.msgError("请输入您的密码")
      } else if (this.loginForm.code === "" && this.captchaEnabled) {
        this.$modal.msgError("请输入验证码")
      } else {
        this.$modal.loading("登录中，请耐心等待...")
        this.pwdLogin()
      }
    },
    async pwdLogin() {
      this.$store.dispatch('Login', this.loginForm).then(() => {
        this.$modal.closeLoading()
        this.loginSuccess()
      }).catch(() => {
        if (this.captchaEnabled) {
          this.getCode()
        }
      })
    },
    loginSuccess(result) {
      this.$store.dispatch('GetInfo').then(res => {
        this.$tab.reLaunch('/pages/index')
      })
    }
  }
}
</script>

<style lang="scss" scoped>
.login-page {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  background: linear-gradient(160deg, #6C9EFF 0%, #A8C8FF 50%, #D6E4FF 100%);
  padding: 0 $page-padding;
  position: relative;
  overflow: hidden;
}

.bg-decoration {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  pointer-events: none;

  .circle {
    position: absolute;
    border-radius: 50%;
    opacity: 0.12;
    background: #FFFFFF;
  }

  .circle-1 {
    width: 400rpx;
    height: 400rpx;
    top: -100rpx;
    right: -80rpx;
    animation: float 8s ease-in-out infinite;
  }

  .circle-2 {
    width: 260rpx;
    height: 260rpx;
    bottom: 200rpx;
    left: -60rpx;
    animation: float 6s ease-in-out infinite reverse;
  }

  .circle-3 {
    width: 180rpx;
    height: 180rpx;
    top: 30%;
    left: 60%;
    animation: float 10s ease-in-out infinite 2s;
  }
}

@keyframes float {
  0%, 100% { transform: translateY(0) scale(1); }
  50% { transform: translateY(-30rpx) scale(1.05); }
}

.brand-area {
  display: flex;
  flex-direction: column;
  align-items: center;
  margin-bottom: 64rpx;
  position: relative;
  z-index: 1;

  .brand-logo {
    width: 140rpx;
    height: 140rpx;
    border-radius: $radius-2xl;
    background: rgba(255, 255, 255, 0.3);
    padding: 16rpx;
    margin-bottom: $spacing-lg;
    box-shadow: 0 8rpx 32rpx rgba(0, 0, 0, 0.1);
  }

  .brand-name {
    font-size: $font-h1;
    font-weight: $font-weight-bold;
    color: #FFFFFF;
    margin-bottom: $spacing-xs;
    letter-spacing: 4rpx;
  }

  .brand-slogan {
    font-size: $font-caption;
    color: rgba(255, 255, 255, 0.8);
    letter-spacing: 2rpx;
  }
}

.login-card {
  width: 100%;
  background: rgba(255, 255, 255, 0.95);
  border-radius: $radius-2xl;
  padding: $spacing-xl $spacing-lg;
  box-shadow: 0 16rpx 48rpx rgba(0, 0, 0, 0.1);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  position: relative;
  z-index: 1;

  .form-area {
    width: 100%;
  }
}

.input-group {
  display: flex;
  align-items: center;
  background: $gray-50;
  border-radius: $radius-lg;
  padding: 0 $spacing-lg;
  height: 96rpx;
  margin-bottom: $spacing-md;
  transition: all $duration-fast $ease-default;
  border: 2rpx solid transparent;

  &:focus-within {
    background: #FFFFFF;
    border-color: $primary-light;
    box-shadow: 0 0 0 4rpx rgba(108, 158, 255, 0.15);
  }

  .input-icon {
    width: 48rpx;
    display: flex;
    align-items: center;
    justify-content: center;
    color: $gray-400;
    font-size: 36rpx;
    margin-right: $spacing-sm;
    flex-shrink: 0;
  }

  .input-field {
    flex: 1;
    height: 100%;
    font-size: $font-body;
    color: $text-primary;
    background: transparent;
  }

  .captcha-input {
    flex: 1;
    min-width: 0;
  }

  .captcha-img {
    height: 72rpx;
    width: 180rpx;
    border-radius: $radius-md;
    margin-left: $spacing-sm;
    flex-shrink: 0;
  }
}

.placeholder-text {
  color: $text-tertiary;
  font-size: $font-body;
}

.login-btn {
  width: 100%;
  height: 96rpx;
  background: linear-gradient(135deg, $primary 0%, $primary-dark 100%);
  color: #FFFFFF;
  font-size: $font-h3;
  font-weight: $font-weight-semibold;
  border: none;
  border-radius: $radius-lg;
  margin-top: $spacing-lg;
  letter-spacing: 4rpx;
  box-shadow: 0 8rpx 24rpx rgba(108, 158, 255, 0.35);
  transition: all $duration-fast $ease-default;

  &:active {
    transform: scale(0.97);
    box-shadow: 0 4rpx 12rpx rgba(108, 158, 255, 0.25);
  }

  &::after {
    border: none;
  }
}

.footer {
  position: absolute;
  bottom: 60rpx;
  left: 0;
  right: 0;
  text-align: center;

  .footer-text {
    font-size: $font-small;
    color: rgba(255, 255, 255, 0.6);
  }
}
</style>
