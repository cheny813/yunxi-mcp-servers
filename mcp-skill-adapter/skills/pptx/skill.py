#!/usr/bin/env python3
"""
PPTX (PowerPoint) Skill 适配器
"""
import os
import sys
import json
from pathlib import Path
from typing import Dict, List, Optional

sys.path.insert(0, str(Path(__file__).parent.parent))

from skill_adapter import BaseSkill


class PPTXSkill(BaseSkill):
    """PowerPoint 处理 Skill"""

    name = "pptx"
    description = "处理 PowerPoint 文件：创建/编辑幻灯片、添加文本/图片/图表、设置布局/主题、合并/拆分演示文稿、导出为 PDF"
    version = "1.0.0"

    def _initialize(self):
        """检查依赖"""
        self._check_dependencies()

    def _check_dependencies(self):
        """检查并安装依赖"""
        try:
            import pptx
        except ImportError:
            raise ImportError("Please install python-pptx: pip install python-pptx")

    def get_input_schema(self) -> dict:
        """获取输入参数 Schema"""
        return {
            "type": "object",
            "properties": {
                "action": {
                    "type": "string",
                    "description": "操作类型: create, add_slide, add_text, add_image, add_chart, get_info, merge, export_pdf"
                },
                "file_path": {
                    "type": "string",
                    "description": "PPT 文件路径"
                },
                "title": {
                    "type": "string",
                    "description": "幻灯片标题"
                },
                "content": {
                    "type": "string",
                    "description": "幻灯片内容"
                },
                "layout": {
                    "type": "string",
                    "description": "布局类型: title, content, title_content, blank"
                },
                "output_path": {
                    "type": "string",
                    "description": "输出文件路径"
                },
                "slide_index": {
                    "type": "integer",
                    "description": "幻灯片索引（从 0 开始）"
                }
            },
            "required": ["action"]
        }

    def execute(self, **kwargs) -> dict:
        """执行 PPT 操作"""
        action = kwargs.get("action", "get_info")
        
        try:
            if action == "create":
                return self._create_presentation(kwargs)
            elif action == "add_slide":
                return self._add_slide(kwargs)
            elif action == "add_text":
                return self._add_text_slide(kwargs)
            elif action == "get_info":
                return self._get_presentation_info(kwargs)
            elif action == "merge":
                return self._merge_presentations(kwargs)
            elif action == "export_pdf":
                return self._export_pdf(kwargs)
            else:
                return self.error(f"Unknown action: {action}")
        except Exception as e:
            return self.error(f"PPTX operation failed: {str(e)}")

    def _create_presentation(self, kwargs: dict) -> dict:
        """创建演示文稿"""
        from pptx import Presentation
        from pptx.util import Inches, Pt
        
        output_path = kwargs.get("output_path", "presentation.pptx")
        title = kwargs.get("title", "New Presentation")
        
        prs = Presentation()
        
        # 设置标题页
        title_slide_layout = prs.slide_layouts[0]
        slide = prs.slides.add_slide(title_slide_layout)
        title_placeholder = slide.placeholders[0]
        title_placeholder.text = title
        
        prs.save(output_path)
        
        return self.success({
            "created": output_path,
            "slides": len(prs.slides)
        })

    def _add_slide(self, kwargs: dict) -> dict:
        """添加幻灯片"""
        from pptx import Presentation
        from pptx.util import Inches, Pt
        
        file_path = kwargs.get("file_path")
        title = kwargs.get("title", "")
        content = kwargs.get("content", "")
        layout = kwargs.get("layout", "content")
        
        if not file_path:
            return self.error("file_path is required")
        
        # 加载或创建演示文稿
        if os.path.exists(file_path):
            prs = Presentation(file_path)
        else:
            prs = Presentation()
        
        # 选择布局
        layout_map = {
            "title": 0,
            "content": 1,
            "title_content": 1,
            "blank": 6
        }
        layout_index = layout_map.get(layout, 1)
        
        slide_layout = prs.slide_layouts[layout_index]
        slide = prs.slides.add_slide(slide_layout)
        
        # 设置标题
        if title and slide.placeholders[0]:
            slide.placeholders[0].text = title
        
        # 设置内容
        if content and len(slide.placeholders) > 1:
            slide.placeholders[1].text = content
        
        prs.save(file_path)
        
        return self.success({
            "file": file_path,
            "slides": len(prs.slides)
        })

    def _add_text_slide(self, kwargs: dict) -> dict:
        """添加文本幻灯片（简化）"""
        from pptx import Presentation
        
        file_path = kwargs.get("file_path", "presentation.pptx")
        title = kwargs.get("title", "")
        content = kwargs.get("content", "")
        
        # 加载或创建
        if os.path.exists(file_path):
            prs = Presentation(file_path)
        else:
            prs = Presentation()
        
        # 使用标题+内容布局
        slide_layout = prs.slide_layouts[1]
        slide = prs.slides.add_slide(slide_layout)
        
        if title:
            slide.shapes.title.text = title
        if content:
            slide.placeholders[1].text = content
        
        prs.save(file_path)
        
        return self.success({
            "file": file_path,
            "slides": len(prs.slides)
        })

    def _get_presentation_info(self, kwargs: dict) -> dict:
        """获取演示文稿信息"""
        from pptx import Presentation
        
        file_path = kwargs.get("file_path")
        
        if not file_path:
            return self.error("file_path is required")
        
        if not os.path.exists(file_path):
            return self.error(f"File not found: {file_path}")
        
        prs = Presentation(file_path)
        
        info = {
            "file": file_path,
            "slides": len(prs.slides),
            "slide_layouts": len(prs.slide_layouts)
        }
        
        # 获取幻灯片信息
        slides_info = []
        for i, slide in enumerate(prs.slides):
            slide_info = {
                "index": i,
                "shapes": len(slide.shapes)
            }
            # 获取标题
            if slide.shapes.title:
                slide_info["title"] = slide.shapes.title.text
            slides_info.append(slide_info)
        
        info["slide_details"] = slides_info
        
        return self.success(info)

    def _merge_presentations(self, kwargs: dict) -> dict:
        """合并演示文稿"""
        from pptx import Presentation
        
        file_paths = kwargs.get("file_paths", [])
        output_path = kwargs.get("output_path", "merged.pptx")
        
        if not file_paths:
            return self.error("file_paths is required")
        
        # 创建新演示文稿
        merged_prs = Presentation()
        
        for file_path in file_paths:
            if not os.path.exists(file_path):
                continue
            
            prs = Presentation(file_path)
            for slide in prs.slides:
                # 复制幻灯片
                slide_layout = merged_prs.slide_layouts[6]  # blank
                new_slide = merged_prs.slides.add_slide(slide_layout)
                
                # 复制内容
                for shape in slide.shapes:
                    if hasattr(shape, "text"):
                        new_shape = new_shapes.add_shape(
                            shape.auto_shape_type,
                            shape.left, shape.top,
                            shape.width, shape.height
                        )
                        new_shape.text = shape.text
        
        merged_prs.save(output_path)
        
        return self.success({
            "merged": output_path,
            "files": len(file_paths),
            "slides": len(merged_prs.slides)
        })

    def _export_pdf(self, kwargs: dict) -> dict:
        """导出为 PDF"""
        from pptx import Presentation
        
        # 注意：python-pptx 不直接支持导出 PDF
        # 需要使用其他工具，如 comtypes (Windows) 或 unoconv
        
        file_path = kwargs.get("file_path")
        output_path = kwargs.get("output_path")
        
        if not file_path:
            return self.error("file_path is required")
        
        return self.warning("PDF export requires additional tools. On Windows, use comtypes. On Linux, use unoconv.")


if __name__ == "__main__":
    import logging
    logging.basicConfig(level=logging.DEBUG)
    
    skill = PPTXSkill()
    print(json.dumps(skill.get_definition(), ensure_ascii=False, indent=2))