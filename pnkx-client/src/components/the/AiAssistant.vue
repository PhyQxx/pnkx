<script setup lang="ts">
const open = ref(false)
const activated = ref(false)
const color = useColorMode()
const primaryColor = computed(() => color.value === 'dark' ? '#6366f1' : '#f97316')
function toggle() {
  activated.value = true
  open.value = !open.value
}
</script>

<template>
  <button
    type="button"
    aria-label="打开或关闭 AI 助手"
    :aria-expanded="open"
    class="fixed bottom-6 right-6 z-[100] flex h-14 w-14 cursor-pointer items-center justify-center rounded-full shadow-lg transition-transform duration-300 hover:scale-110 max-md:bottom-4 max-md:right-4"
    :style="{ background: primaryColor }"
    @click="toggle"
  >
    <Icon :name="open ? 'ri:close-line' : 'ri:robot-2-fill'" size="26" color="#fff" />
  </button>
  <!-- Keep mounted after first use so closing the panel preserves the conversation. -->
  <LazyTheAiChatPanel v-if="activated" :open="open" @close="open = false" />
</template>
