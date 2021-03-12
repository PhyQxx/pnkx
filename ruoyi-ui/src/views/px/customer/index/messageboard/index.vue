<template>
    <div class="message-board-page">
        <message-board messageType="1"/>
    </div>
</template>

<script>
    import messageBoard from '@/components/MessageBoard/index'

    export default {
        name: "index",
        components: {
            messageBoard
        },
        data() {
            return {
            }
        },
        mounted() {
        },
        methods: {
        }
    }
</script>

<style lang="scss" scoped>
    .message-board-page{

    }
</style>
