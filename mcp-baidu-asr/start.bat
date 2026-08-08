chcp 65001 >nul
@echo off
set MAVEN_OPTS=-Xmx512m
call mvn spring-boot:run -DskipTests
pause