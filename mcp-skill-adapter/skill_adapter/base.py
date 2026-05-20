"""
Base Skill 类 - 所有 Skill 的基类
"""
import os
import json
import logging
import re
import ast
from abc import ABC, abstractmethod
from typing import Any, Dict, Optional, List, Tuple

logger = logging.getLogger(__name__)


class BaseSkill(ABC):
    """所有 Skill 的基类"""

    # Skill 元数据（子类必须覆盖）
    name: str = "base_skill"
    description: str = "Base skill"
    version: str = "1.0.0"

    def __init__(self, config: Optional[Dict] = None):
        """
        初始化 Skill
        
        Args:
            config: Skill 配置
        """
        self.config = config or {}
        self._env = os.environ.copy()
        self._initialize()

    def _initialize(self):
        """子类可以重写的初始化方法"""
        pass

    @abstractmethod
    def execute(self, **kwargs) -> Dict[str, Any]:
        """
        执行 Skill 逻辑
        
        Args:
            **kwargs: 工具参数
            
        Returns:
            执行结果字典
        """
        pass

    def get_definition(self) -> Dict[str, Any]:
        """
        获取 MCP 工具定义
        
        Returns:
            工具定义字典
        """
        return {
            "name": self.name,
            "description": self.description,
            "inputSchema": self.get_input_schema()
        }

    def get_input_schema(self) -> Dict[str, Any]:
        """
        获取输入参数 Schema（子类可重写）
        
        Returns:
            JSON Schema
        """
        return {
            "type": "object",
            "properties": {},
            "required": []
        }

    def get_env(self, key: str, default: str = "") -> str:
        """
        获取环境变量
        
        Args:
            key: 环境变量名
            default: 默认值
            
        Returns:
            环境变量值
        """
        return self._env.get(key, os.environ.get(key, default))

    def format_result(self, result: Any, format: str = "text") -> str:
        """
        格式化结果输出
        
        Args:
            result: 原始结果
            format: 输出格式
            
        Returns:
            格式化后的字符串
        """
        if isinstance(result, dict):
            if format == "json":
                return json.dumps(result, ensure_ascii=False, indent=2)
            else:
                # 转换为人类可读文本
                lines = []
                for k, v in result.items():
                    if isinstance(v, (list, dict)):
                        lines.append(f"{k}: {json.dumps(v, ensure_ascii=False)}")
                    else:
                        lines.append(f"{k}: {v}")
                return "\n".join(lines)
        elif isinstance(result, str):
            return result
        else:
            return str(result)

    def error(self, message: str) -> Dict[str, Any]:
        """
        返回错误结果
        
        Args:
            message: 错误消息
            
        Returns:
            错误结果字典
        """
        logger.error(f"[{self.name}] Error: {message}")
        return {
            "error": True,
            "message": message
        }

    def success(self, data: Any = None, message: str = "Success") -> Dict[str, Any]:
        """
        返回成功结果
        
        Args:
            data: 结果数据
            message: 成功消息
            
        Returns:
            成功结果字典
        """
        return {
            "success": True,
            "message": message,
            "data": data
        }


def parse_skill_markdown(markdown_file: str) -> Dict[str, Any]:
    """
    从 SKILL.md 文件解析技能元数据和参数Schema
    
    Args:
        markdown_file: SKILL.md 文件路径
        
    Returns:
        技能定义字典
    """
    try:
        with open(markdown_file, 'r', encoding='utf-8') as f:
            content = f.read()
            
        # 解析基本元数据
        metadata = {}
        
        # 提取技能名称
        name_match = re.search(r'#\s+(.+?)\s*\n', content)
        if name_match:
            metadata['name'] = name_match.group(1).strip()
            
        # 提取描述
        desc_match = re.search(r'##\s*描述\s*\n(.+?)(?=##|$)', content, re.DOTALL)
        if desc_match:
            metadata['description'] = desc_match.group(1).strip()
            
        # 提取参数信息
        schema = {
            "type": "object",
            "properties": {},
            "required": []
        }
        
        # 解析参数表格
        params_section_match = re.search(r'##\s*参数\s*\n(.+?)(?=##|$)', content, re.DOTALL)
        if params_section_match:
            params_text = params_section_match.group(1)
            
            # 解析markdown表格
            table_match = re.search(r'\|\s*参数名\s*\|\s*类型\s*\|\s*描述\s*\|\s*默认值\s*\|\s*必填\s*\|', params_text)
            if table_match:
                # 查找表格行
                rows = re.findall(r'\|\s*([^|]+?)\s*\|\s*([^|]+?)\s*\|\s*([^|]+?)\s*\|\s*([^|]+?)\s*\|\s*([^|]+?)\s*\|', params_text)
                for row in rows:
                    if len(row) >= 5 and row[0].strip() and not row[0].strip().startswith('-'):
                        param_name = row[0].strip()
                        param_type = row[1].strip()
                        param_desc = row[2].strip()
                        param_default = row[3].strip()
                        param_required = row[4].strip().lower() if len(row) >= 5 else '否'
                        
                        # 转换类型到JSON Schema
                        json_type = _python_type_to_schema(param_type)
                        
                        schema['properties'][param_name] = {
                            "type": json_type,
                            "description": param_desc
                        }
                        
                        if param_default and param_default != 'null':
                            if json_type == "string":
                                schema['properties'][param_name]['default'] = param_default
                            elif json_type == "number" and param_default.replace('.', '').isdigit():
                                schema['properties'][param_name]['default'] = float(param_default)
                            elif json_type == "integer" and param_default.isdigit():
                                schema['properties'][param_name]['default'] = int(param_default)
                                
                        if param_required in ['是', 'yes', 'true', 'required']:
                            schema['required'].append(param_name)
                            
        return {
            'metadata': metadata,
            'schema': schema
        }
    except Exception as e:
        logger.error(f"解析SKILL.md文件失败: {e}")
        return {'metadata': {}, 'schema': {"type": "object", "properties": {}, "required": []}}


def _python_type_to_schema(py_type: str) -> str:
    """将Python类型转换为JSON Schema类型"""
    py_type = py_type.lower().strip()
    
    type_mapping = {
        'str': 'string',
        'string': 'string',
        'int': 'integer',
        'integer': 'integer',
        'float': 'number',
        'number': 'number',
        'bool': 'boolean',
        'boolean': 'boolean',
        'list': 'array',
        'array': 'array',
        'dict': 'object',
        'object': 'object'
    }
    
    return type_mapping.get(py_type, 'string')


def parse_python_script_docstring(script_path: str) -> Dict[str, Any]:
    """
    从Python脚本文档字符串解析函数参数信息
    
    Args:
        script_path: Python脚本路径
        
    Returns:
        解析出的参数Schema
    """
    try:
        with open(script_path, 'r', encoding='utf-8') as f:
            source_code = f.read()
            
        # 解析Python AST
        tree = ast.parse(source_code)
        
        schema = {
            "type": "object",
            "properties": {},
            "required": []
        }
        
        # 查找主函数或主要执行逻辑
        for node in ast.walk(tree):
            if isinstance(node, ast.FunctionDef):
                # 只处理主函数或标记为main的函数
                if node.name in ['main', 'run', 'execute', node.name == tree.body[0].name if isinstance(tree.body[0], ast.FunctionDef) else '']:
                    
                    # 解析函数文档字符串
                    if node.body and isinstance(node.body[0], ast.Expr) and isinstance(node.body[0].value, ast.Str):
                        docstring = node.body[0].value.s
                        
                        # 解析Args部分的参数说明
                        args_section_match = re.search(r'Args:(.+?)(?=Returns:|$)', docstring, re.DOTALL)
                        if args_section_match:
                            args_text = args_section_match.group(1)
                            
                            # 解析每个参数
                            param_matches = re.findall(r'\s*(\w+):\s*(.+?)\n', args_text)
                            for param_name, param_desc in param_matches:
                                # 简单类型推断
                                param_type = 'string'  # 默认类型
                                if 'int' in param_desc.lower():
                                    param_type = 'integer'
                                elif 'float' in param_desc.lower():
                                    param_type = 'number'
                                elif 'bool' in param_desc.lower():
                                    param_type = 'boolean'
                                elif 'list' in param_desc.lower():
                                    param_type = 'array'
                                    
                                schema['properties'][param_name] = {
                                    "type": param_type,
                                    "description": param_desc.strip()
                                }
                                
                                # 如果参数有默认值则不是必填
                                has_default = False
                                for arg in node.args.args:
                                    if arg.arg == param_name:
                                        # 检查是否有默认值（复杂判断，简化处理）
                                        schema['required'].append(param_name)
                                        has_default = True
                                        break
        
        return schema
    except Exception as e:
        logger.error(f"解析Python脚本文档失败: {e}")
        return {"type": "object", "properties": {}, "required": []}


class ScriptSkill(BaseSkill):
    """基于原生 Python 脚本的 Skill"""

    def __init__(self, name: str, description: str, script_path: str, config: Optional[Dict] = None):
        """
        初始化脚本 Skill
        
        Args:
            name: Skill 名称
            description: Skill 描述
            script_path: Python 脚本路径
            config: 配置
        """
        self.name = name
        self.description = description
        self.script_path = script_path
        self._script_module = None  # 初始化脚本模块为None
        super().__init__(config)

    def _initialize(self):
        """加载并分析脚本"""
        self._script_module = None
        # 动态加载脚本的逻辑可以在子类实现
        pass

    def execute(self, **kwargs) -> Dict[str, Any]:
        """执行脚本"""
        # 子类需要实现具体的脚本执行逻辑
        return self.error("Script execution not implemented")

    def get_input_schema(self) -> Dict[str, Any]:
        """从脚本解析参数 Schema"""
        # 优先查找SKILL.md文件
        skill_dir = os.path.dirname(self.script_path)
        skill_name = os.path.basename(skill_dir)
        skill_md_path = os.path.join(skill_dir, "SKILL.md")
        
        if os.path.exists(skill_md_path):
            result = parse_skill_markdown(skill_md_path)
            if result['schema']['properties']:
                return result['schema']
        
        # 如果SKILL.md不存在，尝试从Python脚本解析
        if self.script_path.endswith('.py'):
            schema = parse_python_script_docstring(self.script_path)
            if schema['properties']:
                return schema
        
        # 如果都无法解析，返回默认Schema
        return super().get_input_schema()
    
    def execute(self, **kwargs) -> Dict[str, Any]:
        """执行脚本 - 重写此方法实现具体执行逻辑"""
        try:
            # 动态加载脚本模块
            if not self._script_module:
                self._load_script_module()
            
            # 执行主函数
            if hasattr(self._script_module, 'main'):
                result = self._script_module.main(**kwargs)
                return self.success(result, "脚本执行成功")
            elif hasattr(self._script_module, 'run'):
                result = self._script_module.run(**kwargs)
                return self.success(result, "脚本执行成功")
            elif hasattr(self._script_module, 'execute'):
                result = self._script_module.execute(**kwargs)
                return self.success(result, "脚本执行成功")
            else:
                return self.error("脚本中未找到main、run或execute函数")
        except Exception as e:
            logger.error(f"脚本执行失败: {e}", exc_info=True)
            return self.error(f"脚本执行失败: {str(e)}")
    
    def _load_script_module(self):
        """动态加载Python脚本模块"""
        try:
            import importlib.util
            spec = importlib.util.spec_from_file_location("skill_script", self.script_path)
            self._script_module = importlib.util.module_from_spec(spec)
            spec.loader.exec_module(self._script_module)
        except Exception as e:
            logger.error(f"加载脚本模块失败: {e}")
            self._script_module = None