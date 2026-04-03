@echo off
setlocal enabledelayedexpansion

set "JAR_DIR=%~dp0"
set "JAR_FILE=%JAR_DIR%SpeedLAN.jar"

if not exist "%JAR_FILE%" (
    echo Error: SpeedLAN.jar not found in same directory
    pause
    exit /b 1
)

if "%~1"=="" (
    echo Usage:
    echo   SpeedLAN.bat Send    [file-path]
    echo   SpeedLAN.bat Receive [save-directory]
    echo.
    echo Examples:
    echo   SpeedLAN.bat Send C:\path\to\file.jpg
    echo   SpeedLAN.bat Receive
    echo   SpeedLAN.bat Receive C:\Users\Shadow\Downloads
    pause
    exit /b 0
)

java -jar "%JAR_FILE%" %*

pause
