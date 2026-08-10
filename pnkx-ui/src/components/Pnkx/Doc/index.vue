<template>
    <div>
        <svg-icon icon-class="question" @click="goto"/>
    </div>
</template>

<script>
export default {
    name: 'PnkxDoc',
    data() {
        return {
            url: 'https://gitee.com/phyqxx/pnkx/blob/master/README.md'
        }
    },
    methods: {
        goto() {
            window.open(this.url)
        }
    }
}
</script>
