@echo off
chcp 65001 >nul

set PORT=40104

for /f "tokens=5" %%a in ('netstat -ano ^| findstr ":%PORT% " ^| findstr LISTENING') do taskkill /F /PID %%a >nul 2>&1

cd /d %~dp0

echo Starting MCP PPTX Server on port %PORT%...
java -Dfile.encoding=UTF-8 -jar target\mcp-pptx-1.0.0.jar --server.port=%PORT%
