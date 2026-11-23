<template>
    <div class="voice-input">
        <div class="voice-input__controls">
            <button type="button" :disabled="disabled" :aria-label="active ? '结束语音识别' : '开始语音识别'"
                :aria-pressed="active" @click="toggle">
                <van-icon :name="active ? 'stop-circle-o' : 'volume-o'" />
                <span v-if="active">结束</span>
            </button>
            <button v-if="active" type="button" @click="cancel">取消</button>
        </div>
        <p v-if="status" role="status">{{ status }}</p>
    </div>
</template>

<script>
export default {
    name: 'VoiceInput',
    props: { disabled: Boolean },
    emits: ['result', 'busy'],
    data: () => ({ active: false, status: '', recognition: null, timer: null }),
    beforeUnmount() { this.cancel() },
    methods: {
        cancel() {
            const recognition = this.recognition
            this.recognition = null
            clearTimeout(this.timer)
            if (recognition) {
                recognition.onresult = recognition.onerror = recognition.onend = null
                recognition.abort()
            }
            this.active = false
            this.status = ''
            this.$emit('busy', false)
        },
        toggle() {
            if (this.active) {
                this.status = '正在完成识别…'
                this.recognition.stop()
                return
            }
            const Recognition = window.SpeechRecognition || window.webkitSpeechRecognition
            if (!window.isSecureContext || !Recognition) {
                this.status = '当前浏览器无法使用语音识别，请使用支持语音识别的浏览器并通过 HTTPS 访问。'
                return
            }
            const recognition = new Recognition()
            this.recognition = recognition
            recognition.lang = 'zh-CN'
            recognition.continuous = true
            recognition.interimResults = true
            let transcript = ''
            let failed = false
            recognition.onresult = (event) => {
                transcript = Array.from(event.results).filter(result => result.isFinal)
                    .map(result => result[0].transcript).join('')
                const preview = Array.from(event.results).map(result => result[0].transcript).join('')
                this.status = preview || '正在聆听，请说话…'
            }
            recognition.onerror = (event) => {
                failed = true
                const messages = {
                    'not-allowed': '麦克风权限被拒绝，请在浏览器设置中允许使用麦克风。',
                    'audio-capture': '无法使用麦克风，请检查设备。',
                    'no-speech': '未识别到语音，请重试。',
                    network: '语音识别服务连接失败，请检查网络后重试。'
                }
                this.status = messages[event.error] || '语音识别失败，请重试或输入文字。'
            }
            recognition.onend = () => {
                clearTimeout(this.timer)
                this.recognition = null
                this.active = false
                this.$emit('busy', false)
                if (transcript.trim()) {
                    this.$emit('result', transcript.trim())
                    this.status = '识别文字已填入输入框，请确认后发送。'
                } else if (!failed) this.status = '未识别到语音，请重试。'
            }
            try {
                recognition.start()
                this.active = true
                this.$emit('busy', true)
                this.status = '正在聆听，请说话；点击结束后确认文字。'
                this.timer = setTimeout(() => recognition.stop(), 60000)
            } catch {
                this.cancel()
                this.status = '无法启动语音识别，请重试。'
            }
        }
    }
}
</script>

<style scoped>
.voice-input { padding: 6px 12px 0; }
.voice-input__controls { display: flex; gap: 8px; }
button { min-height: 36px; padding: 0 12px; border: 1px solid var(--pnkx-border); border-radius: 18px; background: var(--pnkx-surface-muted); color: var(--pnkx-primary); }
button:disabled { opacity: .4; }
p { margin: 6px 0; font-size: 12px; color: var(--pnkx-text-secondary); overflow-wrap: anywhere; max-height: 64px; overflow: auto; }
</style>
