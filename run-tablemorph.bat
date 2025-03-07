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
    :: Check Java version - capture output to a temporary file
    java -version 2>&1 | findstr /i "version" > java_version.tmp
    
    :: Read the version from the temporary file
    set JAVA_VERSION=0
    for /f "tokens=3 delims=. " %%j in (java_version.tmp) do (
        set JAVA_VERSION_STR=%%j
        :: Remove quotes
        set JAVA_VERSION_STR=!JAVA_VERSION_STR:"=!
        
        :: Check if it's a 1.x version
        if "!JAVA_VERSION_STR:~0,2!" == "1." (
            :: Extract the number after 1.
            set JAVA_VERSION=!JAVA_VERSION_STR:~2,1!
        ) else (
            :: Modern version format
            for /f "delims=." %%v in ("!JAVA_VERSION_STR!") do (
                set JAVA_VERSION=%%v
            )
        )
    )
    
    :: Clean up temporary file
    del java_version.tmp
    
    :: Check if version is less than 8
    if !JAVA_VERSION! LSS 8 (
        echo Java version !JAVA_VERSION! is too old. Java 8 or higher is required.
        echo Installing Java 17...
        call :InstallJava
    ) else (
        echo Java !JAVA_VERSION! detected!
    )
)

:: Launch the application
echo.
echo Launching TableMorph...
java -jar "target\tablemorph-1.0-SNAPSHOT-jar-with-dependencies.jar"
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
    :: Create temp directory for downloads
    if not exist "temp" mkdir temp
    cd temp

    :: Download Adoptium JDK installer
    echo Downloading Java 17 installer...
    echo This may take a few minutes. Please wait...

    :: Determine system architecture
    reg Query "HKLM\Hardware\Description\System\CentralProcessor\0" | find /i "x86" > NUL && set ARCH=x86 || set ARCH=x64

    :: Set the download URL based on architecture
    if "%ARCH%" == "x64" (
        set "DOWNLOAD_URL=https://github.com/adoptium/temurin17-binaries/releases/download/jdk-17.0.8%%2B7/OpenJDK17U-jdk_x64_windows_hotspot_17.0.8_7.msi"
    ) else (
        set "DOWNLOAD_URL=https://github.com/adoptium/temurin17-binaries/releases/download/jdk-17.0.8%%2B7/OpenJDK17U-jdk_x86-32_windows_hotspot_17.0.8_7.msi"
    )

    :: Create a simple progress animation for download
    set "anim=|/-\"
    set count=0

    :: Start the download in the background
    echo Downloading Java installer...
    start /b cmd /c bitsadmin /transfer JavaDownload /download /priority high "%DOWNLOAD_URL%" "%CD%\java_installer.msi" ^> nul

    :: Display a spinner while waiting for download to complete
    echo [    ] 
    :download_spinner
    set /a count+=1
    set /a index=count %% 4
    set "c=!anim:~%index%,1!"
    <nul set /p =!c!
    <nul set /p =^H
    
    :: Check if the file exists and has a size greater than 0
    if exist "java_installer.msi" (
        for %%F in (java_installer.msi) do (
            if %%~zF GTR 0 (
                :: Check if bitsadmin is still running
                tasklist | find /i "bitsadmin" >nul
                if errorlevel 1 (
                    :: Download complete
                    echo Download complete!
                    goto :download_complete
                )
            )
        )
    )
    
    :: Still downloading, continue spinner
    ping -n 2 127.0.0.1 >nul
    goto download_spinner

:download_complete
    :: If the download failed, try using certutil as a fallback
    if not exist "java_installer.msi" (
        echo Trying alternative download method with certutil...
        echo This may take a few minutes. Please wait...
        certutil -urlcache -split -f "%DOWNLOAD_URL%" java_installer.msi > nul
        echo Download complete!
    )

    :: Check if download was successful
    if not exist "java_installer.msi" (
        echo Error: Failed to download Java installer.
        echo Please install Java manually from: https://adoptium.net/
        cd ..
        pause
        exit /b 1
    )

    :: Install Java silently with progress display
    echo Installing Java 17...
    echo This may take a few minutes. Please wait...

    :: Reset the animation counter
    set count=0

    :: Create a unique log file name with timestamp
    set "timestamp=%date:~-4,4%%date:~-7,2%%date:~-10,2%_%time:~0,2%%time:~3,2%%time:~6,2%"
    set "timestamp=%timestamp: =0%"
    set "log_file=install_log_%timestamp%.txt"

    :: Create a marker file to track installation start time
    echo %time% > install_start.txt

    :: Start the installation
    echo Installing Java...
    msiexec /i java_installer.msi /quiet /qn /norestart /log "%log_file%"

    :: Installation is complete at this point
    echo Installation complete!

    :: Wait a bit to ensure installation is fully complete
    ping -n 5 127.0.0.1 >nul

    :: Clean up but keep the installer for troubleshooting if needed
    echo Cleaning up temporary files...
    cd ..
    if exist "temp\%log_file%" type "temp\%log_file%"

    :: Verify Java installation
    echo Verifying Java installation...

    :: Update PATH to include Java
    echo Updating PATH to include Java...
    set "PATH=%PATH%;%ProgramFiles%\Eclipse Adoptium\jdk-17.0.8.7-hotspot\bin;%ProgramFiles(x86)%\Eclipse Adoptium\jdk-17.0.8.7-hotspot\bin"

    :: Check if Java is now installed
    java -version >nul 2>&1
    if %ERRORLEVEL% NEQ 0 (
        echo Error: Java installation failed.
        echo Checking installation log for errors...
        
        if exist "temp\%log_file%" (
            echo Installation log contents:
            type "temp\%log_file%"
        )
        
        echo Please install Java manually from: https://adoptium.net/
        pause
        exit /b 1
    )

    :: Check Java version using the same method as above
    java -version 2>&1 | findstr /i "version" > java_version.tmp
    
    :: Read the version from the temporary file
    set JAVA_VERSION=0
    for /f "tokens=3 delims=. " %%j in (java_version.tmp) do (
        set JAVA_VERSION_STR=%%j
        :: Remove quotes
        set JAVA_VERSION_STR=!JAVA_VERSION_STR:"=!
        
        :: Check if it's a 1.x version
        if "!JAVA_VERSION_STR:~0,2!" == "1." (
            :: Extract the number after 1.
            set JAVA_VERSION=!JAVA_VERSION_STR:~2,1!
        ) else (
            :: Modern version format
            for /f "delims=." %%v in ("!JAVA_VERSION_STR!") do (
                set JAVA_VERSION=%%v
            )
        )
    )
    
    :: Clean up temporary file
    del java_version.tmp

    if !JAVA_VERSION! LSS 8 (
        echo Error: Java installation failed or version is still too old.
        echo Please install Java manually from: https://adoptium.net/
        pause
        exit /b 1
    )

    echo Java !JAVA_VERSION! installed successfully!

    :: Now that Java is installed successfully, clean up the temp directory
    rmdir /s /q temp

    goto :eof 