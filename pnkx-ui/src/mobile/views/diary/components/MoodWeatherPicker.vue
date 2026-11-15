<template>
    <div class="mood-picker">
        <div class="mood-picker__row">
            <div class="mood-picker__item" @click="openMoodPicker">
                <span class="mood-picker__label">心情</span>
                <span class="mood-picker__content">
                    <img v-if="moodIcon" :src="moodIcon" alt="" class="mood-picker__icon">
                    <span v-else class="mood-picker__placeholder">选择心情</span>
                </span>
                <van-icon name="arrow" size="13" color="var(--pnkx-text-muted)" />
            </div>
            <div class="mood-picker__item" @click="openWeatherPicker">
                <span class="mood-picker__label">天气</span>
                <span class="mood-picker__content">
                    <img v-if="weatherIcon" :src="weatherIcon" alt="" class="mood-picker__icon">
                    <span v-else class="mood-picker__placeholder">选择天气</span>
                </span>
                <van-icon name="arrow" size="13" color="var(--pnkx-text-muted)" />
            </div>
        </div>

        <!-- 心情选择 -->
        <van-popup v-model:show="moodShow" position="bottom" round safe-area-inset-bottom>
            <div class="popup-container">
                <div class="popup-header">
                    <span class="popup-title">选择心情</span>
                    <van-icon name="cross" size="18" @click="moodShow = false" />
                </div>
                <van-search v-model="moodKeyword" placeholder="输入图标名称筛选" shape="round" />
                <div class="popup-grid">
                    <div
                        v-for="name in filteredMoodIcons"
                        :key="name"
                        class="grid-item"
                        :class="{ active: mood === name }"
                        @click="onMoodSelected(name)"
                    >
                        <img :src="iconUrl(name)" alt="" class="grid-item__icon">
                        <span class="grid-item__name">{{ formatIconName(name) }}</span>
                    </div>
                    <div v-if="filteredMoodIcons.length === 0" class="m-empty">没有匹配的图标</div>
                </div>
            </div>
        </van-popup>

        <!-- 天气选择 -->
        <van-popup v-model:show="weatherShow" position="bottom" round safe-area-inset-bottom>
            <div class="popup-container">
                <div class="popup-header">
                    <span class="popup-title">选择天气</span>
                    <van-icon name="cross" size="18" @click="weatherShow = false" />
                </div>
                <van-search v-model="weatherKeyword" placeholder="输入图标名称筛选" shape="round" />
                <div class="popup-grid">
                    <div
                        v-for="name in filteredWeatherIcons"
                        :key="name"
                        class="grid-item"
                        :class="{ active: weather === name }"
                        @click="onWeatherSelected(name)"
                    >
                        <img :src="iconUrl(name)" alt="" class="grid-item__icon">
                        <span class="grid-item__name">{{ formatIconName(name) }}</span>
                    </div>
                    <div v-if="filteredWeatherIcons.length === 0" class="m-empty">没有匹配的图标</div>
                </div>
            </div>
        </van-popup>
    </div>
</template>

<script>
import { iconUrl } from '@/mobile/utils/icons'

// 心情 / 天气图标清单（与 uniapp IconSelect 保持一致）
const MOOD_ICONS = [
    'x-gaoxing', 'x-xiaolian', 'x-taibangle', 'x-xihuan',
    'x-zhuangkeai', 'x-kuxiaobude', 'x-ku', 'x-keshui',
    'x-se', 'x-haochi', 'x-keai', 'x-jingya',
    'x-fennu', 'x-fendou', 'x-daku', 'x-ganga',
    'x-bizui', 'x-bao', 'x-aimu', 'x-chayi'
]

const WEATHER_ICONS = [
    'w-qing', 'w-duoyun', 'w-yin', 'w-xiaoyu', 'w-dayu',
    'w-baoyu', 'w-tedabaoyu', 'w-zhenyu', 'w-leizhenyu',
    'w-xiaoxue', 'w-daxue', 'w-baoxue', 'w-zhenxue',
    'w-yujiaxue', 'w-yejianzhenyu', 'w-yejianzhenxue',
    'w-yejianqing', 'w-yejianduoyun', 'w-wu', 'w-mai',
    'w-yangsha', 'w-shachenbao', 'w-qiangshachenbao', 'w-fuchen', 'w-dongyu'
]

export default {
    name: 'MMoodWeatherPicker',
    props: {
        mood: { type: String, default: '' },
        weather: { type: String, default: '' }
    },
    emits: ['mood-change', 'weather-change'],
    data() {
        return {
            moodShow: false,
            weatherShow: false,
            moodKeyword: '',
            weatherKeyword: ''
        }
    },
    computed: {
        moodIcon() {
            return this.mood ? iconUrl(this.mood) : ''
        },
        weatherIcon() {
            return this.weather ? iconUrl(this.weather) : ''
        },
        filteredMoodIcons() {
            return this.filterIcons(MOOD_ICONS, this.moodKeyword)
        },
        filteredWeatherIcons() {
            return this.filterIcons(WEATHER_ICONS, this.weatherKeyword)
        }
    },
    methods: {
        iconUrl,
        filterIcons(icons, keyword) {
            if (!keyword) return icons
            return icons.filter(icon => icon.toLowerCase().includes(keyword.toLowerCase()))
        },
        formatIconName(name) {
            return name.substring(2)
        },
        openMoodPicker() {
            this.moodKeyword = ''
            this.moodShow = true
        },
        openWeatherPicker() {
            this.weatherKeyword = ''
            this.weatherShow = true
        },
        onMoodSelected(mood) {
            this.$emit('mood-change', mood)
            this.moodShow = false
        },
        onWeatherSelected(weather) {
            this.$emit('weather-change', weather)
            this.weatherShow = false
        }
    }
}
</script>

<style lang="scss" scoped>
.mood-picker {
    &__row {
        display: flex;
        gap: 10px;
    }

    &__item {
        flex: 1;
        display: flex;
        align-items: center;
        justify-content: space-between;
        padding: 12px;
        background: var(--pnkx-surface-muted);
        border-radius: 10px;
    }

    &__label {
        font-size: 14px;
        color: var(--pnkx-text);
        font-weight: 500;
        margin-right: 8px;
    }

    &__content {
        flex: 1;
        display: flex;
        align-items: center;
        justify-content: center;
    }

    &__icon {
        width: 32px;
        height: 32px;
        object-fit: contain;
    }

    &__placeholder {
        font-size: 12px;
        color: var(--pnkx-text-muted);
    }
}

.popup-container {
    background: var(--pnkx-surface);
    border-radius: 16px 16px 0 0;
    max-height: 76vh;
    display: flex;
    flex-direction: column;
}

.popup-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 14px 16px;
    border-bottom: 1px solid var(--pnkx-border-soft);
}

.popup-title {
    font-size: 16px;
    font-weight: 600;
    color: var(--pnkx-text);
}

.popup-grid {
    flex: 1;
    overflow-y: auto;
    display: grid;
    grid-template-columns: repeat(5, 1fr);
    gap: 4px 0;
    padding: 10px 10px calc(14px + env(safe-area-inset-bottom));
}

.grid-item {
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    gap: 5px;
    padding: 8px 2px;
    border-radius: 8px;

    &.active {
        background: var(--pnkx-primary-soft);
    }

    &:active {
        opacity: 0.8;
    }

    &__icon {
        width: 28px;
        height: 28px;
        object-fit: contain;
    }

    &__name {
        max-width: 100%;
        font-size: 10px;
        color: var(--pnkx-text-secondary);
        text-align: center;
        overflow: hidden;
        text-overflow: ellipsis;
        white-space: nowrap;
    }
}
</style>
