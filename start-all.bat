@echo off
chcp 65001 >nul
echo Starting all MCP servers...
echo.

cd mcp-database && start cmd /k start.bat
timeout /t 2 /nobreak >nul

cd ..\mcp-filesystem && start cmd /k start.bat
timeout /t 2 /nobreak >nul

cd ..\mcp-redis && start cmd /k start.bat
timeout /t 2 /nobreak >nul

cd ..\mcp-github && start cmd /k start.bat
timeout /t 2 /nobreak >nul

cd ..\mcp-git && start cmd /k start.bat
timeout /t 2 /nobreak >nul

cd ..\mcp-s3 && start cmd /k start.bat
timeout /t 2 /nobreak >nul

cd ..\mcp-mongodb && start cmd /k start.bat
timeout /t 2 /nobreak >nul

cd ..\mcp-elasticsearch && start cmd /k start.bat
timeout /t 2 /nobreak >nul

cd ..\mcp-docker && start cmd /k start.bat
timeout /t 2 /nobreak >nul

cd ..\mcp-k8s && start cmd /k start.bat
timeout /t 2 /nobreak >nul

cd ..\mcp-email && start cmd /k start.bat
timeout /t 2 /nobreak >nul

cd ..\mcp-wikipedia && start cmd /k start.bat

cd ..
echo.
echo All MCP servers started!
pause
