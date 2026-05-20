@echo off
chcp 65001 >nul
echo Stopping all MCP servers...
taskkill /F /FI "WINDOWTITLE eq mcp-database*" 2>nul
taskkill /F /FI "WINDOWTITLE eq mcp-filesystem*" 2>nul
taskkill /F /FI "WINDOWTITLE eq mcp-redis*" 2>nul
taskkill /F /FI "WINDOWTITLE eq mcp-github*" 2>nul
taskkill /F /FI "WINDOWTITLE eq mcp-git*" 2>nul
taskkill /F /FI "WINDOWTITLE eq mcp-s3*" 2>nul
taskkill /F /FI "WINDOWTITLE eq mcp-mongodb*" 2>nul
taskkill /F /FI "WINDOWTITLE eq mcp-elasticsearch*" 2>nul
taskkill /F /FI "WINDOWTITLE eq mcp-docker*" 2>nul
taskkill /F /FI "WINDOWTITLE eq mcp-k8s*" 2>nul
taskkill /F /FI "WINDOWTITLE eq mcp-email*" 2>nul
taskkill /F /FI "WINDOWTITLE eq mcp-wikipedia*" 2>nul
echo All MCP servers stopped.
pause
