#!/bin/bash
cd "$(dirname "$0")"

echo "Starting MCP PDF Server on port 8081..."
java -jar target/mcp-pdf-1.0.0.jar