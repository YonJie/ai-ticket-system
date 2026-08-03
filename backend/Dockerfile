# 多阶段构建：在镜像内完成 Maven 打包，便于 Vercel / 本地 Docker 一键构建
FROM maven:3.9-eclipse-temurin-8 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn -q -DskipTests package

# 运行阶段：轻量 JRE
FROM eclipse-temurin:8-jre-alpine
WORKDIR /app
COPY --from=build /app/target/*.jar /app.jar

# Vercel / 容器平台会注入 PORT；本地默认 8080
ENV PORT=8080
EXPOSE 8080

ENTRYPOINT ["sh", "-c", "java -jar /app.jar --server.port=${PORT}"]
