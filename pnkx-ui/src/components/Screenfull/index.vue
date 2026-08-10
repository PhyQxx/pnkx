<template>
    <div>
        <svg-icon :icon-class="isFullscreen?'exit-fullscreen':'fullscreen'" @click="click"/>
    </div>
</template>

<script>
import screenfull from 'screenfull'

export default {
    name: 'Screenfull',
    data() {
        return {
            isFullscreen: false
        }
    },
    mounted() {
        this.init()
    },
    beforeUnmount() {
        this.destroy()
    },
    methods: {
        click() {
            if (!screenfull.isEnabled) {
                this.$notify({message: '你的浏览器不支持全屏', type: 'warning'})
                return false
            }
            screenfull.toggle()
        },
        change() {
            this.isFullscreen = screenfull.isFullscreen
        },
        init() {
            if (screenfull.isEnabled) {
                screenfull.on('change', this.change)
            }
        },
        destroy() {
            if (screenfull.isEnabled) {
                screenfull.off('change', this.change)
            }
        }
    }
}
</script>

<style lang="scss" scoped>
.screenfull {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    width: 40px;
    height: 40px;
    border-radius: var(--radius-md);
    cursor: pointer;
    transition: background-color 0.3s ease;

    &:hover {
        background-color: var(--bg-hover);
    }
}

.screenfull-svg {
    display: inline-block;
    cursor: pointer;
    fill: var(--text-secondary);
    width: 20px;
    height: 20px;
    transition: fill 0.3s ease;

    &:hover {
        fill: var(--color-primary);
    }
}
</style>
