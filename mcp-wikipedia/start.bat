@echo off
chcp 65001 >nul

set PORT=40510

for /f "tokens=5" %%a in ('netstat -ano ^| findstr ":%PORT% " ^| findstr LISTENING') do taskkill /F /PID %%a >nul 2>&1

cd target
java -Dfile.encoding=UTF-8 -jar mcp-wikipedia-1.0.0.jar --server.port=%PORT%
