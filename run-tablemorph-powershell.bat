@echo off
setlocal EnableDelayedExpansion

:: TableMorph PowerShell Launcher
:: This batch file launches the PowerShell script with the correct execution policy
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
echo *     TableMorph PowerShell Launcher - Windows        *
echo ********************************************************
echo.
echo Running with administrative privileges.
echo.

:: Launch the PowerShell script with execution policy bypass
echo Launching TableMorph PowerShell script...
powershell.exe -ExecutionPolicy Bypass -Command "& {Start-Process PowerShell.exe -ArgumentList '-ExecutionPolicy Bypass -File \"%~dp0run-tablemorph.ps1\" -AdminMode' -Verb RunAs}"

:: If PowerShell fails, provide instructions
if %ERRORLEVEL% NEQ 0 (
    echo.
    echo Error: Failed to launch PowerShell script.
    echo.
    echo Try running the PowerShell script directly:
    echo 1. Right-click on run-tablemorph.ps1 and select "Run as administrator"
    echo 2. Or open PowerShell as Administrator and run:
    echo    powershell -ExecutionPolicy Bypass -File "%~dp0run-tablemorph.ps1"
    echo.
    echo If you're still having issues, you may need to install Java manually:
    echo 1. Download Java from: https://adoptium.net/
    echo 2. Install Java with administrative privileges
    echo 3. Then run TableMorph again
    echo.
    pause
    exit /b 1
)

exit /b 0 