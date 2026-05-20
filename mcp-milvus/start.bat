@echo off
chcp 65001 >nul
echo Starting mcp-milvus on port 40103...
for /f "tokens=5" %%a in ('netstat -ano ^| findstr ":40103" ^| findstr "LISTENING"') do taskkill /F /PID %%a >nul 2>&1
java -Dfile.encoding=UTF-8 -jar target\mcp-milvus-1.0.0.jar --server.port=40103
pause
