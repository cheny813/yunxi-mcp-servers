#!/bin/bash
# Python Skill MCP Adapter - Linux/Mac 启动脚本

echo "Starting Python Skill MCP Adapter..."

# 检查 Python 版本
python_version=$(python3 --version 2>&1 | awk '{print $2}')
echo "Python version: $python_version"

# 激活虚拟环境（如果存在）
if [ -d "venv" ]; then
    echo "Activating virtual environment..."
    source venv/bin/activate
fi

# 安装依赖（如果需要）
if [ ! -d "venv" ] && [ -f "requirements.txt" ]; then
    echo "Installing dependencies..."
    pip install -r requirements.txt
fi

# 设置环境变量
export BAIDU_API_KEY=${BAIDU_API_KEY:-""}

# 启动服务
echo "Starting MCP server on port 40801..."
python3 main.py --port 40801 --skills-dir "./skills" --log-level INFO