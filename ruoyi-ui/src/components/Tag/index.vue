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
                let height = Math.random()*20 - 10;
                let width = Math.random()*14 - 10;
                return `${height*10}px 0 0 ${width*10}px`
            }
        }

    }
</script>

<style lang='scss' scoped>
    .tags{
        white-space: nowrap;
    }
</style>
