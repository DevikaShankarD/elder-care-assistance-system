@echo off
title Elder Care Assistance System
cd /d "%~dp0"
echo ===================================================
echo     Starting Elder Care Assistance System...
echo ===================================================
call mvnw.bat exec:java
pause
