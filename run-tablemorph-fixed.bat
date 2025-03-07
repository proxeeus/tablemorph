@echo off
setlocal EnableDelayedExpansion

echo ********************************************************
echo *            TableMorph Launcher - Windows             *
echo ********************************************************
echo.

:: Check if Java is installed
echo Checking Java installation...
java -version >nul 2>&1
if %ERRORLEVEL% NEQ 0 (
    echo Java not found! Please install Java 8 or higher.
    echo Visit: https://adoptium.net/
    pause
    exit /b 1
) else (
    echo Java detected! Continuing...
)

:: Check if the JAR file exists
set "JAR_FILE=target\tablemorph-1.0-SNAPSHOT-jar-with-dependencies.jar"
if not exist "%JAR_FILE%" (
    echo JAR file not found. Building TableMorph...
    
    if exist "mvnw.cmd" (
        call mvnw.cmd clean package assembly:single
        
        if not exist "%JAR_FILE%" (
            echo Error: Build failed! JAR file not created.
            pause
            exit /b 1
        )
        
        echo TableMorph built successfully!
    ) else (
        echo Error: Maven wrapper (mvnw.cmd) not found!
        pause
        exit /b 1
    )
)

:: Launch the application
echo.
echo Launching TableMorph...
java -jar "%JAR_FILE%"
if %ERRORLEVEL% NEQ 0 (
    echo.
    echo Error: Failed to launch TableMorph.
    pause
    exit /b 1
)

exit /b 0 