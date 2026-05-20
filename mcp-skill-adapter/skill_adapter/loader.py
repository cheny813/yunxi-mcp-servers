"""
Skill 加载器 - 自动发现和加载 Skill
"""
import os
import sys
import importlib.util
import logging
from pathlib import Path
from typing import Dict, List, Optional, Type

from .base import BaseSkill

logger = logging.getLogger(__name__)


class SkillLoader:
    """Skill 加载器 - 自动扫描和加载 skills"""

    def __init__(self, skills_dir: str = "./skills"):
        """
        初始化加载器
        
        Args:
            skills_dir: Skills 目录路径
        """
        self.skills_dir = Path(skills_dir)
        self.skills: Dict[str, BaseSkill] = {}

    def load_all(self) -> Dict[str, BaseSkill]:
        """
        加载所有 Skills
        
        Returns:
            Skill 实例字典
        """
        if not self.skills_dir.exists():
            logger.warning(f"Skills directory not found: {self.skills_dir}")
            return {}

        # 扫描所有 Python 文件
        for item in self.skills_dir.iterdir():
            if item.is_dir():
                # 目录形式：skill_name/
                self._load_skill_from_dir(item)
            elif item.is_file() and item.suffix == ".py" and not item.name.startswith("_"):
                # 单文件形式：skill_name.py
                self._load_skill_from_file(item)

        logger.info(f"Loaded {len(self.skills)} skills: {list(self.skills.keys())}")
        return self.skills

    def _load_skill_from_dir(self, skill_dir: Path):
        """从目录加载 Skill"""
        # 查找 skill.py 或 __init__.py
        skill_file = skill_dir / "skill.py"
        if not skill_file.exists():
            skill_file = skill_dir / "__init__.py"
        
        if not skill_file.exists():
            logger.warning(f"No skill.py or __init__.py found in {skill_dir}")
            return

        try:
            # 将目录添加到 Python 路径
            if str(skill_dir.parent) not in sys.path:
                sys.path.insert(0, str(skill_dir.parent))

            # 动态加载模块
            module_name = skill_dir.name
            spec = importlib.util.spec_from_file_location(module_name, skill_file)
            if spec and spec.loader:
                module = importlib.util.module_from_spec(spec)
                sys.modules[module_name] = module
                spec.loader.exec_module(module)

                # 查找继承自 BaseSkill 的类
                skill_class = self._find_skill_class(module)
                if skill_class:
                    # 实例化 Skill
                    skill = skill_class()
                    self.skills[skill.name] = skill
                    logger.info(f"Loaded skill: {skill.name} from {skill_dir}")
                else:
                    logger.warning(f"No BaseSkill subclass found in {skill_dir}")

        except Exception as e:
            logger.error(f"Failed to load skill from {skill_dir}: {e}")

    def _load_skill_from_file(self, skill_file: Path):
        """从单文件加载 Skill"""
        try:
            module_name = skill_file.stem
            spec = importlib.util.spec_from_file_location(module_name, skill_file)
            if spec and spec.loader:
                module = importlib.util.module_from_spec(spec)
                sys.modules[module_name] = module
                spec.loader.exec_module(module)

                skill_class = self._find_skill_class(module)
                if skill_class:
                    skill = skill_class()
                    self.skills[skill.name] = skill
                    logger.info(f"Loaded skill: {skill.name} from {skill_file}")

        except Exception as e:
            logger.error(f"Failed to load skill from {skill_file}: {e}")

    def _find_skill_class(self, module) -> Optional[Type[BaseSkill]]:
        """从模块中查找 BaseSkill 子类"""
        for name in dir(module):
            obj = getattr(module, name)
            if isinstance(obj, type) and issubclass(obj, BaseSkill) and obj is not BaseSkill:
                return obj
        return None

    def get_skill(self, name: str) -> Optional[BaseSkill]:
        """获取指定 Skill"""
        return self.skills.get(name)

    def get_all_skill_definitions(self) -> List[Dict]:
        """获取所有 Skill 的定义"""
        return [skill.get_definition() for skill in self.skills.values()]

    def register_skill(self, skill: BaseSkill):
        """手动注册 Skill"""
        self.skills[skill.name] = skill
        logger.info(f"Registered skill: {skill.name}")