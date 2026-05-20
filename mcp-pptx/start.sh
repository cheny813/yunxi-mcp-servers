#!/bin/bash
PORT=40104

PID=$(lsof -ti:$PORT 2>/dev/null)
if [ -n "$PID" ]; then
    echo "端口 $PORT 被占用(PID: $PID)，正在终止进程..."
    kill -9 $PID 2>/dev/null
    sleep 2
fi

cd "$(dirname "$0")"

echo "Starting MCP PPTX Server on port $PORT..."
java -jar target/mcp-pptx-1.0.0.jar --server.port=$PORT