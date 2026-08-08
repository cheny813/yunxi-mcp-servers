@echo off
chcp 65001 >nul
cd /d %~dp0
set PORT=8082

for /f "tokens=5" %%a in ('netstat -ano ^| findstr ":%PORT% " ^| findstr LISTENING') do taskkill /F /PID %%a >nul 2>&1

echo Starting MCP XLSX Server on port %PORT%...
java -jar target\mcp-xlsx-1.0.0.jar
pause
