@echo off
chcp 65001 >nul
cd target
java -Dfile.encoding=UTF-8 -jar mcp-api-gateway-1.0.0.jar
