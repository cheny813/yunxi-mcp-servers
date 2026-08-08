chcp 65001 >nul 2>&1
@echo off
echo ========================================
echo   Start FormFill MCP Server (Port: 3003)
echo ========================================
cd /d "%~dp0"
java -Dfile.encoding=UTF-8 -jar target/mcp-formfill-1.0.0.jar
pause
