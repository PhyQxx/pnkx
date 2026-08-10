# 基础镜像
FROM node:18.16.0 AS build-stage

# 创建工作目录
RUN mkdir -p /app
WORKDIR /app

# 复制项目文件和目录到容器中
COPY . /app

# 安装pm2
RUN npm install pm2 -g --registry https://registry.npmmirror.com

# 安装依赖项并构建应用程序
RUN npm install --registry https://registry.npmmirror.com && \
    npm run build && \
    npm cache clean --force


# 设置环境变量
ENV NITRO_PORT=3000

# 暴露端口
EXPOSE 3000

# 将应用程序添加到 PM2
RUN pm2 start ecosystem.config.js --only pnkx-client

# 保存 PM2 进程列表
RUN pm2 save

# 设置入口点为启动脚本
CMD ["pm2", "start", "ecosystem.config.js", "--no-daemon"]
