<!--
  * @File: record
  * @Author: PHY
  * @Date: 2025/03/10
  * @Description: 姨妈记录编辑页
-->
<template>
  <view class="record-page subpage-shell">
    <scroll-view class="record-scroll" scroll-y>
      <view class="record-hero">
        <text class="record-hero__eyebrow">{{ recordId ? '编辑记录' : '新增记录' }}</text>
        <text class="record-hero__title">{{ form.date || '选择日期' }}</text>
        <text class="record-hero__text">把关键变化轻量记下来，日后看趋势会更省心。</text>
      </view>

      <uni-forms ref="form" :model="form" :rules="rules" label-width="0">
        <view class="form-section">
          <view class="section-title">
            <view>
              <text class="section-title__main">记录日期</text>
              <text class="section-title__sub">默认带入当前选择的日期</text>
            </view>
          </view>
          <uni-forms-item name="date" required>
            <uni-datetime-picker
              v-model="form.date"
              type="date"
              :clear-icon="false"
              @change="onDateChange"
            />
          </uni-forms-item>
        </view>

        <view class="form-section">
          <view class="section-title">
            <view>
              <text class="section-title__main">经期状态</text>
              <text class="section-title__sub">开始和结束只能选择一个</text>
            </view>
          </view>

          <view class="toggle-row">
            <view class="toggle-card" :class="{ active: form.come }" @click="toggleCome">
              <view class="toggle-card__icon start">
                <uni-icons type="heart-filled" size="20" color="#FFFFFF" />
              </view>
              <view class="toggle-card__text">
                <text class="toggle-card__title">姨妈来了</text>
                <text class="toggle-card__desc">标记经期开始</text>
              </view>
              <switch :checked="form.come" @click.stop @change="onComeChange" color="#F472B6" />
            </view>

            <view class="toggle-card" :class="{ active: form.go }" @click="toggleGo">
              <view class="toggle-card__icon end">
                <uni-icons type="checkmarkempty" size="22" color="#FFFFFF" />
              </view>
              <view class="toggle-card__text">
                <text class="toggle-card__title">姨妈走了</text>
                <text class="toggle-card__desc">标记经期结束</text>
              </view>
              <switch :checked="form.go" @click.stop @change="onGoChange" color="#4F86F7" />
            </view>
          </view>
        </view>

        <view class="form-section">
          <view class="section-title">
            <view>
              <text class="section-title__main">身体数据</text>
              <text class="section-title__sub">选填，适合记录体温和体重变化</text>
            </view>
          </view>

          <view class="field-grid">
            <view class="field-card">
              <text class="field-card__label">体温</text>
              <uni-easyinput
                v-model="form.temperature"
                type="number"
                placeholder="℃"
                :clearable="true"
              />
            </view>
            <view class="field-card">
              <text class="field-card__label">体重</text>
              <uni-easyinput
                v-model="form.weight"
                type="number"
                placeholder="kg"
                :clearable="true"
              />
            </view>
          </view>
        </view>

        <view class="form-section">
          <view class="section-title">
            <view>
              <text class="section-title__main">其他信息</text>
              <text class="section-title__sub">检查、同房和备注会展示在记录卡片中</text>
            </view>
          </view>

          <view class="toggle-card single" :class="{ active: form.makeLove }" @click="toggleMakeLove">
            <view class="toggle-card__icon soft">
              <uni-icons type="heart" size="20" color="#BE185D" />
            </view>
            <view class="toggle-card__text">
              <text class="toggle-card__title">有同房记录</text>
              <text class="toggle-card__desc">用于后续回看周期事件</text>
            </view>
            <switch :checked="form.makeLove" @click.stop @change="onMakeLoveChange" color="#F472B6" />
          </view>

          <view class="field-list">
            <view class="field-block">
              <text class="field-block__label">检查项目</text>
              <uni-easyinput
                v-model="form.items"
                placeholder="例如：排卵试纸、B超、激素检查"
                :clearable="true"
              />
            </view>

            <view class="field-block">
              <text class="field-block__label">检查结果</text>
              <uni-easyinput
                v-model="form.results"
                type="textarea"
                placeholder="记录检查结果或医生建议"
                :clearable="true"
                :maxlength="500"
              />
            </view>

            <view class="field-block">
              <text class="field-block__label">备注</text>
              <uni-easyinput
                v-model="form.remark"
                type="textarea"
                placeholder="身体感受、情绪、疼痛程度等"
                :clearable="true"
                :maxlength="200"
              />
            </view>
          </view>
        </view>
      </uni-forms>

      <view v-if="recordId" class="delete-section">
        <button class="delete-btn" @click="handleDelete">删除记录</button>
      </view>
    </scroll-view>

    <view class="bottom-bar">
      <button class="save-btn" :loading="saving" @click="handleSave">
        {{ saving ? '保存中' : '保存记录' }}
      </button>
    </view>
  </view>
</template>

<script>
import {
  getMenstruationRecord,
  addMenstruationRecord,
  updateMenstruationRecord,
  delMenstruationRecord
} from '@/api/px/life/menstruationRecord'

export default {
  name: 'MenstruationAssistantRecord',
  data() {
    return {
      recordId: null,
      form: {
        date: '',
        come: false,
        go: false,
        makeLove: false,
        temperature: '',
        weight: '',
        items: '',
        results: '',
        remark: '',
        type: null
      },
      rules: {
        date: {
          rules: [{ required: true, errorMessage: '请选择日期' }]
        }
      },
      saving: false
    }
  },
  onLoad(options) {
    if (options.id) {
      this.recordId = options.id
      this.loadRecordDetail()
    } else if (options.date) {
      this.form.date = options.date
    } else {
      this.form.date = this.formatDate(new Date())
    }
  },
  onNavigationBarButtonTap(e) {
    if (e.index === 0) {
      this.handleSave()
    }
  },
  methods: {
    formatDate(date) {
      if (!date) return ''
      const d = new Date(date)
      return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
    },

    async loadRecordDetail() {
      try {
        uni.showLoading({ title: '加载中...' })
        const response = await getMenstruationRecord(this.recordId)
        uni.hideLoading()

        if (response.code === 200) {
          const data = response.data
          this.form = {
            date: data.date ? data.date.substring(0, 10) : '',
            come: data.type === '0',
            go: data.type === '1',
            makeLove: data.makeLove || false,
            temperature: data.temperature || '',
            weight: data.weight || '',
            items: data.items || '',
            results: data.results || '',
            remark: data.remark || '',
            type: data.type
          }
        }
      } catch (error) {
        uni.hideLoading()
        console.error('加载记录详情失败:', error)
        uni.showToast({
          title: '加载失败',
          icon: 'none'
        })
      }
    },

    onDateChange(e) {
      this.form.date = e
    },

    toggleCome() {
      this.form.come = !this.form.come
      if (this.form.come) this.form.go = false
    },

    toggleGo() {
      this.form.go = !this.form.go
      if (this.form.go) this.form.come = false
    },

    toggleMakeLove() {
      this.form.makeLove = !this.form.makeLove
    },

    onComeChange(e) {
      this.form.come = e.detail.value
      if (this.form.come) this.form.go = false
    },

    onGoChange(e) {
      this.form.go = e.detail.value
      if (this.form.go) this.form.come = false
    },

    onMakeLoveChange(e) {
      this.form.makeLove = e.detail.value
    },

    async handleSave() {
      try {
        await this.$refs.form.validate()
      } catch (error) {
        return
      }

      if (!this.form.date) {
        uni.showToast({
          title: '请选择日期',
          icon: 'none'
        })
        return
      }

      this.saving = true

      try {
        const data = {
          date: this.form.date,
          makeLove: this.form.makeLove,
          temperature: this.form.temperature ? parseFloat(this.form.temperature) : null,
          weight: this.form.weight ? parseFloat(this.form.weight) : null,
          items: this.form.items,
          results: this.form.results,
          remark: this.form.remark
        }

        if (this.form.come) {
          data.type = '0'
        } else if (this.form.go) {
          data.type = '1'
        } else {
          data.type = null
        }

        let response
        if (this.recordId) {
          data.id = this.recordId
          response = await updateMenstruationRecord(data)
        } else {
          response = await addMenstruationRecord(data)
        }

        this.saving = false

        if (response.code === 200) {
          uni.showToast({
            title: this.recordId ? '修改成功' : '添加成功',
            icon: 'success'
          })
          setTimeout(() => {
            uni.navigateBack()
          }, 800)
        }
      } catch (error) {
        this.saving = false
        console.error('保存记录失败:', error)
        uni.showToast({
          title: '保存失败',
          icon: 'none'
        })
      }
    },

    handleDelete() {
      uni.showModal({
        title: '确认删除',
        content: '确定要删除这条记录吗？',
        success: async (res) => {
          if (res.confirm) {
            try {
              uni.showLoading({ title: '删除中...' })
              const response = await delMenstruationRecord(this.recordId)
              uni.hideLoading()

              if (response.code === 200) {
                uni.showToast({
                  title: '删除成功',
                  icon: 'success'
                })
                setTimeout(() => {
                  uni.navigateBack()
                }, 800)
              }
            } catch (error) {
              uni.hideLoading()
              console.error('删除记录失败:', error)
              uni.showToast({
                title: '删除失败',
                icon: 'none'
              })
            }
          }
        }
      })
    }
  }
}
</script>

<style lang="scss" scoped>
.record-page {
  height: 100vh;
  display: flex;
  flex-direction: column;
  background: #F2F7FE;
}

.record-scroll {
  flex: 1;
  height: 0;
  padding: 24rpx 30rpx 150rpx;
}

.record-hero {
  padding: 30rpx;
  border-radius: 24rpx;
  margin-bottom: 22rpx;
  background: linear-gradient(135deg, #FFF1F6 0%, #FFFFFF 62%, #EEF6FF 100%);
  box-shadow: 0 12rpx 32rpx rgba(244, 114, 182, 0.1);

  &__eyebrow {
    display: block;
    font-size: 24rpx;
    color: #BE185D;
    font-weight: 600;
  }

  &__title {
    display: block;
    margin-top: 10rpx;
    font-size: 42rpx;
    font-weight: 800;
    color: #1A202C;
  }

  &__text {
    display: block;
    margin-top: 12rpx;
    font-size: 25rpx;
    color: #6B7B8D;
    line-height: 1.5;
  }
}

.form-section {
  padding: 26rpx;
  border-radius: 18rpx;
  background: #FFFFFF;
  margin-bottom: 20rpx;

  ::v-deep .uni-forms-item {
    margin-bottom: 0;
  }

  ::v-deep .uni-easyinput__content {
    min-height: 76rpx;
    border: none;
    border-radius: 14rpx;
    background: #F2F7FE;
  }
}

.section-title {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 22rpx;

  &__main {
    display: block;
    font-size: 30rpx;
    font-weight: 700;
    color: #1A202C;
  }

  &__sub {
    display: block;
    margin-top: 6rpx;
    font-size: 23rpx;
    color: #8EA0B8;
  }
}

.toggle-row {
  display: flex;
  flex-direction: column;
  gap: 16rpx;
}

.toggle-card {
  display: flex;
  align-items: center;
  gap: 16rpx;
  min-height: 104rpx;
  padding: 18rpx;
  border-radius: 16rpx;
  background: #F2F7FE;
  border: 2rpx solid transparent;

  &.single {
    margin-bottom: 22rpx;
  }

  &.active {
    border-color: #F9A8D4;
    background: #FFF1F6;
  }

  &__icon {
    width: 58rpx;
    height: 58rpx;
    border-radius: 18rpx;
    display: flex;
    align-items: center;
    justify-content: center;
    flex-shrink: 0;

    &.start {
      background: #F472B6;
    }

    &.end {
      background: #4F86F7;
    }

    &.soft {
      background: #FCE7F3;
    }
  }

  &__text {
    flex: 1;
    min-width: 0;
  }

  &__title {
    display: block;
    font-size: 28rpx;
    color: #2D3748;
    font-weight: 700;
  }

  &__desc {
    display: block;
    margin-top: 6rpx;
    font-size: 23rpx;
    color: #8EA0B8;
  }

  switch {
    transform: scale(0.82);
  }
}

.field-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 16rpx;
}

.field-card,
.field-block {
  &__label {
    display: block;
    margin-bottom: 12rpx;
    font-size: 25rpx;
    color: #4A5568;
    font-weight: 600;
  }
}

.field-list {
  display: flex;
  flex-direction: column;
  gap: 20rpx;
}

.delete-section {
  padding: 20rpx 0 0;
}

.delete-btn {
  width: 100%;
  height: 88rpx;
  line-height: 88rpx;
  color: #EF4444;
  background: #FFFFFF;
  border: 2rpx solid #FECACA;
  border-radius: 16rpx;
  font-size: 28rpx;
}

.bottom-bar {
  position: fixed;
  left: 0;
  right: 0;
  bottom: 0;
  padding: 18rpx 30rpx 24rpx;
  background: rgba(255, 255, 255, 0.96);
  box-shadow: 0 -8rpx 24rpx rgba(0, 0, 0, 0.06);
}

.save-btn {
  width: 100%;
  height: 90rpx;
  line-height: 90rpx;
  color: #FFFFFF;
  background: linear-gradient(135deg, #F472B6 0%, #DB2777 100%);
  border: none;
  border-radius: 18rpx;
  font-size: 30rpx;
  font-weight: 700;
}
</style>
