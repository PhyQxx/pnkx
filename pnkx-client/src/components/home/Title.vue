<script setup lang="ts">
interface Props {
  title: string
  iconName: string
  wavyColor: string
}

defineProps<Props>()
</script>

<template>
  <h3 class="mb-10 flex flex-col items-center">
    <Icon :name="iconName" size="25" />
    <span
      class="mt-1 text-xl underline decoration-wavy underline-offset-[10px]"
      :style="{ textDecorationColor: wavyColor }"
    >
      {{ title }}
    </span>
  </h3>
</template>
