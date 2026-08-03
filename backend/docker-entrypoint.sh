#!/usr/bin/env bash
# Vercel / 容器入口：监听平台注入的 PORT（默认 80）
set -euo pipefail

PORT="${PORT:-80}"
JAVA_BIN="${JAVA_HOME:-/opt/java/openjdk}/bin/java"

echo "[entrypoint] JAVA_BIN=${JAVA_BIN} PORT=${PORT}"
exec "${JAVA_BIN}" -jar /app/app.jar --server.port="${PORT}" --server.address=0.0.0.0
