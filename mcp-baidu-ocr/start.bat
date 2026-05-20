@echo off
chcp 65001 >nul
set MAVEN_OPTS=-Xmx512m
call mvn spring-boot:run -DskipTests
pause