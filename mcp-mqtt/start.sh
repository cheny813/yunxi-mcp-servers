#!/bin/bash
PORT=40502

PID=$(lsof -ti:$PORT 2>/dev/null)
if [ -n "$PID" ]; then
    echo "端口 $PORT 被占用(PID: $PID)，正在终止进程..."
    kill -9 $PID 2>/dev/null
    sleep 2
fi

echo "========================================"
echo "  MCP MQTT Server 启动脚本"
echo "  端口: $PORT"
echo "  MQTT Broker: tcp://localhost:1883"
echo "========================================"
echo ""

cd "$(dirname "$0")"
mvn spring-boot:run -Dspring-boot.run.arguments=--server.port=$PORT
