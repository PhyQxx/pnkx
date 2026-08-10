<!--
 * @File: index
 * @Author: PHY
 * @Date: 2022-02-07 13:54
 * @Description: 弹幕组件
-->
<template>
    <vue-danmaku ref="danmaku"
                 :channels="5"
                 :danmus="barrageList"
                 :isSuspend="true"
                 :speeds="100"
                 class="vue-danmaku"
                 loop
                 use-slot
    >
        <template v-slot:dm="{ index, danmu }">
            <div class="one">
                <el-image :src="danmu.authorHeader || danmu.avatar"/>
                <div class="message" v-html="sanitizeHtml(danmu.content)"></div>
            </div>
        </template>
        <div></div>
    </vue-danmaku>
</template>

<script>
import vueDanmaku from 'vue-danmaku'
import {sanitizeHtml} from '@/utils/sanitizeHtml';

export default {
    props: {
        barrageList: {
            type: Array,
            default: []
        }
    },
    components: {
        vueDanmaku
    },
};
</script>
<style lang="scss" scoped>
.vue-danmaku {
    width: 60%;
    height: 80%;
    .one {
        display: flex;
        justify-content: center;
        align-items: center;
        .el-image {
            width: 4rem;
            height: 4rem;
            margin-right: 1rem;
        }
    }
}
</style>
