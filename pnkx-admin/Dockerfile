FROM eclipse-temurin:8-jre-alpine
LABEL authors="Pei你看雪"

# 设置工作目录
WORKDIR /app

# 将构建好的 JAR 文件复制到镜像中（注意替换 YOUR_JAR_NAME.jar）
COPY pnkx-admin/target/pnkx-admin.jar app.jar

# 暴露 Spring Boot 服务端口（根据实际项目修改）
EXPOSE 8068

# 运行 JAR 文件
ENTRYPOINT ["sh", "-c", "java ${JAVA_OPTS} -jar /app/app.jar"]
