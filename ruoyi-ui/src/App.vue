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
