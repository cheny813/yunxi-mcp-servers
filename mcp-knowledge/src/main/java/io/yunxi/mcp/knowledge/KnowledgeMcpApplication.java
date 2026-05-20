package io.yunxi.mcp.knowledge;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 知识库 MCP 服务器应用启动类
 * <p>
 * 提供基于向量的知识库管理和语义搜索服务。
 * 支持从代码仓库自动提取知识，并存储到 Milvus 向量数据库中。
 * </p>
 * <p>
 * 主要功能：
 * </p>
 * <ul>
 * <li>知识添加 - 支持自动向量化的知识入库</li>
 * <li>语义搜索 - 基于向量相似度的知识检索</li>
 * <li>知识提取 - 从代码仓库自动提取架构、业务逻辑、数据库结构</li>
 * <li>变更监控 - 监控 Git 提交自动触发知识更新</li>
 * </ul>
 *
 * @author yunxi-mcp-servers
 */
@SpringBootApplication
public class KnowledgeMcpApplication {
    public static void main(String[] args) {
        SpringApplication.run(KnowledgeMcpApplication.class, args);
    }
}