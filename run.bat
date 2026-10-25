@echo off
title MTR Polish PIDS - Test Environment
echo ===================================================
echo   Uruchamianie klienta testowego Minecraft z modem
echo   MTR Polish PIDS
echo ===================================================

set "JAVA_HOME=C:\Users\SZABLIX\AppData\Roaming\PrismLauncher\java\java-runtime-gamma"
set "PATH=%JAVA_HOME%\bin;%PATH%"

echo Wersja Java:
java -version
echo.
echo Kompilacja ewentualnych zmian i start klienta gry...
call gradlew.bat runClient

if %ERRORLEVEL% NEQ 0 (
    echo.
    echo Wystapil blad podczas uruchamiania klienta.
    pause
)
