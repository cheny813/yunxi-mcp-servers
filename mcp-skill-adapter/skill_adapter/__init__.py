"""
Python Skill MCP Adapter - 核心框架
"""
__version__ = "1.0.0"

from .base import BaseSkill
from .loader import SkillLoader
from .mcp_server import SkillMcpServer

__all__ = ["BaseSkill", "SkillLoader", "SkillMcpServer"]