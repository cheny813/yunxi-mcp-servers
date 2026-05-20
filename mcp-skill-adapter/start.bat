@echo off
chcp 65001 >nul
echo Starting Python Skill MCP Adapter on port 40801...

REM 检查 Python
python --version >nul 2>&1
if errorlevel 1 (
    echo Error: Python not found
    pause
    exit /b 1
)

REM 安装依赖
echo Installing dependencies...
pip install -r requirements.txt >nul 2>&1

REM 设置环境变量
set BAIDU_API_KEY=%BAIDU_API_KEY%

REM 启动服务
echo Starting MCP server...
python main.py --port 40801 --skills-dir "./skills" --log-level INFO
pause