#!/bin/bash
# 设置变量
CONTAINER_NAME="pnkx-admin"
echo "开始构建镜像"
docker build -t pnkx-admin .
echo "构建镜像结束"
# 检查容器是否正在运行
if docker ps --filter "name=$CONTAINER_NAME" --format "{{.Names}}" | grep -q "^$CONTAINER_NAME$"; then
    echo "$CONTAINER_NAME 容器正在运行，现在停止并删除..."
    # 停止并删除容器
    docker stop $CONTAINER_NAME
    docker rm $CONTAINER_NAME
    echo "$CONTAINER_NAME 容器已停止并删除。"
fi

# 检查是否有停止的容器（未运行），如果有则删除
if docker ps -a --filter "name=$CONTAINER_NAME" --format "{{.Names}}" | grep -q "^$CONTAINER_NAME$"; then
    echo "$CONTAINER_NAME 容器存在但未运行，现在删除..."
    # 删除停止的容器
    docker rm $CONTAINER_NAME
    echo "$CONTAINER_NAME 容器已删除。"
fi
# 启动新的容器
echo "启动新的 $CONTAINER_NAME 容器..."
docker run --name pnkx-admin -p 8068:8068 -d pnkx-admin -v /volume3/docker/pnkx/uploadPath:/app/uploadPath
echo "运行容器成功"
