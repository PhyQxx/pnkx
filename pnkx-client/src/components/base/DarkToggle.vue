<script setup lang="ts">
const color = useColorMode()

function toggleDark() {
  color.preference = color.value === 'dark' ? 'light' : 'dark'
}
</script>

<template>
  <div class="mr-4 cursor-pointer" @click="toggleDark()">
    <Icon v-if="color.value === 'dark'" name="line-md:sun-rising-filled-loop" size="22" />
    <Icon v-else name="line-md:moon-filled-loop" size="22" />
  </div>
</template>
