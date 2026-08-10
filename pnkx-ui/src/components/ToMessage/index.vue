<!--
 * @File: index
 * @Author: PHY
 * @Date: 2022/4/14 11:30
 * @Description: 描述
-->
<template>
    <div v-dom-drag class="to-message" @click="$goToMessagePosition($event)">我要留言</div>
</template>

<script>
export default {
    name: "index",
    data() {
        return {}
    },
    methods: {}
}
</script>

<style lang='scss' scoped>
.to-message {
    position: fixed;
    top: 20%;
    left: 70%;
    z-index: 1001;
    width: 8rem;
    height: 8rem;
    background-color: #5A8DEEDD;
    display: flex;
    justify-content: center;
    align-items: center;
    border-radius: 50%;
    padding: 1rem;
    font-weight: bold;
    font-size: 1.2rem;
    color: #FFFFFF;
    cursor: pointer;
    user-select: none;
    box-shadow: 0 4px 6px #5A8DEEDF;

    &:active {
        background-color: #5A8DEE5F;
    }
}
</style>
