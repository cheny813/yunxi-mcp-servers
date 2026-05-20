@echo off
chcp 65001 >nul
cd /d %~dp0

set PORT=8081

for /f "tokens=5" %%a in ('netstat -ano ^| findstr ":%PORT% " ^| findstr LISTENING') do taskkill /F /PID %%a >nul 2>&1

echo Starting MCP PDF Server on port %PORT%...
java -jar target\mcp-pdf-1.0.0.jar --server.port=%PORT%
