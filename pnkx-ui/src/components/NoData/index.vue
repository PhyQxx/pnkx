<template>
    <div class="no-data">
        <div class="no-data-logo"></div>
        <div class="text">{{ text }}</div>
    </div>
</template>

<script>
export default {
    name: "index",
    props: {
        text: {
            type: String,
            default: '暂无数据'
        }
    }
}
</script>

<style lang='scss' scoped>
.no-data {
    display: flex;
    flex-flow: column;
    justify-content: center;
    align-items: center;

    .no-data-logo {
        width: 8rem;
        height: 8rem;
        background-image: url('https://pnkx.cloud:8866/ftp/2022/04/02/b25ba69c-2aac-4145-a1f7-baa40b081ffc.png');
        background-size: 100% 100%;
        background-position: center;
    }

    .text {
        font-size: 1.2rem;
        color: #999;
    }
}
</style>
