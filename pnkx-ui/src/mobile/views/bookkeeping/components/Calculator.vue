<template>
    <div class="calc">
        <div class="calc__grid">
            <button class="calc__key calc__key--num" @click="clickKey('num', '7')">7</button>
            <button class="calc__key calc__key--num" @click="clickKey('num', '8')">8</button>
            <button class="calc__key calc__key--num" @click="clickKey('num', '9')">9</button>
            <button class="calc__key calc__key--func" @click="clickKey('func', '-')">-</button>

            <button class="calc__key calc__key--num" @click="clickKey('num', '4')">4</button>
            <button class="calc__key calc__key--num" @click="clickKey('num', '5')">5</button>
            <button class="calc__key calc__key--num" @click="clickKey('num', '6')">6</button>
            <button class="calc__key calc__key--func" @click="clickKey('func', '+')">+</button>

            <button class="calc__key calc__key--num" @click="clickKey('num', '1')">1</button>
            <button class="calc__key calc__key--num" @click="clickKey('num', '2')">2</button>
            <button class="calc__key calc__key--num" @click="clickKey('num', '3')">3</button>
            <button class="calc__key calc__key--confirm" @click="clickKey('confirm')">确定</button>

            <button class="calc__key calc__key--num" @click="clickKey('point', '.')">.</button>
            <button class="calc__key calc__key--num" @click="clickKey('num', '0')">0</button>
            <button class="calc__key calc__key--clear" @click="clickKey('clear')">
                <img v-if="clearIcon" class="calc__clear-icon" :src="clearIcon" alt="">
                <van-icon v-else name="delete-o" />
            </button>
        </div>
    </div>
</template>

<script>
import { iconUrl } from '@/mobile/utils/icons'

export default {
    name: 'BkCalculator',
    data() {
        return {
            result: 0,
            inputValue: ''
        }
    },
    computed: {
        clearIcon() {
            return iconUrl('clear')
        },
        isFunction() {
            return this.inputValue.indexOf('-') !== -1 || this.inputValue.indexOf('+') !== -1
        },
        funcOperator() {
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
                        this.result = this.calcExpression()
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
                        this.result = this.calcExpression()
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
        },
        // 按当前运算符计算已输入表达式（与蓝本逐字一致：NaN/空段按 0 处理）
        calcExpression() {
            const op = this.funcOperator
            const parts = String(this.inputValue).split(op)
            const left = Number(parts[0])
            const right = Number(parts[1])
            if (op === '+') return left + right
            return left - right
        }
    }
}
</script>

<style lang="scss" scoped>
.calc {
    padding: 8px;

    &__grid {
        display: grid;
        grid-template-columns: repeat(4, 1fr);
        grid-auto-rows: minmax(48px, 1fr);
        gap: 6px;
    }

    &__key {
        border: none;
        display: flex;
        align-items: center;
        justify-content: center;
        border-radius: 10px;
        font-weight: 700;
        font-size: 18px;
        background: transparent;
        padding: 0;
        transition: transform 80ms ease, background-color 0.15s ease;

        &:active {
            transform: scale(0.92);
        }
    }

    &__key--num {
        background: var(--pnkx-surface-muted);
        color: var(--pnkx-text);

        &:active {
            background: var(--pnkx-primary-soft);
        }
    }

    &__key--func {
        background: var(--pnkx-primary-soft);
        color: var(--pnkx-primary);
        font-size: 20px;

        &:active {
            opacity: 0.75;
        }
    }

    &__key--clear {
        background: var(--pnkx-surface-muted);
        color: var(--pnkx-text-secondary);

        &:active {
            background: var(--pnkx-danger-soft);
            color: var(--pnkx-danger);
        }
    }

    &__key--confirm {
        grid-row: span 2;
        background: var(--pnkx-primary);
        color: #fff;
        font-size: 16px;

        &:active {
            opacity: 0.85;
        }
    }

    &__clear-icon {
        width: 22px;
        height: 22px;
        object-fit: contain;
    }
}
</style>
