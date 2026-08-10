<template>
    <div>
        <svg-icon icon-class="github" @click="goto"/>
    </div>
</template>

<script>
export default {
    name: 'PnkxGit',
    data() {
        return {
            url: 'https://gitee.com/phyqxx/pnkx'
        }
    },
    methods: {
        goto() {
            window.open(this.url)
        }
    }
}
</script>
