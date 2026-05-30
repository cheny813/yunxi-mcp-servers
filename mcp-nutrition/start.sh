#!/bin/bash

# 启动 MCP Nutrition Server
# 默认端口：40112
# 使用方式：./start.sh [端口]

PORT=${1:-40602}

echo "启动 MCP Nutrition Server，端口: $PORT"
echo

PID=$(lsof -ti:$PORT 2>/dev/null)
if [ -n "$PID" ]; then
    echo "端口 $PORT 被占用(PID: $PID)，正在终止进程..."
    kill -9 $PID 2>/dev/null
    sleep 2
fi

java -Dfile.encoding=UTF-8 -jar target/mcp-nutrition-1.0.0.jar --server.port=$PORT