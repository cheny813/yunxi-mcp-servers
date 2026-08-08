#!/bin/bash
cd "$(dirname "$0")"

echo "========================================"
echo "  Start FormFill MCP Server (Port: 3003)"
echo "========================================"

java -Dfile.encoding=UTF-8 -jar target/mcp-formfill-1.0.0.jar
