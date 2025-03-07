@echo off
setlocal EnableDelayedExpansion

:: TableMorph Launcher - Windows Edition
:: This batch file launches TableMorph, installing Java if needed
:: It will also attempt to elevate to administrator privileges if needed

:: Check for admin rights and self-elevate if needed
>nul 2>&1 "%SYSTEMROOT%\system32\cacls.exe" "%SYSTEMROOT%\system32\config\system"
if '%errorlevel%' NEQ '0' (
    echo Requesting administrative privileges...
    echo This is needed to install Java properly.
    goto UACPrompt
) else (
    goto GotAdmin
)

:UACPrompt
echo Set UAC = CreateObject^("Shell.Application"^) > "%temp%\getadmin.vbs"
echo UAC.ShellExecute "%~s0", "", "", "runas", 1 >> "%temp%\getadmin.vbs"
"%temp%\getadmin.vbs"
exit /B

:GotAdmin
if exist "%temp%\getadmin.vbs" ( del "%temp%\getadmin.vbs" )
pushd "%CD%"
CD /D "%~dp0"

:: Display header
echo ********************************************************
echo *            TableMorph Launcher - Windows             *
echo ********************************************************
echo.
echo Running with administrative privileges.
echo.

:: Check if Java is installed
echo Checking Java installation...
java -version >nul 2>&1
if %ERRORLEVEL% NEQ 0 (
    echo Java not found! Installing Java...
    call :InstallJava
) else (
    :: Check Java version
    java -version 2>&1 | findstr /i "version" > java_version.tmp
    
    set JAVA_VERSION=0
    for /f "tokens=3 delims=. " %%j in (java_version.tmp) do (
        set JAVA_VERSION_STR=%%j
        set JAVA_VERSION_STR=!JAVA_VERSION_STR:"=!
        
        if "!JAVA_VERSION_STR:~0,2!" == "1." (
            set JAVA_VERSION=!JAVA_VERSION_STR:~2,1!
        ) else (
            for /f "delims=." %%v in ("!JAVA_VERSION_STR!") do (
                set JAVA_VERSION=%%v
            )
        )
    )
    
    del java_version.tmp
    
    if !JAVA_VERSION! LSS 8 (
        echo Java version !JAVA_VERSION! is too old. Java 8 or higher is required.
        echo Installing Java 17...
        call :InstallJava
    ) else (
        echo Java !JAVA_VERSION! detected!
    )
)

:: Check if the JAR file exists, build if not
set "JAR_FILE=target\tablemorph-1.0-SNAPSHOT-jar-with-dependencies.jar"
if not exist "%JAR_FILE%" (
    echo JAR file not found. Building TableMorph...
    
    if exist "mvnw.cmd" (
        attrib -R mvnw.cmd
        call mvnw.cmd clean package assembly:single
        
        if not exist "%JAR_FILE%" (
            echo Error: Build failed! JAR file not created.
            echo Please check the build output for errors.
            pause
            exit /b 1
        )
        
        echo TableMorph built successfully!
    ) else (
        echo Error: Maven wrapper (mvnw.cmd) not found!
        echo Please ensure you've cloned the complete repository.
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
    echo.
    pause
    exit /b 1
)

exit /b 0

:: Function to install Java
:InstallJava
if not exist "temp" mkdir temp
cd temp

echo Downloading Java 17 installer...
echo This may take a few minutes. Please wait...

:: Determine system architecture
reg Query "HKLM\Hardware\Description\System\CentralProcessor\0" | find /i "x86" > NUL && set "ARCH=x86" || set "ARCH=x64"

:: Set the download URL based on architecture
if "%ARCH%" == "x64" (
    set "DOWNLOAD_URL=https://github.com/adoptium/temurin17-binaries/releases/download/jdk-17.0.8%%2B7/OpenJDK17U-jdk_x64_windows_hotspot_17.0.8_7.msi"
) else (
    set "DOWNLOAD_URL=https://github.com/adoptium/temurin17-binaries/releases/download/jdk-17.0.8%%2B7/OpenJDK17U-jdk_x86-32_windows_hotspot_17.0.8_7.msi"
)

:: Download the installer
echo Downloading Java installer...
bitsadmin /transfer JavaDownload /download /priority high "%DOWNLOAD_URL%" "%CD%\java_installer.msi" >nul

:: Check if download was successful
if not exist "java_installer.msi" (
    echo Trying alternative download method with certutil...
    certutil -urlcache -split -f "%DOWNLOAD_URL%" java_installer.msi >nul
)

:: Check if download was successful
if not exist "java_installer.msi" (
    echo Error: Failed to download Java installer.
    echo Please install Java manually from: https://adoptium.net/
    cd ..
    pause
    exit /b 1
)

:: Install Java
echo Installing Java 17...
echo This may take a few minutes. Please wait...

set "timestamp=%date:~-4,4%%date:~-7,2%%date:~-10,2%_%time:~0,2%%time:~3,2%%time:~6,2%"
set "timestamp=%timestamp: =0%"
set "log_file=install_log_%timestamp%.txt"

echo Installing Java...
msiexec /i java_installer.msi /quiet /qn /norestart /log "%log_file%"

:: Wait for installation to complete
echo Installation in progress...
ping -n 30 127.0.0.1 >nul

:: Clean up
echo Cleaning up temporary files...
cd ..

:: Verify Java installation
echo Verifying Java installation...

:: Update PATH to include Java
echo Updating PATH to include Java...
set "PATH=%PATH%;%ProgramFiles%\Eclipse Adoptium\jdk-17.0.8.7-hotspot\bin;%ProgramFiles(x86)%\Eclipse Adoptium\jdk-17.0.8.7-hotspot\bin"

:: Check if Java is now installed
java -version >nul 2>&1
if %ERRORLEVEL% NEQ 0 (
    echo Error: Java installation failed.
    echo Please install Java manually from: https://adoptium.net/
    pause
    exit /b 1
)

:: Check Java version
java -version 2>&1 | findstr /i "version" > java_version.tmp
    
set JAVA_VERSION=0
for /f "tokens=3 delims=. " %%j in (java_version.tmp) do (
    set JAVA_VERSION_STR=%%j
    set JAVA_VERSION_STR=!JAVA_VERSION_STR:"=!
    
    if "!JAVA_VERSION_STR:~0,2!" == "1." (
        set JAVA_VERSION=!JAVA_VERSION_STR:~2,1!
    ) else (
        for /f "delims=." %%v in ("!JAVA_VERSION_STR!") do (
            set JAVA_VERSION=%%v
        )
    )
)

del java_version.tmp

if !JAVA_VERSION! LSS 8 (
    echo Error: Java installation failed or version is still too old.
    echo Please install Java manually from: https://adoptium.net/
    pause
    exit /b 1
)

echo Java !JAVA_VERSION! installed successfully!

:: Clean up temp directory
if exist "temp" rmdir /s /q temp

goto :eof 