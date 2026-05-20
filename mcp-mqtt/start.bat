@echo off
chcp 65001 >nul

set PORT=40502

for /f "tokens=5" %%a in ('netstat -ano ^| findstr ":%PORT% " ^| findstr LISTENING') do taskkill /F /PID %%a >nul 2>&1

echo ========================================
echo   MCP MQTT Server
echo   Port: %PORT%
echo   MQTT Broker: tcp://localhost:1883
echo ========================================
echo.

cd /d "%~dp0"
mvn spring-boot:run -Dspring-boot.run.arguments=--server.port=%PORT%
