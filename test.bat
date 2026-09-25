@echo off
title Elder Care Assistance System - Tests
cd /d "%~dp0"
echo ===================================================
echo     Running All 53 JUnit 5 Automated Tests...
echo ===================================================
call mvnw.bat test
pause
