#!/bin/bash
# Milvus MCP Server 启动脚本
# 端口: 40103

APP_NAME="mcp-milvus"
JAR_FILE="target/mcp-milvus-1.0.0.jar"

echo "Starting $APP_NAME on port 40103..."

java -Dfile.encoding=UTF-8 -jar $JAR_FILE --server.port=40103
