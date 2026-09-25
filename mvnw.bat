@echo off
setlocal

rem 1. Ensure JAVA_HOME is set if java is not on PATH
where java >nul 2>nul
if %ERRORLEVEL% NEQ 0 (
    for /d %%D in ("%USERPROFILE%\.jdks\*") do (
        if exist "%%D\bin\java.exe" (
            set "JAVA_HOME=%%D"
            set "PATH=%%D\bin;%PATH%"
        )
    )
)

rem 2. Check if 'mvn' is already on PATH
where mvn >nul 2>nul
if %ERRORLEVEL% EQU 0 (
    call mvn %*
    exit /b %ERRORLEVEL%
)

rem 3. Check local user wrapper cache
if exist "%USERPROFILE%\.m2\wrapper\dists\apache-maven-3.8.5-bin\5i5jha092a3i37g0paqnfr15e0\apache-maven-3.8.5\bin\mvn.cmd" (
    call "%USERPROFILE%\.m2\wrapper\dists\apache-maven-3.8.5-bin\5i5jha092a3i37g0paqnfr15e0\apache-maven-3.8.5\bin\mvn.cmd" %*
    exit /b %ERRORLEVEL%
)

if exist "%USERPROFILE%\.m2\wrapper\apache-maven-3.9.6\bin\mvn.cmd" (
    call "%USERPROFILE%\.m2\wrapper\apache-maven-3.9.6\bin\mvn.cmd" %*
    exit /b %ERRORLEVEL%
)

rem 4. Auto-download portable Maven if running on a new laptop without Maven installed
echo [Setup] Maven not found on PATH. Downloading portable Apache Maven 3.9.6 (one-time setup)...
powershell -NoProfile -Command ^
    "$ErrorActionPreference = 'Stop'; " ^
    "$dir = Join-Path $env:USERPROFILE '.m2\wrapper'; " ^
    "New-Item -ItemType Directory -Force -Path $dir | Out-Null; " ^
    "$zip = Join-Path $dir 'maven.zip'; " ^
    "Invoke-WebRequest -Uri 'https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/3.9.6/apache-maven-3.9.6-bin.zip' -OutFile $zip; " ^
    "Expand-Archive -Path $zip -DestinationPath $dir -Force; " ^
    "Remove-Item $zip -Force"

if exist "%USERPROFILE%\.m2\wrapper\apache-maven-3.9.6\bin\mvn.cmd" (
    call "%USERPROFILE%\.m2\wrapper\apache-maven-3.9.6\bin\mvn.cmd" %*
    exit /b %ERRORLEVEL%
) else (
    echo [ERROR] Could not find or download Maven. Please install Java 17+ and Maven.
    exit /b 1
)

