import mitt from 'mitt'

const emitter = mitt()

export default {
    install(app) {
        // 这里的Appid是腾讯工作台配置
        let Captcha = new TencentCaptcha('2053242547', res => {
            emitter.emit('getTicket', res)
        });
        app.config.globalProperties.$Captcha = Captcha
        app.config.globalProperties.$captchaBus = emitter
    }
}
