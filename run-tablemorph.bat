@echo off
setlocal

:: ANSI color codes for Windows 10+
set "GREEN=[0;32m"
set "YELLOW=[1;33m"
set "RED=[0;31m"
set "BLUE=[0;34m"
set "NC=[0m"

:: Check if ANSI colors are supported (Windows 10+)
for /f "tokens=4-5 delims=. " %%i in ('ver') do set VERSION=%%i.%%j
if "%version%" == "10.0" (
    set "COLORED=yes"
) else (
    set "COLORED=no"
)

:: Print header
echo.
if "%COLORED%" == "yes" echo %BLUE%
echo ********************************************************
echo *            TableMorph Launcher - Windows             *
echo ********************************************************
if "%COLORED%" == "yes" echo %NC%
echo.

:: Check if Java is installed and has the correct version
if "%COLORED%" == "yes" echo %YELLOW%
echo Checking Java installation...
if "%COLORED%" == "yes" echo %NC%

java -version >nul 2>&1
if %ERRORLEVEL% NEQ 0 (
    if "%COLORED%" == "yes" echo %RED%
    echo Error: Java not found!
    if "%COLORED%" == "yes" echo %YELLOW%
    echo Please install Java 8 or higher to run TableMorph.
    echo You can download it from: https://adoptium.net/
    if "%COLORED%" == "yes" echo %NC%
    echo.
    pause
    exit /b 1
)

:: Check Java version
for /f tokens^=3^ delims^=.^"^  %%j in ('java -version 2^>^&1 ^| findstr /i "version"') do (
    set JAVA_VERSION=%%j
)

:: Handle "1.8" style version
for /f tokens^=1^ delims^=. %%v in ("%JAVA_VERSION%") do (
    if "%%v" == "1" (
        for /f tokens^=2^ delims^=. %%w in ("%JAVA_VERSION%") do (
            set JAVA_VERSION=%%w
        )
    )
)

if %JAVA_VERSION% LSS 8 (
    if "%COLORED%" == "yes" echo %RED%
    echo Error: Java 8 or higher is required. Found version: %JAVA_VERSION%
    if "%COLORED%" == "yes" echo %YELLOW%
    echo Please install Java 8 or higher to run TableMorph.
    echo You can download it from: https://adoptium.net/
    if "%COLORED%" == "yes" echo %NC%
    echo.
    pause
    exit /b 1
)

if "%COLORED%" == "yes" echo %GREEN%
echo Java %JAVA_VERSION% detected!
if "%COLORED%" == "yes" echo %NC%

:: Create wavetables directory if it doesn't exist
if not exist "wavetables\" (
    if "%COLORED%" == "yes" echo %YELLOW%
    echo Creating wavetables directory...
    if "%COLORED%" == "yes" echo %NC%
    mkdir wavetables
    if "%COLORED%" == "yes" echo %GREEN%
    echo Wavetables directory created!
    if "%COLORED%" == "yes" echo %NC%
)

:: Check if the JAR file exists, build if not
set JAR_FILE=target\tablemorph-1.0-SNAPSHOT-jar-with-dependencies.jar
if not exist "%JAR_FILE%" (
    if "%COLORED%" == "yes" echo %YELLOW%
    echo Building TableMorph...
    if "%COLORED%" == "yes" echo %NC%
    
    :: Build the project using Maven Wrapper
    call mvnw.cmd clean package assembly:single
    
    if not exist "%JAR_FILE%" (
        if "%COLORED%" == "yes" echo %RED%
        echo Error: Build failed!
        if "%COLORED%" == "yes" echo %NC%
        pause
        exit /b 1
    )
    
    if "%COLORED%" == "yes" echo %GREEN%
    echo TableMorph built successfully!
    if "%COLORED%" == "yes" echo %NC%
)

:: Launch the application
if "%COLORED%" == "yes" echo %YELLOW%
echo Launching TableMorph...
if "%COLORED%" == "yes" echo %NC%
java -jar "%JAR_FILE%"

endlocal 