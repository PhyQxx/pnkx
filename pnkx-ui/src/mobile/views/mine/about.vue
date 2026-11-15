<template>
    <div class="about-page">
        <div class="about-brand">
            <div class="about-brand__bg"></div>
            <div class="about-brand__body">
                <img class="about-brand__logo" src="@/mobile/assets/images/logo200.png" alt="">
                <div class="about-brand__name">Pei你看雪</div>
                <div class="about-brand__slogan">陪伴你的生活每一天</div>
            </div>
        </div>

        <van-cell-group inset :border="false" class="m-cell-card">
            <van-cell title="版本信息" :value="version" />
            <van-cell title="官方邮箱" value="pnkx0930@foxmail.com" />
            <van-cell title="服务热线" value="15006732580" />
            <van-cell title="公司网站" is-link @click="openSite">
                <template #value>
                    <span class="about-site">{{ siteUrl }}</span>
                </template>
            </van-cell>
        </van-cell-group>

        <div class="about-copyright">Copyright © {{ year }} pnkx.top All Rights Reserved.</div>
    </div>
</template>

<script>
import { appVersion, siteUrl } from '@/mobile/utils/version'

export default {
    name: 'MMineAbout',
    data() {
        return {
            version: appVersion,
            siteUrl,
            year: new Date().getFullYear()
        }
    },
    methods: {
        openSite() {
            window.open(this.siteUrl, '_blank')
        }
    }
}
</script>

<style lang="scss" scoped>
.about-page {
    min-height: 100vh;
    display: flex;
    flex-direction: column;
}

.about-brand {
    position: relative;
    overflow: hidden;
    padding: 48px 20px 36px;

    &__bg {
        position: absolute;
        inset: 0;
        background:
            linear-gradient(180deg, rgba(248, 251, 255, 0.02), rgba(242, 247, 254, 0.18)),
            url('@/mobile/assets/images/glacier-aurora-bg.jpg') center / cover no-repeat;
        z-index: 0;
    }

    &__body {
        position: relative;
        z-index: 1;
        display: flex;
        flex-direction: column;
        align-items: center;
        gap: 8px;
    }

    &__logo {
        width: 72px;
        height: 72px;
        border-radius: 18px;
        box-shadow: 0 8px 20px rgba(15, 23, 42, 0.12);
    }

    &__name {
        margin-top: 8px;
        font-size: 20px;
        font-weight: 700;
        color: var(--pnkx-text);
    }

    &__slogan {
        font-size: 13px;
        color: var(--pnkx-text-secondary);
    }
}

.about-site { color: var(--pnkx-primary); }

.about-copyright {
    text-align: center;
    padding: 24px 0 16px;
    font-size: 11px;
    color: var(--pnkx-text-muted);
}
</style>
