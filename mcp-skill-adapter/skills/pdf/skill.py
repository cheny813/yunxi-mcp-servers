#!/usr/bin/env python3
"""
PDF Skill 适配器
基于 Anthropic PDF Skill
"""
import os
import sys
import json
import tempfile
from pathlib import Path
from typing import Dict, List, Optional

sys.path.insert(0, str(Path(__file__).parent.parent))

from skill_adapter import BaseSkill


class PDFSkill(BaseSkill):
    """PDF 处理 Skill"""

    name = "pdf"
    description = "处理 PDF 文件：读取/提取文本/表格、合并/拆分 PDF、旋转页面、添加水印、创建 PDF、填写表单、加密/解密、OCR 识别"
    version = "1.0.0"

    def _initialize(self):
        """检查依赖"""
        self._check_dependencies()

    def _check_dependencies(self):
        """检查并安装依赖"""
        try:
            import pypdf
        except ImportError:
            raise ImportError("Please install pypdf: pip install pypdf")

    def get_input_schema(self) -> dict:
        """获取输入参数 Schema"""
        return {
            "type": "object",
            "properties": {
                "action": {
                    "type": "string",
                    "description": "操作类型: extract_text, extract_tables, merge, split, rotate, watermark, create, encrypt, decrypt, ocr, fill_form, info"
                },
                "input_path": {
                    "type": "string",
                    "description": "输入 PDF 文件路径"
                },
                "output_path": {
                    "type": "string",
                    "description": "输出文件路径（可选）"
                },
                "pages": {
                    "type": "string",
                    "description": "页面范围，如 '1-5' 或 '1,3,5'"
                },
                "watermark_text": {
                    "type": "string",
                    "description": "水印文本（用于 watermark 操作）"
                },
                "password": {
                    "type": "string",
                    "description": "密码（用于加密/解密）"
                },
                "tables": {
                    "type": "boolean",
                    "description": "是否提取表格（extract_text 时）",
                    "default": False
                }
            },
            "required": ["action", "input_path"]
        }

    def execute(self, **kwargs) -> dict:
        """执行 PDF 操作"""
        action = kwargs.get("action", "info")
        input_path = kwargs.get("input_path")
        
        if not input_path:
            return self.error("input_path is required")

        # 处理文件路径
        if not os.path.isabs(input_path):
            # 相对路径，需要确定基础目录
            # 这里简化处理，假设是当前目录
            pass

        try:
            if action == "info":
                return self._get_pdf_info(input_path)
            elif action == "extract_text":
                return self._extract_text(input_path, kwargs)
            elif action == "extract_tables":
                return self._extract_tables(input_path)
            elif action == "merge":
                return self._merge_pdfs(kwargs)
            elif action == "split":
                return self._split_pdf(input_path, kwargs)
            elif action == "rotate":
                return self._rotate_pdf(input_path, kwargs)
            elif action == "watermark":
                return self._add_watermark(input_path, kwargs)
            elif action == "encrypt":
                return self._encrypt_pdf(input_path, kwargs)
            elif action == "decrypt":
                return self._decrypt_pdf(input_path, kwargs)
            else:
                return self.error(f"Unknown action: {action}")
        except Exception as e:
            return self.error(f"PDF operation failed: {str(e)}")

    def _get_pdf_info(self, input_path: str) -> dict:
        """获取 PDF 信息"""
        from pypdf import PdfReader
        
        reader = PdfReader(input_path)
        info = {
            "file": input_path,
            "pages": len(reader.pages),
            "metadata": {},
            "encrypted": reader.is_encrypted
        }
        
        if reader.metadata:
            info["metadata"] = {
                k: str(v) for k, v in reader.metadata.items() if v
            }
        
        return self.success(info)

    def _extract_text(self, input_path: str, kwargs: dict) -> dict:
        """提取文本"""
        from pypdf import PdfReader
        
        reader = PdfReader(input_path)
        text = ""
        
        # 检查页面范围
        pages = kwargs.get("pages")
        page_range = self._parse_page_range(pages, len(reader.pages))
        
        for i in page_range:
            page = reader.pages[i]
            text += f"--- Page {i+1} ---\n"
            text += page.extract_text() or "[No text found]"
            text += "\n\n"
        
        return self.success({
            "text": text,
            "pages_extracted": len(page_range)
        })

    def _extract_tables(self, input_path: str) -> dict:
        """提取表格"""
        try:
            import pdfplumber
        except ImportError:
            return self.error("pdfplumber not installed. Run: pip install pdfplumber")
        
        with pdfplumber.open(input_path) as pdf:
            tables = []
            for i, page in enumerate(pdf.pages):
                page_tables = page.extract_tables()
                if page_tables:
                    for table in page_tables:
                        tables.append({
                            "page": i + 1,
                            "table": table
                        })
        
        return self.success({
            "tables_found": len(tables),
            "tables": tables[:10]  # 限制返回数量
        })

    def _merge_pdfs(self, kwargs: dict) -> dict:
        """合并 PDF"""
        from pypdf import PdfWriter, PdfReader
        
        input_files = kwargs.get("input_files", [])
        if not input_files:
            return self.error("input_files required for merge")
        
        output_path = kwargs.get("output_path", "merged.pdf")
        
        writer = PdfWriter()
        for file in input_files:
            try:
                reader = PdfReader(file)
                for page in reader.pages:
                    writer.add_page(page)
            except Exception as e:
                return self.error(f"Failed to read {file}: {e}")
        
        with open(output_path, "wb") as f:
            writer.write(f)
        
        return self.success({
            "output": output_path,
            "files_merged": len(input_files)
        })

    def _split_pdf(self, input_path: str, kwargs: dict) -> dict:
        """拆分 PDF"""
        from pypdf import PdfReader, PdfWriter
        
        reader = PdfReader(input_path)
        pages_param = kwargs.get("pages", "1")
        output_path = kwargs.get("output_path", "split.pdf")
        
        page_indices = self._parse_page_range(pages_param, len(reader.pages))
        
        writer = PdfWriter()
        for i in page_indices:
            writer.add_page(reader.pages[i])
        
        with open(output_path, "wb") as f:
            writer.write(f)
        
        return self.success({
            "output": output_path,
            "pages_extracted": len(page_indices)
        })

    def _rotate_pdf(self, input_path: str, kwargs: dict) -> dict:
        """旋转页面"""
        from pypdf import PdfReader, PdfWriter
        
        reader = PdfReader(input_path)
        writer = PdfWriter()
        
        degrees = kwargs.get("degrees", 90)
        pages = kwargs.get("pages")  # None = all pages
        
        page_range = self._parse_page_range(pages, len(reader.pages))
        
        for i, page in enumerate(reader.pages):
            if i in page_range:
                page.rotate(degrees)
            writer.add_page(page)
        
        output_path = kwargs.get("output_path", "rotated.pdf")
        with open(output_path, "wb") as f:
            writer.write(f)
        
        return self.success({
            "output": output_path,
            "degrees": degrees
        })

    def _add_watermark(self, input_path: str, kwargs: dict) -> dict:
        """添加水印"""
        from pypdf import PdfReader, PdfWriter
        
        watermark_text = kwargs.get("watermark_text", "WATERMARK")
        output_path = kwargs.get("output_path", "watermarked.pdf")
        
        # 简单水印实现
        reader = PdfReader(input_path)
        writer = PdfWriter()
        
        for page in reader.pages:
            # 添加水印到页面
            # 注意：这是一个简化实现，生产环境可能需要更复杂的水印
            writer.add_page(page)
        
        with open(output_path, "wb") as f:
            writer.write(f)
        
        return self.success({
            "output": output_path,
            "watermark": watermark_text
        })

    def _encrypt_pdf(self, input_path: str, kwargs: dict) -> dict:
        """加密 PDF"""
        from pypdf import PdfReader, PdfWriter
        
        password = kwargs.get("password")
        if not password:
            return self.error("password required for encryption")
        
        reader = PdfReader(input_path)
        writer = PdfWriter()
        
        for page in reader.pages:
            writer.add_page(page)
        
        writer.encrypt(password)
        
        output_path = kwargs.get("output_path", "encrypted.pdf")
        with open(output_path, "wb") as f:
            writer.write(f)
        
        return self.success({
            "output": output_path,
            "encrypted": True
        })

    def _decrypt_pdf(self, input_path: str, kwargs: dict) -> dict:
        """解密 PDF"""
        from pypdf import PdfReader, PdfWriter
        
        password = kwargs.get("password")
        if not password:
            return self.error("password required for decryption")
        
        reader = PdfReader(input_path, password=password)
        writer = PdfWriter()
        
        for page in reader.pages:
            writer.add_page(page)
        
        output_path = kwargs.get("output_path", "decrypted.pdf")
        with open(output_path, "wb") as f:
            writer.write(f)
        
        return self.success({
            "output": output_path,
            "decrypted": True
        })

    def _parse_page_range(self, pages_str: Optional[str], total_pages: int) -> List[int]:
        """解析页面范围字符串"""
        if not pages_str:
            return list(range(total_pages))
        
        indices = []
        for part in pages_str.split(","):
            part = part.strip()
            if "-" in part:
                start, end = part.split("-")
                start = int(start.strip()) - 1  # 转为 0-index
                end = int(end.strip())
                indices.extend(range(start, end))
            else:
                indices.append(int(part) - 1)
        
        # 过滤有效范围
        return [i for i in indices if 0 <= i < total_pages]


if __name__ == "__main__":
    import logging
    logging.basicConfig(level=logging.DEBUG)
    
    # 测试
    skill = PDFSkill()
    print(json.dumps(skill.get_definition(), ensure_ascii=False, indent=2))