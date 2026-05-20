#!/bin/bash
cd "$(dirname "$0")"

echo "Starting MCP XLSX Server on port 8082..."
java -jar target/mcp-xlsx-1.0.0.jar