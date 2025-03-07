@echo off
setlocal EnableDelayedExpansion

echo DEBUG: Starting script
echo ********************************************************
echo *            TableMorph Launcher - Windows             *
echo ********************************************************
echo.

echo DEBUG: Checking Java installation
:: Check if Java is installed
echo Checking Java installation...
java -version >nul 2>&1
echo DEBUG: ERRORLEVEL after java check is %ERRORLEVEL%
if %ERRORLEVEL% NEQ 0 (
    echo DEBUG: Java not found branch
    echo Java not found! Please install Java 8 or higher.
    echo Visit: https://adoptium.net/
    pause
    exit /b 1
) else (
    echo DEBUG: Java found branch
    :: Check Java version - capture output to a temporary file
    echo DEBUG: About to check Java version
    java -version 2>&1 | findstr /i "version" > java_version.tmp
    echo DEBUG: Created version temp file
    
    :: Read the version from the temporary file
    set JAVA_VERSION=0
    echo DEBUG: About to parse version
    for /f "tokens=3 delims=. " %%j in (java_version.tmp) do (
        echo DEBUG: Inside for loop
        set JAVA_VERSION_STR=%%j
        echo DEBUG: Set version string to !JAVA_VERSION_STR!
        :: Remove quotes
        set JAVA_VERSION_STR=!JAVA_VERSION_STR:"=!
        echo DEBUG: Removed quotes: !JAVA_VERSION_STR!
        
        :: Check if it's a 1.x version
        echo DEBUG: Checking if 1.x version
        if "!JAVA_VERSION_STR:~0,2!" == "1." (
            echo DEBUG: Is 1.x version
            :: Extract the number after 1.
            set JAVA_VERSION=!JAVA_VERSION_STR:~2,1!
            echo DEBUG: Set version to !JAVA_VERSION!
        ) else (
            echo DEBUG: Not 1.x version
            :: Modern version format
            for /f "delims=." %%v in ("!JAVA_VERSION_STR!") do (
                echo DEBUG: Inside inner for loop
                set JAVA_VERSION=%%v
                echo DEBUG: Set version to !JAVA_VERSION!
            )
        )
    )
    
    echo DEBUG: After version parsing
    
    :: Clean up temporary file
    del java_version.tmp
    echo DEBUG: Deleted temp file
    
    :: Check if version is less than 8
    echo DEBUG: About to check if version < 8
    if !JAVA_VERSION! LSS 8 (
        echo DEBUG: Version too old
        echo Java version !JAVA_VERSION! is too old. Java 8 or higher is required.
        pause
        exit /b 1
    ) else (
        echo DEBUG: Version OK
        echo Java !JAVA_VERSION! detected!
    )
)

echo DEBUG: After Java check
echo DEBUG: About to check JAR file

:: Check if the JAR file exists
set "JAR_FILE=target\tablemorph-1.0-SNAPSHOT-jar-with-dependencies.jar"
echo DEBUG: Set JAR_FILE to %JAR_FILE%
if not exist "%JAR_FILE%" (
    echo DEBUG: JAR not found
    echo JAR file not found. Building TableMorph...
    
    :: Check if Maven wrapper exists
    if exist "mvnw.cmd" (
        echo DEBUG: Maven wrapper found
        :: Make sure the Maven wrapper is executable
        attrib -R mvnw.cmd
        
        :: Build the project using Maven wrapper
        call mvnw.cmd clean package assembly:single
        
        if not exist "%JAR_FILE%" (
            echo DEBUG: Build failed
            echo Error: Build failed! JAR file not created.
            echo Please check the build output for errors.
            pause
            exit /b 1
        )
        
        echo DEBUG: Build succeeded
        echo TableMorph built successfully!
    ) else (
        echo DEBUG: Maven wrapper not found
        echo Error: Maven wrapper (mvnw.cmd) not found!
        echo Please ensure you've cloned the complete repository.
        pause
        exit /b 1
    )
)

echo DEBUG: After JAR check
echo DEBUG: About to launch application

:: Launch the application
echo.
echo Launching TableMorph...
java -jar "%JAR_FILE%"
echo DEBUG: After launching application, ERRORLEVEL=%ERRORLEVEL%
if %ERRORLEVEL% NEQ 0 (
    echo DEBUG: Launch failed
    echo.
    echo Error: Failed to launch TableMorph.
    echo.
    pause
    exit /b 1
)

echo DEBUG: Exiting script
exit /b 0 