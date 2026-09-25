@echo off
title Elder Care Assistance System - Web Dashboard Server
cd /d "%~dp0"
echo ====================================================================
echo     Launching Elder Care Assistance System Web Dashboard Server...
echo     URL: http://localhost:8080/
echo ====================================================================
call mvnw.bat compile exec:java -Pweb
pause
