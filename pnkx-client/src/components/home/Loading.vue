<script setup lang="ts">
import { NSkeleton } from 'naive-ui'

interface Props {
  active?: boolean
}

withDefaults(defineProps<Props>(), {
  active: false
})
</script>

<template>
  <li
    class="post-loading mb-10 overflow-hidden rounded-lg bg-[rgba(255,255,255,0.9)] shadow-[0_1px_30px_-4px_#e8e8e8] dark:bg-[rgba(51,51,51,0.7)] dark:shadow-[0_1px_35px_-8px_rgba(26,26,26,0.6)] md:flex md:h-56"
    :class="{ 'flex-row-reverse': active }"
  >
    <div class="max-md:h-64 md:flex-[1.4]">
      <n-skeleton height="100%" width="100%" />
    </div>
    <div class="px-5 pt-3 md:flex-1">
      <n-skeleton height="40px" width="60%" :sharp="false" />
      <n-skeleton height="40px" width="80%" :sharp="false" class="my-3" />
      <n-skeleton text :repeat="4" />
    </div>
  </li>
</template>
