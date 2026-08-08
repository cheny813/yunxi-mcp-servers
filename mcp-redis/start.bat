@echo off
chcp 65001 >nul
set PORT=40102

REM 杀掉占用该端口的已有进程（LISTENING 状态）
for /f "tokens=5" %%a in ('netstat -ano ^| findstr ":%PORT% " ^| findstr LISTENING') do (
    taskkill /F /PID %%a >nul 2>&1
)

cd target
java -Dfile.encoding=UTF-8 -jar mcp-redis-1.0.0.jar --server.port=%PORT%
pause
