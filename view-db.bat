@echo off
title Elder Care Assistance System - Database Viewer
cd /d "%~dp0"
echo ====================================================================
echo   Inspecting SQLite Database (eldercare.db)...
echo ====================================================================
call mvnw.bat compile exec:java -Pdb
pause
