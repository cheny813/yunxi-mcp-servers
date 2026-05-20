@echo off
chcp 65001 >nul
echo Starting mcp-baidu-search on port 40204...
for /f "tokens=5" %%a in ('netstat -ano ^| findstr ":40204" ^| findstr "LISTENING"') do taskkill /F /PID %%a >nul 2>&1
java -Dfile.encoding=UTF-8 -jar target\mcp-baidu-search-1.0.0.jar --server.port=40204
pause