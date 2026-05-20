#!/bin/bash

# 启动 MCP Redis 服务
# 默认端口：40102
# 使用方式：./start.sh [端口]

PORT=${1:-40102}

echo "启动 MCP Redis 服务，端口: $PORT"
echo

PID=$(lsof -ti:$PORT 2>/dev/null)
if [ -n "$PID" ]; then
    echo "端口 $PORT 被占用(PID: $PID)，正在终止进程..."
    kill -9 $PID 2>/dev/null
    sleep 2
fi

java -Dfile.encoding=UTF-8 -jar mcp-redis-1.0.0.jar --server.port=$PORT
