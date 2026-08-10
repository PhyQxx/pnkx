<template>
  <view class="no-data">
    <view v-if="loading" class="loading_container">
      <view class="loading">
        <view class="loading_a"></view>
        <view class="loading_b"></view>
        <view class="loading_c"></view>
      </view>
    </view>
    <view v-else class="text">
      {{ text }}
    </view>
  </view>
</template>

<script>
export default {
  name: "NoData",
  props: {
    loading: {
      type: Boolean,
      default: false
    },
    text: {
      type: String,
      default: '暂无数据'
    }
  }
}
</script>

<style lang="scss">
.no-data {
  .text {
    padding: 0.5rem;
    text-align: center;
  }

  .loading_container {
    background: $bg-card;
    display: flex;
    justify-content: center;
    align-items: center;
  }

  @keyframes ball-beat {
    50% {
      opacity: 0.2;
      -webkit-transform: scale(0.75);
      transform: scale(0.75);
    }

    100% {
      opacity: 1;
      -webkit-transform: scale(1);
      transform: scale(1);
    }
  }

  .loading {
    width: 200rpx;
    margin-top: -36rpx;
    display: flex;
    flex-direction: row;
    justify-content: space-between;
  }

  .loading_a,
  .loading_b,
  .loading_c {
    width: 20rpx;
    height: 20rpx;
    margin: 40rpx 10rpx;
    display: inline-block;
    -webkit-animation: ball-beat 0.7s 0s infinite linear;
    animation: ball-beat 0.7s 0s infinite linear;
    background-color: $primary;
    border-radius: 100%;
  }

  .loading_b {
    -webkit-animation-delay: -0.35s !important;
    animation-delay: -0.35s !important;
  }
}
</style>
