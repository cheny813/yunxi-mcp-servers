#!/usr/bin/env python3
"""
百度搜索 Skill 适配器
将原始 baidu-search skill 适配为 MCP 可用的 Skill
"""
import os
import sys
import json
import requests
from datetime import datetime, timedelta
from pathlib import Path

# 添加父目录到路径以便导入 skill_adapter
sys.path.insert(0, str(Path(__file__).parent.parent))

from skill_adapter import BaseSkill


class BaiduSearchSkill(BaseSkill):
    """百度搜索 Skill"""

    name = "baidu_search"
    description = "使用百度 AI 搜索引擎进行网页搜索，获取实时信息和新闻"
    version = "1.0.0"

    API_ENDPOINT = "https://qianfan.baidubce.com/v2/ai_search/web_search"

    def _initialize(self):
        """初始化"""
        self.api_key = self.get_env("BAIDU_API_KEY", "")
        if not self.api_key:
            raise ValueError("BAIDU_API_KEY environment variable not set")

    def get_input_schema(self) -> dict:
        """获取输入参数 Schema"""
        return {
            "type": "object",
            "properties": {
                "query": {
                    "type": "string",
                    "description": "搜索关键词。示例: '人工智能最新发展'"
                },
                "count": {
                    "type": "integer",
                    "description": "返回结果数量 (1-50，默认 10)",
                    "default": 10
                },
                "freshness": {
                    "type": "string",
                    "description": "时间范围: pd(过去24小时), pw(过去7天), pm(过去30天), py(过去365天)"
                }
            },
            "required": ["query"]
        }

    def execute(self, **kwargs) -> dict:
        """
        执行搜索
        
        Args:
            query: 搜索关键词
            count: 结果数量
            freshness: 时间范围
        """
        query = kwargs.get("query")
        if not query:
            return self.error("query is required")

        count = kwargs.get("count", 10)
        if count < 1: count = 1
        if count > 50: count = 50
        
        freshness = kwargs.get("freshness")

        try:
            # 构建请求
            request_body = {
                "messages": [
                    {"content": query, "role": "user"}
                ],
                "search_source": "baidu_search_v2",
                "resource_type_filter": [
                    {"type": "web", "top_k": count}
                ]
            }

            # 添加时间过滤
            if freshness:
                search_filter = self._build_search_filter(freshness)
                if search_filter:
                    request_body["search_filter"] = search_filter

            # 发送请求
            headers = {
                "Authorization": f"Bearer {self.api_key}",
                "X-Appbuilder-From": "yunxi-skill-adapter",
                "Content-Type": "application/json"
            }

            response = requests.post(
                self.API_ENDPOINT,
                json=request_body,
                headers=headers,
                timeout=30
            )
            response.raise_for_status()
            results = response.json()

            # 检查错误
            if "code" in results:
                return self.error(results.get("message", "Unknown error"))

            # 提取结果
            references = results.get("references", [])
            if not references:
                return self.success({"results": [], "message": f"No results found for: {query}"})

            # 格式化输出
            formatted_results = []
            for i, item in enumerate(references, 1):
                formatted_results.append({
                    "index": i,
                    "title": item.get("title", ""),
                    "url": item.get("url", ""),
                    "time": item.get("time", ""),
                    "content": item.get("content", "")[:200]  # 截断过长内容
                })

            return self.success({
                "query": query,
                "count": len(formatted_results),
                "results": formatted_results
            })

        except requests.exceptions.RequestException as e:
            return self.error(f"Request failed: {str(e)}")
        except Exception as e:
            return self.error(f"Search failed: {str(e)}")

    def _build_search_filter(self, freshness: str) -> dict:
        """构建搜索过滤条件"""
        if not freshness:
            return None

        # 预定义时间范围
        range_map = {
            "pd": "1",   # past day
            "pw": "7",   # past week
            "pm": "30",  # past month
            "py": "365"  # past year
        }

        if freshness in range_map:
            return {
                "range": {
                    "page_time": {
                        "gte": range_map[freshness],
                        "lt": "1"
                    }
                }
            }

        # 自定义日期范围 YYYY-MM-DDtoYYYY-MM-DD
        if "to" in freshness:
            parts = freshness.split("to")
            if len(parts) == 2:
                return {
                    "range": {
                        "page_time": {
                            "gte": parts[0].strip(),
                            "lt": parts[1].strip()
                        }
                    }
                }

        return None


# 如果直接运行，执行测试
if __name__ == "__main__":
    import logging
    logging.basicConfig(level=logging.DEBUG)
    
    # 测试
    skill = BaiduSearchSkill()
    result = skill.execute(query="人工智能")
    print(json.dumps(result, ensure_ascii=False, indent=2))