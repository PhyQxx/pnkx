<template>
  <div id="app">
    <router-view />
  </div>
</template>

<script>
export default  {
  name:  'App',
    mounted() {
        let htmlWidth = document.documentElement.clientWidth || document.body.clientWidth;
        let html = document.documentElement;
        html.style.fontSize = htmlWidth/150 + 'px';
    }
}
</script>

<style>
    /*全局样式*/
    html, body{
    width: 100%;
    height: 100%;
    background-color: #bfe7fa;
    }
    .pointer{
        cursor: pointer;
    }

</style>
