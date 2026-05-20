#!/bin/bash
echo "Starting mcp-baidu-search on port 8086..."

# Kill existing process on port 8086
lsof -ti:8086 | xargs -r kill -9

# Start the application
java -Dfile.encoding=UTF-8 -jar target/mcp-baidu-search-1.0.0.jar --server.port=8086