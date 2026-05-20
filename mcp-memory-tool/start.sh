#!/bin/bash

# MCP Memory Tool Server 启动脚本（Linux/Mac）

echo "========================================"
echo "MCP Memory Tool Server 启动脚本"
echo "========================================"

# 设置默认配置
MCP_PORT=${MCP_PORT:-40304}
MCP_HOST=${MCP_HOST:-localhost}
LOG_LEVEL=${LOG_LEVEL:-INFO}
MEMORY_STORAGE_PATH_DEV=${MEMORY_STORAGE_PATH_DEV:-./data/memory}
MEMORY_STORAGE_PATH_PROD=${MEMORY_STORAGE_PATH_PROD:-/shared/data/memory}
ENVIRONMENT=${ENVIRONMENT:-prod}

# 存储引擎配置
MEMORY_STORE_TYPE=${MEMORY_STORE_TYPE:-file}
MILVUS_ENABLED=${MILVUS_ENABLED:-false}
EMBEDDING_PROVIDER=${EMBEDDING_PROVIDER:-mock}

# 检查 JAR 文件是否存在
if [ ! -f "target/mcp-memory-tool-1.0.0.jar" ]; then
    echo "错误：未找到 mcp-memory-tool-1.0.0.jar"
    echo "请先运行：mvn clean package"
    exit 1
fi

# 创建数据目录
if [ "$ENVIRONMENT" == "dev" ]; then
    if [ ! -d "$MEMORY_STORAGE_PATH_DEV" ]; then
        echo "创建数据目录: $MEMORY_STORAGE_PATH_DEV"
        mkdir -p "$MEMORY_STORAGE_PATH_DEV"
    fi
else
    echo "生产模式：使用共享存储 $MEMORY_STORAGE_PATH_PROD"
    echo "请确保共享存储已挂载并可用"
fi

# 检查端口是否被占用
if lsof -Pi :$MCP_PORT -sTCP:LISTEN -t >/dev/null ; then
    echo "警告：端口 $MCP_PORT 已被占用"
    echo "请停止占用该端口的进程或修改配置"
    read -p "是否继续启动？(y/n): " -n 1 -r
    echo
    [[ $REPLY =~ ^[Yy]$ ]] || exit 1
fi

echo "启动 MCP Memory Tool Server..."
echo "端口：$MCP_PORT"
echo "主机：$MCP_HOST"
echo "数据目录：$MEMORY_STORAGE_PATH"
echo "========================================"
echo "可用端点："
echo "- 健康检查: http://$MCP_HOST:$MCP_PORT/mcp/health"
echo "- 服务器信息: http://$MCP_HOST:$MCP_PORT/mcp/info"
echo "- 工具列表: http://$MCP_HOST:$MCP_PORT/mcp/tools"
echo "- SSE 连接: http://$MCP_HOST:$MCP_PORT/mcp/sse"
echo "========================================"
echo

# 启动应用
java -Dserver.port=$MCP_PORT \
     -Dspring.profiles.active=$ENVIRONMENT \
     -Dyunxi.environment.mode=$ENVIRONMENT \
     -Dyunxi.learning-loop.memory.storage-path-dev=$MEMORY_STORAGE_PATH_DEV \
     -Dyunxi.learning-loop.memory.storage-path-prod=$MEMORY_STORAGE_PATH_PROD \
     -Dmemory.store.type=$MEMORY_STORE_TYPE \
     -Dmilvus.enabled=$MILVUS_ENABLED \
     -Dembedding.provider=$EMBEDDING_PROVIDER \
     -Dlogging.level.io.yunxi.mcp.memory=$LOG_LEVEL \
     -jar target/mcp-memory-tool-1.0.0.jar

# 检查启动是否成功
if [ $? -eq 0 ]; then
    echo "========================================"
    echo "MCP Memory Tool Server 启动成功！"
    echo "访问健康检查：http://$MCP_HOST:$MCP_PORT/mcp/health"
    echo "========================================"
else
    echo "========================================"
    echo "应用启动失败，退出代码：$?"
    echo "========================================"
    exit 1
fi
