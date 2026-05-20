@echo off
chcp 65001 >nul
echo ========================================
echo   启动表单填写 MCP 服务器
echo ========================================
cd /d "%~dp0"
java -Dfile.encoding=UTF-8 -jar target/mcp-formfill-1.0.0.jar
pause
