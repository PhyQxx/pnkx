## 开发

```bash
# 克隆项目
git clone https://gitee.com/phyqxx/pnkx.git

# 进入项目目录
cd pnkx-ui

# 安装依赖
npm install

# 建议不要直接使用 cnpm 安装依赖，会有各种诡异的 bug。可以通过如下操作解决 npm 下载速度慢的问题
npm install --registry=https://registry.npm.taobao.org

# 启动服务
npm run dev
```

浏览器访问 http://localhost:80

## 发布

```bash
# 构建测试环境
npm run build:stage

# 构建生产环境
npm run build:prod
```

## 技术栈

- **框架**: Vue 3 + Element Plus
- **构建**: Vite
- **状态管理**: Vuex
- **路由**: Vue Router
- **HTTP**: Axios（基于若依封装）
- **图标**: 自定义 SVG 图标（中文命名）

## 主要功能模块

- 博客管理：文章、相册、视频、留言、友链
- AI 智能助手：SSE 流式对话、意图识别
- 生活管理：记账、日记、待办
- 系统管理：用户、角色、菜单、字典
- 文件管理：分片上传、进度显示