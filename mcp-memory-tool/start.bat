@echo off
chcp 65001 >nul

set MCP_PORT=40304
set MCP_HOST=localhost
set LOG_LEVEL=INFO
set ENVIRONMENT=dev

set MEMORY_STORE_TYPE=milvus
set MILVUS_ENABLED=true
set EMBEDDING_PROVIDER=dashscope
set MILVUS_HOST=localhost
set MILVUS_PORT=19530

set DASHSCOPE_API_KEY=sk-afdfb45cced2419aa5de0c79e3c9a416

for /f "tokens=5" %%a in ('netstat -ano ^| findstr ":%MCP_PORT% " ^| findstr LISTENING') do taskkill /F /PID %%a >nul 2>&1

timeout /t 2 /nobreak >nul

echo Starting MCP Memory Tool Server...
echo Port: %MCP_PORT%
echo Host: %MCP_HOST%
echo Storage: Milvus

java -Dfile.encoding=UTF-8 ^
     -Dserver.port=%MCP_PORT% ^
     -Dspring.profiles.active=%ENVIRONMENT% ^
     -Dyunxi.environment.mode=%ENVIRONMENT% ^
     -Dmemory.store.type=%MEMORY_STORE_TYPE% ^
     -Dmilvus.enabled=%MILVUS_ENABLED% ^
     -Dmilvus.host=%MILVUS_HOST% ^
     -Dmilvus.port=%MILVUS_PORT% ^
     -Dembedding.provider=%EMBEDDING_PROVIDER% ^
     -Dlogging.level.io.yunxi.mcp.memory=%LOG_LEVEL% ^
     -jar target\mcp-memory-tool-1.0.0.jar

pause
