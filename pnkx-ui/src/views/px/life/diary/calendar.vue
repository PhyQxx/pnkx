<script>
export default {
  name: 'DiaryCalendar',
  props: {
    openDiary: {
      type: Function,
      default: () => {}
    },
    diaryList: {
      type: Array,
      default: () => []
    }
  },
  data() {
    return {
      // 选择时间
      day: new Date(),
      // html文本只显示文字的正则表达式
      regex: /(<([^>]+)>)/ig
    }
  },
  methods: {
    diaryTitle(data) {
      return this.diaryList.find(item => item.date === data.day)
    }
  },
  watch: {
    day() {
      this.$emit('date-change', this.day)
    }
  },
}
</script>

<template>
  <el-calendar v-model="day">
    <template #date-cell="{date, data}">
      <div class="day" @click="openDiary(data, diaryTitle(data))">
        {{ data.day.slice(5) }}
        <div v-if="diaryTitle(data)" class="title">
          {{ diaryTitle(data).content.replace(regex, '') }}
        </div>
        <div
          v-else
          class="title"
          @click="date.getTime() < Date.now() - 8.64e6 ? openDiary(data) : ''"
        />
      </div>
    </template>
  </el-calendar>
</template>

<style scoped lang="scss">
.title {
  margin-top: 0.2rem;
  font-size: var(--text-sm, 0.9rem);
  font-weight: normal;
  color: var(--text-tertiary, #b7b7b7);
  display: -webkit-box;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 3;
  overflow: hidden;
}
.day {
  font-size: var(--text-lg, 1.2rem);
  font-weight: var(--font-bold, bold);
}
</style>
