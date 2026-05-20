#!/bin/bash
PORT=40509

PID=$(lsof -ti:$PORT 2>/dev/null)
if [ -n "$PID" ]; then
    echo "端口 $PORT 被占用(PID: $PID)，正在终止进程..."
    kill -9 $PID 2>/dev/null
    sleep 2
fi

cd target
java -Dfile.encoding=UTF-8 -jar mcp-git-1.0.0.jar --server.port=$PORT
