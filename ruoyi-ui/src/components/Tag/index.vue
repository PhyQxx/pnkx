<template>
    <a class="tags" style="position: absolute" :style="{color: setColor, fontSize: normalSize, margin: randomPosition}">
        {{name}}
    </a>
</template>

<script>
    export default {
        name: "index",
        props: {
            name: {
                type: String,
                default: ''
            }
        },
        computed: {
            setColor() {
                return "hsl(" +
                    Math.round(Math.random() * 360) + "," +
                    Math.round(Math.random() * 100) + '%,' +
                    Math.round(Math.random() * 80) + '%)';
            },
            normalSize() {
                return (0.5+Math.random()) + 'rem'
            },
            randomPosition() {
                let height = Math.random()*10 - 5;
                let width = Math.random()*20 - 10;
                return `${height*14}px 0 0 ${width*16}px`
            }
        }

    }
</script>

<style lang='scss' scoped>
    .tags{
        white-space: nowrap;
    }
</style>
