@echo off
chcp 65001 >nul

set PORT=40302

for /f "tokens=5" %%a in ('netstat -ano ^| findstr ":%PORT% " ^| findstr LISTENING') do taskkill /F /PID %%a >nul 2>&1

echo ========================================
echo   MCP Chart Server
echo   Port: %PORT%
echo ========================================
echo.

cd /d "%~dp0"
mvn spring-boot:run -Dspring-boot.run.arguments=--server.port=%PORT%
