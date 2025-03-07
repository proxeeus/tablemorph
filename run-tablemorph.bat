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

    :: Create a PowerShell script to download with progress
    echo $progressPreference = 'Continue' > download.ps1
    echo $webClient = New-Object System.Net.WebClient >> download.ps1
    echo $uri = New-Object System.Uri('%DOWNLOAD_URL%') >> download.ps1
    echo $file = '%CD%\java_installer.msi' >> download.ps1
    echo Write-Host "Downloading Java installer..." >> download.ps1
    echo $totalLength = 0 >> download.ps1
    echo try { >> download.ps1
    echo     $request = [System.Net.HttpWebRequest]::Create($uri) >> download.ps1
    echo     $response = $request.GetResponse() >> download.ps1
    echo     $totalLength = $response.ContentLength >> download.ps1
    echo     $response.Close() >> download.ps1
    echo } catch { >> download.ps1
    echo     Write-Host "Could not determine file size. Download will proceed without accurate progress reporting." >> download.ps1
    echo     $totalLength = 100000000 >> download.ps1
    echo } >> download.ps1
    echo $webClient.DownloadProgressChanged = { >> download.ps1
    echo     $percent = [Math]::Floor($_.BytesReceived * 100 / $totalLength) >> download.ps1
    echo     $bar = "[" + ("=" * [Math]::Floor($percent/2)) + ">" + (" " * (50 - [Math]::Floor($percent/2))) + "]" >> download.ps1
    echo     Write-Host -NoNewline "`r$bar $percent%% " >> download.ps1
    echo } >> download.ps1
    echo $webClient.DownloadFileCompleted = { >> download.ps1
    echo     Write-Host "`nDownload complete!" >> download.ps1
    echo } >> download.ps1
    echo $webClient.DownloadFileAsync($uri, $file) >> download.ps1
    echo while ($webClient.IsBusy) { Start-Sleep -Milliseconds 100 } >> download.ps1

    :: Run the PowerShell script to download with progress
    powershell -ExecutionPolicy Bypass -File download.ps1

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
        echo Error: Failed to download Java installer.
        echo Please install Java manually from: https://adoptium.net/
        cd ..
        pause
        exit /b 1
    )

    :: Install Java silently with progress display
    echo Installing Java 17...
    echo This may take a few minutes. Please wait...

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
    echo Verifying Java installation...

    :: Update PATH to include Java
    echo Updating PATH to include Java...
    set "PATH=%PATH%;%ProgramFiles%\Eclipse Adoptium\jdk-17.0.8.7-hotspot\bin;%ProgramFiles(x86)%\Eclipse Adoptium\jdk-17.0.8.7-hotspot\bin"

    :: Check if Java is now installed
    java -version >nul 2>&1
    if %ERRORLEVEL% NEQ 0 (
        echo Error: Java installation failed.
        echo Checking installation log for errors...
        
        if exist "temp\install_log.txt" (
            echo Installation log contents:
            type temp\install_log.txt
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