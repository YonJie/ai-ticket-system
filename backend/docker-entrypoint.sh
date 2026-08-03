#!/usr/bin/env bash
# Vercel 容器入口：
# 1) 立刻在 $PORT 上 accept TCP（满足平台约 15s 启动探测）
# 2) Spring Boot 绑定内部端口，由 socat 转发（Java 冷启动常超过 15s）
set -euo pipefail

PORT="${PORT:-80}"
INTERNAL_PORT="${INTERNAL_PORT:-8080}"
JAVA_BIN="${JAVA_HOME:-/opt/java/openjdk}/bin/java"

echo "[entrypoint] JAVA_BIN=${JAVA_BIN} public_port=${PORT} internal_port=${INTERNAL_PORT}"

# 立即监听对外 PORT；对内部端口重试，直到 Spring 真正就绪
socat TCP-LISTEN:"${PORT}",fork,reuseaddr,bind=0.0.0.0 TCP:127.0.0.1:"${INTERNAL_PORT}",retry=120,interval=0.25 &
SOCAT_PID=$!
echo "[entrypoint] socat started pid=${SOCAT_PID}"

# 退出时清理转发进程
cleanup() {
  kill "${SOCAT_PID}" 2>/dev/null || true
}
trap cleanup EXIT

exec "${JAVA_BIN}" \
  -XX:TieredStopAtLevel=1 \
  -Xms128m -Xmx512m \
  -jar /app/app.jar \
  --server.port="${INTERNAL_PORT}" \
  --server.address=127.0.0.1
