@echo off
chcp 65001 >nul
cd target
java -Dfile.encoding=UTF-8 -jar mcp-code-search-1.0.0.jar
