@echo off
rem Helper wrapper to run Maven using installed Maven 3.8.5 if 'mvn' is not on PATH
where mvn >nul 2>nul
if %ERRORLEVEL% EQU 0 (
    mvn %*
) else (
    if exist "C:\Users\divya\.m2\wrapper\dists\apache-maven-3.8.5-bin\5i5jha092a3i37g0paqnfr15e0\apache-maven-3.8.5\bin\mvn.cmd" (
        "C:\Users\divya\.m2\wrapper\dists\apache-maven-3.8.5-bin\5i5jha092a3i37g0paqnfr15e0\apache-maven-3.8.5\bin\mvn.cmd" %*
    ) else if exist "C:\Program Files\JetBrains\IntelliJ IDEA 2026.2.2\plugins\maven-plugin\lib\maven3\bin\mvn.cmd" (
        "C:\Program Files\JetBrains\IntelliJ IDEA 2026.2.2\plugins\maven-plugin\lib\maven3\bin\mvn.cmd" %*
    ) else (
        echo [ERROR] Maven not found. Please ensure Maven is installed or added to PATH.
        exit /b 1
    )
)
