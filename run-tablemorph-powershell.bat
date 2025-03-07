@echo off
:: TableMorph PowerShell Launcher
:: This batch file launches the PowerShell script with the correct execution policy

echo ********************************************************
echo *     TableMorph PowerShell Launcher - Windows        *
echo ********************************************************
echo.

:: Launch the PowerShell script with execution policy bypass
echo Launching TableMorph PowerShell script...
powershell.exe -ExecutionPolicy Bypass -File "%~dp0run-tablemorph.ps1"

:: If PowerShell fails, provide instructions
if %ERRORLEVEL% NEQ 0 (
    echo.
    echo Error: Failed to launch PowerShell script.
    echo.
    echo Try running the PowerShell script directly:
    echo 1. Right-click on run-tablemorph.ps1 and select "Run with PowerShell"
    echo 2. Or open PowerShell as Administrator and run:
    echo    powershell -ExecutionPolicy Bypass -File "%~dp0run-tablemorph.ps1"
    echo.
    pause
    exit /b 1
)

exit /b 0 