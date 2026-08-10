<!--
 * @File: Calculator
 * @Author: PHY
 * @Date: 2021/11/19 17:31
 * @Description: 计算器组件
-->
<template>
  <view class="calculator">
    <view class="calculator__grid">
      <view class="calculator__key calculator__key--num" @click="clickKey('num', '7')">7</view>
      <view class="calculator__key calculator__key--num" @click="clickKey('num', '8')">8</view>
      <view class="calculator__key calculator__key--num" @click="clickKey('num', '9')">9</view>
      <view class="calculator__key calculator__key--func" @click="clickKey('func', '-')">-</view>

      <view class="calculator__key calculator__key--num" @click="clickKey('num', '4')">4</view>
      <view class="calculator__key calculator__key--num" @click="clickKey('num', '5')">5</view>
      <view class="calculator__key calculator__key--num" @click="clickKey('num', '6')">6</view>
      <view class="calculator__key calculator__key--func" @click="clickKey('func', '+')">+</view>

      <view class="calculator__key calculator__key--num" @click="clickKey('num', '1')">1</view>
      <view class="calculator__key calculator__key--num" @click="clickKey('num', '2')">2</view>
      <view class="calculator__key calculator__key--num" @click="clickKey('num', '3')">3</view>
      <view class="calculator__key calculator__key--confirm" @click="clickKey('confirm')">确定</view>

      <view class="calculator__key calculator__key--num" @click="clickKey('point', '.')">.</view>
      <view class="calculator__key calculator__key--num" @click="clickKey('num', '0')">0</view>
      <view class="calculator__key calculator__key--clear" @click="clickKey('clear')">
        <svg-icon icon-class="clear" size="36rpx" />
      </view>
    </view>
  </view>
</template>

<script>
export default {
  name: "Calculator",
  data() {
    return {
      result: 0,
      inputValue: '',
    }
  },
  computed: {
    isFunction() {
      return this.inputValue.indexOf('-') !== -1 || this.inputValue.indexOf('+') !== -1
    },
    function() {
      if (this.inputValue.indexOf('-') !== -1) return '-'
      if (this.inputValue.indexOf('+') !== -1) return '+'
      return '+'
    }
  },
  watch: {
    inputValue(newValue) {
      this.$emit('getInputValue', newValue, this.result)
    }
  },
  methods: {
    clickKey(type, value) {
      switch (type) {
        case 'num':
          if (this.isFunction) {
            this.inputValue += value
            if (this.function === '+') {
              this.result = Number(this.inputValue.split(this.function)[0]) + Number(this.inputValue.split(this.function)[1])
            } else if (this.function === '-') {
              this.result = Number(this.inputValue.split(this.function)[0]) - Number(this.inputValue.split(this.function)[1])
            }
          } else {
            this.inputValue += value
            this.result = Number(this.inputValue)
          }
          break
        case 'func':
          if (this.isFunction) {
            this.inputValue = this.result
            this.inputValue += value
          } else {
            this.result = Number(this.inputValue)
            this.inputValue += value
          }
          break
        case 'point':
          this.inputValue += value
          break
        case 'clear':
          this.inputValue = this.inputValue.slice(0, -1)
          if (this.isFunction) {
            if (this.function === '+') {
              this.result = Number(this.inputValue.split(this.function)[0]) + Number(this.inputValue.split(this.function)[1])
            } else if (this.function === '-') {
              this.result = Number(this.inputValue.split(this.function)[0]) - Number(this.inputValue.split(this.function)[1])
            }
          } else {
            this.result = Number(this.inputValue)
          }
          break
        case 'confirm':
          this.$emit('confirm')
          break
        default:
          break
      }
    }
  }
}
</script>

<style lang="scss" scoped>
.calculator {
  background-color: $bg-card;
  padding: $spacing-md;

  &__grid {
    display: grid;
    grid-template-columns: repeat(4, 1fr);
    grid-template-rows: repeat(4, 80rpx);
    gap: $spacing-xs;
  }

  &__key {
    display: flex;
    align-items: center;
    justify-content: center;
    border-radius: $radius-md;
    font-weight: $font-weight-bold;
    font-size: $font-h2;
    transition: transform 80ms $ease-default, background-color $duration-fast $ease-default;

    &:active {
      transform: scale(0.92);
    }
  }

  &__key--num {
    background-color: $gray-50;
    color: $text-primary;

    &:active {
      background-color: rgba($primary, 0.12);
    }
  }

  &__key--func {
    background-color: $bookkeeping-light;
    color: $primary;

    &:active {
      background-color: rgba($primary, 0.2);
    }
  }

  &__key--clear {
    background-color: $gray-100;
    color: $text-secondary;

    &:active {
      background-color: rgba($danger, 0.12);
      color: $danger;
    }
  }

  &__key--confirm {
    background: linear-gradient(135deg, $primary 0%, $primary-dark 100%);
    color: $text-inverse;
    grid-row: span 2;
    font-size: $font-h3;

    &:active {
      opacity: 0.85;
    }
  }
}
</style>
