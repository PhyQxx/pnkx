// 应用全局配置
// API 地址经 .env.development / .env.production 注入（Vite 环境变量），
// 本地调试不再靠注释切换；import.meta.env 在个别构建链路不可用时兜底生产地址
export default {
  baseUrl: import.meta.env.VITE_API_BASE_URL || 'https://admin.pnkx.top:8/prod-api',
  // 应用信息
  appInfo: {
    // 应用名称
    name: "pnkx-app",
    // 应用logo
    logo: "/static/logo.png",
    // 官方网站
    site_url: "https://pnkx.top",
    // 政策协议
    agreements: [{
      title: "隐私政策",
      url: "https://pnkx.top"
    },
    {
      title: "用户服务协议",
      url: "https://pnkx.top"
    }
    ]
  }
}
