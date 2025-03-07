@echo off
setlocal EnableDelayedExpansion

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

:: Call the check_java function at the beginning
goto :check_java

:: Function to install Java
:install_java
if "%COLORED%" == "yes" echo %YELLOW%
echo Attempting to install Java automatically...
if "%COLORED%" == "yes" echo %NC%

:: Create temp directory for downloads
if not exist "temp" mkdir temp
cd temp

:: Download Adoptium JDK installer
if "%COLORED%" == "yes" echo %YELLOW%
echo Downloading Java 17 installer...
if "%COLORED%" == "yes" echo %NC%

:: Determine system architecture
reg Query "HKLM\Hardware\Description\System\CentralProcessor\0" | find /i "x86" > NUL && set ARCH=x86 || set ARCH=x64

:: Set the download URL based on architecture
if "%ARCH%" == "x64" (
    set "DOWNLOAD_URL=https://github.com/adoptium/temurin17-binaries/releases/download/jdk-17.0.8%%2B7/OpenJDK17U-jdk_x64_windows_hotspot_17.0.8_7.msi"
) else (
    set "DOWNLOAD_URL=https://github.com/adoptium/temurin17-binaries/releases/download/jdk-17.0.8%%2B7/OpenJDK17U-jdk_x86-32_windows_hotspot_17.0.8_7.msi"
)

:: Try using PowerShell with progress display
echo Downloading Java installer with progress display...
powershell -Command "$ProgressPreference = 'Continue'; $wc = New-Object System.Net.WebClient; $wc.DownloadFile('%DOWNLOAD_URL%', 'java_installer.msi'); Write-Host 'Download complete!'"

:: If PowerShell fails, try using bitsadmin with progress display
if not exist "java_installer.msi" (
    echo Trying alternative download method with BITS...
    echo This may take a few minutes. Please wait...
    bitsadmin /transfer JavaDownload /download /priority normal "%DOWNLOAD_URL%" "%CD%\java_installer.msi"
    echo Download complete!
)

:: If bitsadmin fails, try using certutil as a fallback
if not exist "java_installer.msi" (
    echo Trying alternative download method with certutil...
    echo This may take a few minutes. Please wait...
    certutil -urlcache -split -f "%DOWNLOAD_URL%" java_installer.msi
    echo Download complete!
)

:: Check if download was successful
if not exist "java_installer.msi" (
    if "%COLORED%" == "yes" echo %RED%
    echo Error: Failed to download Java installer.
    if "%COLORED%" == "yes" echo %YELLOW%
    echo Please install Java manually from: https://adoptium.net/
    if "%COLORED%" == "yes" echo %NC%
    cd ..
    pause
    exit /b 1
)

:: Install Java silently with progress display
if "%COLORED%" == "yes" echo %YELLOW%
echo Installing Java 17...
echo This may take a few minutes. Please wait...
if "%COLORED%" == "yes" echo %NC%

:: Create a simple progress animation
set "anim=|/-\"
set count=0

:: Start the installation in the background
start /b "" msiexec /i java_installer.msi /quiet /qn /norestart /log install_log.txt

:: Display a spinner while waiting for installation to complete
echo Installing Java 
:spinner
set /a count+=1
set /a index=count %% 4
set "c=!anim:~%index%,1!"
<nul set /p =!c!
<nul set /p =^H
:: Check if msiexec is still running
tasklist | find /i "msiexec" >nul
if not errorlevel 1 (
    :: Still running, continue spinner
    ping -n 2 127.0.0.1 >nul
    goto spinner
) else (
    :: Installation complete
    echo Installation complete!
)

:: Wait a bit to ensure installation is fully complete
ping -n 5 127.0.0.1 >nul

:: Clean up but keep the installer for troubleshooting if needed
echo Cleaning up temporary files...
cd ..
if exist "temp\install_log.txt" type temp\install_log.txt
:: Don't delete the temp folder immediately in case we need to debug

:: Verify Java installation
if "%COLORED%" == "yes" echo %YELLOW%
echo Verifying Java installation...
if "%COLORED%" == "yes" echo %NC%

:: Update PATH to include Java
echo Updating PATH to include Java...
set "PATH=%PATH%;%ProgramFiles%\Eclipse Adoptium\jdk-17.0.8.7-hotspot\bin;%ProgramFiles(x86)%\Eclipse Adoptium\jdk-17.0.8.7-hotspot\bin"

:: Check if Java is now installed
java -version >nul 2>&1
if %ERRORLEVEL% NEQ 0 (
    if "%COLORED%" == "yes" echo %RED%
    echo Error: Java installation failed.
    echo Checking installation log for errors...
    
    if exist "temp\install_log.txt" (
        echo Installation log contents:
        type temp\install_log.txt
    )
    
    if "%COLORED%" == "yes" echo %YELLOW%
    echo Please install Java manually from: https://adoptium.net/
    if "%COLORED%" == "yes" echo %NC%
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
    echo Error: Java installation failed or version is still too old.
    if "%COLORED%" == "yes" echo %YELLOW%
    echo Please install Java manually from: https://adoptium.net/
    if "%COLORED%" == "yes" echo %NC%
    pause
    exit /b 1
)

if "%COLORED%" == "yes" echo %GREEN%
echo Java %JAVA_VERSION% installed successfully!
if "%COLORED%" == "yes" echo %NC%

:: Now that Java is installed successfully, clean up the temp directory
rmdir /s /q temp

goto :continue_after_java

:: Check if Java is installed and has the correct version
:check_java
:: Print header
echo.
if "%COLORED%" == "yes" echo %BLUE%
echo ********************************************************
echo *            TableMorph Launcher - Windows             *
echo ********************************************************
if "%COLORED%" == "yes" echo %NC%
echo.

if "%COLORED%" == "yes" echo %YELLOW%
echo Checking Java installation...
if "%COLORED%" == "yes" echo %NC%

java -version >nul 2>&1
if %ERRORLEVEL% NEQ 0 (
    if "%COLORED%" == "yes" echo %YELLOW%
    echo Java not found!
    echo Would you like to install Java 17 automatically? (Y/N)
    if "%COLORED%" == "yes" echo %NC%
    
    set /p INSTALL_JAVA=
    if /i "%INSTALL_JAVA%" == "Y" goto :install_java
    
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
    if "%COLORED%" == "yes" echo %YELLOW%
    echo Java version %JAVA_VERSION% is too old. Java 8 or higher is required.
    echo Would you like to install Java 17 automatically? (Y/N)
    if "%COLORED%" == "yes" echo %NC%
    
    set /p INSTALL_JAVA=
    if /i "%INSTALL_JAVA%" == "Y" goto :install_java
    
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

:continue_after_java

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