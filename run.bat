@echo off
cd /d "%~dp0"
title MTR Polish PIDS - Test Client
echo ===================================================
echo   Uruchamianie klienta testowego Minecraft z modem
echo   MTR Polish PIDS
echo ===================================================

set "JAVA_HOME=C:\Users\SZABLIX\AppData\Roaming\PrismLauncher\java\java-runtime-gamma"
set "PATH=%JAVA_HOME%\bin;%PATH%"

echo Wersja Java:
"%JAVA_HOME%\bin\java.exe" -version
echo.
echo Kompilacja i start klienta gry Minecraft...
call gradlew.bat runClient

if %ERRORLEVEL% NEQ 0 (
    echo.
    echo Wystapil problem podczas uruchamiania.
    pause
)
