@echo off
setlocal enabledelayedexpansion
cd /d "%~dp0"
title MTR Polish Pack - Test Client

echo ===================================================
echo   Uruchamianie klienta testowego Minecraft z modem
echo   MTR Polish Pack (Sodium + Iris + Complementary)
echo ===================================================

set "JAVA_HOME=C:\Users\SZABLIX\AppData\Roaming\PrismLauncher\java\java-runtime-gamma"
set "PATH=%JAVA_HOME%\bin;%PATH%"
set "PRISM_DIR=C:\Users\SZABLIX\AppData\Roaming\PrismLauncher\instances\Ciapongi RP\minecraft"

:: Upewnij sie, ze foldery run istnieja
if not exist "run\mods" mkdir "run\mods"
if not exist "run\shaderpacks" mkdir "run\shaderpacks"
if not exist "run\resourcepacks" mkdir "run\resourcepacks"
if not exist "run\config" mkdir "run\config"

:: Kopiowanie modow testowych z instancji PrismLauncher, jesli brakuje
if exist "%PRISM_DIR%\mods" (
    if not exist "run\mods\iris*.jar" (
        echo Kopiowanie Iris Shaders z instancji Prism...
        copy /y "%PRISM_DIR%\mods\iris*.jar" "run\mods\" >nul
    )
    if not exist "run\mods\sodium*.jar" (
        echo Kopiowanie Sodium z instancji Prism...
        copy /y "%PRISM_DIR%\mods\sodium*.jar" "run\mods\" >nul
    )
    if not exist "run\mods\modmenu*.jar" (
        echo Kopiowanie Mod Menu z instancji Prism...
        copy /y "%PRISM_DIR%\mods\modmenu*.jar" "run\mods\" >nul
    )
    if not exist "run\mods\caxton*.jar" (
        echo Kopiowanie Caxton z instancji Prism...
        copy /y "%PRISM_DIR%\mods\caxton*.jar" "run\mods\" >nul
    )
    if not exist "run\mods\ImmediatelyFast*.jar" (
        echo Kopiowanie ImmediatelyFast z instancji Prism...
        copy /y "%PRISM_DIR%\mods\ImmediatelyFast*.jar" "run\mods\" >nul
    )
)

:: Kopiowanie paczki shaderow z instancji PrismLauncher, jesli brakuje
if exist "%PRISM_DIR%\shaderpacks" (
    if not exist "run\shaderpacks\ComplementaryUnbound*.zip" (
        echo Kopiowanie shaderow Complementary Unbound z instancji Prism...
        copy /y "%PRISM_DIR%\shaderpacks\ComplementaryUnbound*.zip" "run\shaderpacks\" >nul
    )
)

:: Kopiowanie resourcepackow z instancji PrismLauncher, jesli brakuje
if exist "%PRISM_DIR%\resourcepacks" (
    if not exist "run\resourcepacks\MFP-Caxton*.zip" (
        echo Kopiowanie resourcepacka MFP-Caxton z instancji Prism...
        copy /y "%PRISM_DIR%\resourcepacks\MFP-Caxton*.zip" "run\resourcepacks\" >nul
    )
)

:: Kopiowanie konfiguracji shaderow, sodium i caxtona, jesli brakuje
if exist "%PRISM_DIR%\config" (
    if not exist "run\config\iris.properties" (
        copy /y "%PRISM_DIR%\config\iris.properties" "run\config\" >nul
    )
    if not exist "run\config\sodium-options.json" (
        copy /y "%PRISM_DIR%\config\sodium-options.json" "run\config\" >nul
    )
    if not exist "run\config\sodium-mixins.properties" (
        copy /y "%PRISM_DIR%\config\sodium-mixins.properties" "run\config\" >nul
    )
    if not exist "run\config\caxton.json" (
        copy /y "%PRISM_DIR%\config\caxton.json" "run\config\" >nul
    )
)

:: Upewnij sie, ze iris.properties ma wlaczone shadery i wybrana paczke
if not exist "run\config\iris.properties" (
    (
        echo colorSpace=SRGB
        echo disableUpdateMessage=false
        echo enableDebugOptions=false
        echo enableShaders=true
        echo maxShadowRenderDistance=8
        echo shaderPack=ComplementaryUnbound_r5.9.3.zip
    ) > "run\config\iris.properties"
)

echo Wersja Java:
"%JAVA_HOME%\bin\java.exe" -version
echo.
echo Aktywne dodatki graficzne:
echo   [x] Sodium
echo   [x] ImmediatelyFast
echo   [x] Iris Shaders
echo   [x] Mod Menu
echo   [x] Caxton + MFP Resourcepack
echo   [x] Shaders: Complementary Unbound
echo.
echo Kompilacja i start klienta gry Minecraft...
call gradlew.bat runClient

if %ERRORLEVEL% NEQ 0 (
    echo.
    echo Wystapil problem podczas uruchamiania.
    pause
)

