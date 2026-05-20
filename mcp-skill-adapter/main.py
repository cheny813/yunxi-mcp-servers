#!/usr/bin/env python3
"""
Python Skill MCP Adapter - 主入口
"""
import os
import sys
import json
import logging
import argparse
from pathlib import Path
from http.server import HTTPServer, BaseHTTPRequestHandler
from threading import Thread

# 添加当前目录到 Python 路径
sys.path.insert(0, str(Path(__file__).parent))

from skill_adapter import SkillLoader, SkillMcpServer

# 配置日志
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s'
)
logger = logging.getLogger(__name__)


class McpRequestHandler(BaseHTTPRequestHandler):
    """MCP HTTP 请求处理器"""

    server_version = "yunxi-skill-adapter/1.0.0"

    def do_GET(self):
        """处理 GET 请求"""
        if self.path == "/mcp/tools":
            self.send_json_response(self.server.mcp_server.list_tools())
        
        elif self.path == "/mcp/health":
            self.send_json_response({"status": "UP"})
        
        elif self.path == "/mcp/info":
            self.send_json_response(self.server.mcp_server.get_server_info())
        
        else:
            self.send_error(404)

    def do_POST(self):
        """处理 POST 请求"""
        if self.path == "/mcp":
            content_length = int(self.headers.get("Content-Length", 0))
            request_body = self.rfile.read(content_length).decode("utf-8")
            
            response = self.server.mcp_server.handle_request(request_body)
            self.send_response(200)
            self.send_header("Content-Type", "application/json")
            self.end_headers()
            self.wfile.write(response.encode("utf-8"))
        
        elif self.path == "/mcp/message":
            # SSE 消息处理
            content_length = int(self.headers.get("Content-Length", 0))
            request_body = self.rfile.read(content_length).decode("utf-8")
            
            if hasattr(self.server.mcp_server, "handle_sse_message"):
                response = self.server.mcp_server.handle_sse_message(request_body)
                self.send_json_response(response)
            else:
                self.send_error(501)
        
        else:
            self.send_error(404)

    def send_json_response(self, data: dict):
        """发送 JSON 响应"""
        self.send_response(200)
        self.send_header("Content-Type", "application/json")
        self.end_headers()
        self.wfile.write(json.dumps(data, ensure_ascii=False).encode("utf-8"))

    def log_message(self, format, *args):
        """自定义日志格式"""
        logger.debug(f"{self.client_address[0]} - {format % args}")


class McpHTTPServer(HTTPServer):
    """MCP HTTP 服务器"""

    def __init__(self, port: int, mcp_server: SkillMcpServer):
        super().__init__(("", port), McpRequestHandler)
        self.mcp_server = mcp_server
        logger.info(f"MCP HTTP Server initialized on port {port}")


def parse_args():
    """解析命令行参数"""
    parser = argparse.ArgumentParser(description="Python Skill MCP Adapter")
    parser.add_argument("--port", type=int, default=8090, help="Server port (default: 8090)")
    parser.add_argument("--host", type=str, default="0.0.0.0", help="Server host (default: 0.0.0.0)")
    parser.add_argument("--skills-dir", type=str, default="./skills", help="Skills directory")
    parser.add_argument("--log-level", type=str, default="INFO", 
                        choices=["DEBUG", "INFO", "WARNING", "ERROR"], 
                        help="Log level")
    return parser.parse_args()


def main():
    """主函数"""
    args = parse_args()
    
    # 设置日志级别
    logging.getLogger().setLevel(getattr(logging, args.log_level))
    
    # 加载 Skills
    logger.info(f"Loading skills from: {args.skills_dir}")
    loader = SkillLoader(args.skills_dir)
    skills = loader.load_all()
    
    # 创建 MCP 服务器
    mcp_server = SkillMcpServer()
    mcp_server.initialize(loader)
    
    # 启动服务器
    server = McpHTTPServer(args.port, mcp_server)
    
    logger.info(f"=" * 50)
    logger.info(f"Python Skill MCP Adapter Started")
    logger.info(f"Server: http://{args.host}:{args.port}")
    logger.info(f"Skills loaded: {len(skills)}")
    logger.info(f"Endpoints:")
    logger.info(f"  - POST /mcp           (JSON-RPC)")
    logger.info(f"  - GET  /mcp/tools     (Tool list)")
    logger.info(f"  - GET  /mcp/health    (Health check)")
    logger.info(f"=" * 50)
    
    try:
        server.serve_forever()
    except KeyboardInterrupt:
        logger.info("Shutting down...")
        server.shutdown()


if __name__ == "__main__":
    main()