"""
MCP Server - 基于 Model Context Protocol 的服务器实现
"""
import asyncio
import json
import logging
from typing import Any, Dict, Optional

from .base import BaseSkill
from .loader import SkillLoader

logger = logging.getLogger(__name__)


class McpMethod:
    """MCP 方法名常量"""
    TOOLS_LIST = "tools/list"
    TOOLS_CALL = "tools/call"
    INITIALIZE = "initialize"


class SkillMcpServer:
    """Skill MCP 服务器"""

    def __init__(self, name: str = "yunxi-skill-adapter", version: str = "1.0.0"):
        """
        初始化 MCP 服务器
        
        Args:
            name: 服务器名称
            version: 服务器版本
        """
        self.name = name
        self.version = version
        self.skill_loader: Optional[SkillLoader] = None
        self._server_info = {
            "name": name,
            "version": version
        }

    def initialize(self, skill_loader: SkillLoader):
        """
        初始化服务器
        
        Args:
            skill_loader: Skill 加载器
        """
        self.skill_loader = skill_loader
        logger.info(f"MCP Server initialized: {self.name} v{self.version}")

    def get_server_info(self) -> Dict:
        """获取服务器信息"""
        return self._server_info

    def list_tools(self) -> Dict:
        """
        列出所有可用工具
        
        Returns:
            工具列表响应
        """
        if not self.skill_loader:
            return {"tools": []}

        tools = self.skill_loader.get_all_skill_definitions()
        logger.debug(f"Listing {len(tools)} tools")
        
        return {"tools": tools}

    def call_tool(self, name: str, arguments: Dict) -> Dict:
        """
        调用工具
        
        Args:
            name: 工具名称
            arguments: 工具参数
            
        Returns:
            工具执行结果
        """
        if not self.skill_loader:
            return {"error": {"code": -32603, "message": "Server not initialized"}}

        skill = self.skill_loader.get_skill(name)
        if not skill:
            return {"error": {"code": -32602, "message": f"Tool not found: {name}"}}

        try:
            logger.info(f"Calling tool: {name} with args: {arguments}")
            result = skill.execute(**arguments)
            
            # 格式化结果
            if isinstance(result, dict) and "error" in result:
                return {"error": result["error"]}
            
            # 转换为文本格式
            formatted = skill.format_result(result)
            return {"content": [{"type": "text", "text": formatted}]}
            
        except Exception as e:
            logger.error(f"Tool execution failed: {name} - {e}")
            return {"error": {"code": -32603, "message": str(e)}}

    def handle_request(self, request_json: str) -> str:
        """
        处理 JSON-RPC 请求
        
        Args:
            request_json: JSON-RPC 请求字符串
            
        Returns:
            JSON-RPC 响应字符串
        """
        try:
            request = json.loads(request_json)
        except json.JSONDecodeError as e:
            return self._error_response(None, -32700, f"Parse error: {e}")

        method = request.get("method")
        request_id = request.get("id")

        # 处理请求
        try:
            if method == McpMethod.TOOLS_LIST:
                result = self.list_tools()
                return self._success_response(request_id, result)
            
            elif method == McpMethod.TOOLS_CALL:
                params = request.get("params", {})
                tool_name = params.get("name")
                arguments = params.get("arguments", {})
                result = self.call_tool(tool_name, arguments)
                return self._success_response(request_id, result)
            
            elif method == McpMethod.INITIALIZE:
                result = self.get_server_info()
                return self._success_response(request_id, result)
            
            else:
                return self._error_response(request_id, -32601, f"Method not found: {method}")
        
        except Exception as e:
            logger.error(f"Request handling error: {e}")
            return self._error_response(request_id, -32603, str(e))

    def _success_response(self, request_id: Any, result: Any) -> str:
        """构建成功响应"""
        response = {
            "jsonrpc": "2.0",
            "id": request_id,
            "result": result
        }
        return json.dumps(response, ensure_ascii=False)

    def _error_response(self, request_id: Any, code: int, message: str) -> str:
        """构建错误响应"""
        response = {
            "jsonrpc": "2.0",
            "id": request_id,
            "error": {
                "code": code,
                "message": message
            }
        }
        return json.dumps(response, ensure_ascii=False)


class SseMcpServer(SkillMcpServer):
    """支持 SSE 的 MCP 服务器"""

    def __init__(self, name: str = "yunxi-skill-adapter", version: str = "1.0.0"):
        super().__init__(name, version)
        self._sessions = {}

    async def handle_sse_message(self, message_json: str) -> str:
        """处理 SSE 消息"""
        return self.handle_request(message_json)