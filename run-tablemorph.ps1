# TableMorph Launcher - PowerShell Edition
# To run this script, you may need to set the execution policy:
# Open PowerShell as Administrator and run: Set-ExecutionPolicy -ExecutionPolicy Bypass -Scope Process -Force
# Or run this script directly with: powershell -ExecutionPolicy Bypass -File run-tablemorph.ps1

# ANSI color codes
$GREEN = "`e[0;32m"
$YELLOW = "`e[1;33m"
$RED = "`e[0;31m"
$BLUE = "`e[0;34m"
$NC = "`e[0m" # No Color

# Function to show a progress bar
function Show-Progress {
    param (
        [int]$PercentComplete,
        [string]$Status
    )
    
    $width = 50
    $completeChars = [math]::Floor($width * $PercentComplete / 100)
    $remainingChars = $width - $completeChars
    
    $progressBar = "["
    $progressBar += "=" * $completeChars
    if ($completeChars -lt $width) {
        $progressBar += ">"
        $progressBar += " " * ($remainingChars - 1)
    }
    $progressBar += "]"
    
    Write-Host "`r$progressBar $PercentComplete% $Status" -NoNewline
}

# Function to download file with progress
function Download-FileWithProgress {
    param (
        [string]$Url,
        [string]$OutputPath
    )
    
    try {
        $webClient = New-Object System.Net.WebClient
        $totalBytes = 0
        $receivedBytes = 0
        $lastPercent = 0
        
        # Get file size
        try {
            $request = [System.Net.WebRequest]::Create($Url)
            $response = $request.GetResponse()
            $totalBytes = $response.ContentLength
            $response.Close()
        } catch {
            Write-Host "Could not determine file size. Download will proceed without accurate progress reporting."
            $totalBytes = 100000000  # Assume 100MB if we can't get the size
        }
        
        # Set up progress event
        $webClient.DownloadProgressChanged = {
            param($sender, $e)
            $receivedBytes = $e.BytesReceived
            $percent = [math]::Min(100, [math]::Floor($receivedBytes * 100 / $totalBytes))
            
            # Only update when percentage changes to reduce console output
            if ($percent -ne $lastPercent) {
                Show-Progress -PercentComplete $percent -Status "Downloading Java installer"
                $lastPercent = $percent
            }
        }
        
        $webClient.DownloadFileCompleted = {
            param($sender, $e)
            if ($e.Error -ne $null) {
                Write-Host "`nDownload failed: $($e.Error.Message)"
                $global:downloadSuccess = $false
            } else {
                Write-Host "`nDownload completed successfully!"
                $global:downloadSuccess = $true
            }
            $global:downloadCompleted = $true
        }
        
        # Start download
        Write-Host "Starting download from $Url"
        Show-Progress -PercentComplete 0 -Status "Initializing download..."
        $global:downloadCompleted = $false
        $global:downloadSuccess = $false
        
        $webClient.DownloadFileAsync([System.Uri]$Url, $OutputPath)
        
        # Wait for download to complete
        while (-not $global:downloadCompleted) {
            Start-Sleep -Milliseconds 100
        }
        
        return $global:downloadSuccess
    } catch {
        Write-Host "`nDownload failed with error: $_"
        return $false
    }
}

# Print header
Write-Host ""
Write-Host "$BLUE╔════════════════════════════════════════════════════════════╗$NC"
Write-Host "$BLUE║          TableMorph Launcher - PowerShell                  ║$NC"
Write-Host "$BLUE╚════════════════════════════════════════════════════════════╝$NC"
Write-Host ""

# Function to install Java
function Install-Java {
    Write-Host "${YELLOW}Attempting to install Java automatically...$NC"
    
    # Create temp directory for downloads
    $tempDir = Join-Path -Path $PSScriptRoot -ChildPath "temp"
    if (-not (Test-Path -Path $tempDir -PathType Container)) {
        New-Item -Path $tempDir -ItemType Directory | Out-Null
    }
    
    # Determine system architecture
    $arch = if ([Environment]::Is64BitOperatingSystem) { "x64" } else { "x86-32" }
    
    # Download Adoptium JDK installer
    Write-Host "${YELLOW}Downloading Java 17 installer...$NC"
    $installerUrl = if ($arch -eq "x64") {
        "https://github.com/adoptium/temurin17-binaries/releases/download/jdk-17.0.8%2B7/OpenJDK17U-jdk_x64_windows_hotspot_17.0.8_7.msi"
    } else {
        "https://github.com/adoptium/temurin17-binaries/releases/download/jdk-17.0.8%2B7/OpenJDK17U-jdk_x86-32_windows_hotspot_17.0.8_7.msi"
    }
    
    $installerPath = Join-Path -Path $tempDir -ChildPath "java_installer.msi"
    
    # Try multiple download methods
    $downloadSuccess = $false
    
    # Method 1: WebClient with progress
    if (-not $downloadSuccess) {
        Write-Host "Trying download with progress display..."
        $downloadSuccess = Download-FileWithProgress -Url $installerUrl -OutputPath $installerPath
    }
    
    # Method 2: BITS Transfer (Windows only) with progress
    if (-not $downloadSuccess -and (Get-Command Start-BitsTransfer -ErrorAction SilentlyContinue)) {
        try {
            Write-Host "Trying BITS transfer download method with progress..."
            Start-BitsTransfer -Source $installerUrl -Destination $installerPath -DisplayName "Downloading Java Installer" -Description "Downloading Java 17 installer from Adoptium" -Priority High
            if (Test-Path -Path $installerPath -PathType Leaf) {
                $downloadSuccess = $true
                Write-Host "Download completed successfully!"
            }
        } catch {
            Write-Host "BITS transfer download failed: $_"
        }
    }
    
    # Method 3: Invoke-WebRequest as fallback
    if (-not $downloadSuccess) {
        try {
            Write-Host "Trying Invoke-WebRequest download method..."
            Write-Host "This may take a few minutes. Please wait..."
            
            # Create a progress indicator
            $job = Start-Job -ScriptBlock {
                $i = 0
                $chars = '|', '/', '-', '\'
                while ($true) {
                    Write-Host "`rDownloading... $($chars[$i % 4])" -NoNewline
                    Start-Sleep -Milliseconds 250
                    $i++
                }
            }
            
            # Perform the download
            Invoke-WebRequest -Uri $installerUrl -OutFile $installerPath -UseBasicParsing
            
            # Stop the progress indicator
            Stop-Job -Job $job
            Remove-Job -Job $job
            Write-Host "`rDownload complete!                     "
            
            if (Test-Path -Path $installerPath -PathType Leaf) {
                $downloadSuccess = $true
            }
        } catch {
            Write-Host "Invoke-WebRequest download failed: $_"
            
            # Stop the progress indicator if it's still running
            if (Get-Job -Id $job.Id -ErrorAction SilentlyContinue) {
                Stop-Job -Job $job
                Remove-Job -Job $job
            }
        }
    }
    
    # Check if download was successful
    if (-not (Test-Path -Path $installerPath -PathType Leaf) -or -not $downloadSuccess) {
        Write-Host "${RED}Error: Failed to download Java installer.$NC"
        Write-Host "${YELLOW}Please install Java manually from: https://adoptium.net/$NC"
        Remove-Item -Path $tempDir -Recurse -Force -ErrorAction SilentlyContinue
        Read-Host "Press Enter to exit"
        exit 1
    }
    
    # Install Java silently with progress display
    Write-Host "${YELLOW}Installing Java 17...$NC"
    Write-Host "This may take a few minutes. Please wait..."
    
    # Create a log file for the installation
    $logFile = Join-Path -Path $tempDir -ChildPath "install_log.txt"
    
    # Create a progress indicator for installation
    $installJob = Start-Job -ScriptBlock {
        $i = 0
        $chars = '|', '/', '-', '\'
        while ($true) {
            Write-Host "`rInstalling Java... $($chars[$i % 4])" -NoNewline
            Start-Sleep -Milliseconds 250
            $i++
        }
    }
    
    try {
        # Start the installation process
        $process = Start-Process -FilePath "msiexec.exe" -ArgumentList "/i", "`"$installerPath`"", "/quiet", "/qn", "/norestart", "/log", "`"$logFile`"" -Wait -PassThru
        
        # Stop the progress indicator
        Stop-Job -Job $installJob
        Remove-Job -Job $installJob
        Write-Host "`rInstallation complete!                     "
        
        if ($process.ExitCode -ne 0) {
            throw "MSI installer returned exit code: $($process.ExitCode)"
        }
    } catch {
        # Stop the progress indicator if it's still running
        if (Get-Job -Id $installJob.Id -ErrorAction SilentlyContinue) {
            Stop-Job -Job $installJob
            Remove-Job -Job $installJob
        }
        
        Write-Host "`r${RED}Error during Java installation: $_$NC"
        
        # Display the installation log if available
        if (Test-Path -Path $logFile) {
            Write-Host "Installation log contents:"
            Get-Content -Path $logFile | Select-Object -Last 20
        }
        
        Write-Host "${YELLOW}Please install Java manually from: https://adoptium.net/$NC"
        # Don't remove the temp directory so the log file can be examined
        Read-Host "Press Enter to exit"
        exit 1
    }
    
    # Verify Java installation
    Write-Host "${YELLOW}Verifying Java installation...$NC"
    
    # Update PATH to include Java
    Write-Host "Updating PATH to include Java..."
    $env:Path = [System.Environment]::GetEnvironmentVariable("Path", "Machine") + ";" + [System.Environment]::GetEnvironmentVariable("Path", "User")
    
    # Add potential Java paths
    $javaLocations = @(
        "${env:ProgramFiles}\Eclipse Adoptium\jdk-17.0.8.7-hotspot\bin",
        "${env:ProgramFiles(x86)}\Eclipse Adoptium\jdk-17.0.8.7-hotspot\bin"
    )
    
    foreach ($location in $javaLocations) {
        if (Test-Path -Path $location) {
            Write-Host "Adding Java location to PATH: $location"
            $env:Path += ";$location"
        }
    }
    
    # Check if Java is now installed
    try {
        $javaInfo = Get-Command java -ErrorAction Stop
        $javaPath = $javaInfo.Source
        Write-Host "Java found at: $javaPath"
        
        $versionOutput = & java -version 2>&1
        $versionString = $versionOutput -join "`n"
        Write-Host "Java version output: $versionString"
        
        if ($versionString -match "version `"(\d+)\.(\d+)") {
            $majorVersion = if ($Matches[1] -eq "1") { $Matches[2] } else { $Matches[1] }
            
            if ([int]$majorVersion -lt 8) {
                Write-Host "${RED}Error: Java installation failed or version is still too old.$NC"
                
                # Display the installation log if available
                if (Test-Path -Path $logFile) {
                    Write-Host "Installation log contents:"
                    Get-Content -Path $logFile | Select-Object -Last 20
                }
                
                Write-Host "${YELLOW}Please install Java 8 or higher manually from: https://adoptium.net/$NC"
                Read-Host "Press Enter to exit"
                exit 1
            }
            
            Write-Host "${GREEN}Java $majorVersion installed successfully!$NC"
        } else {
            throw "Could not parse Java version"
        }
    } catch {
        Write-Host "${RED}Error: Java installation failed: $_$NC"
        
        # Display the installation log if available
        if (Test-Path -Path $logFile) {
            Write-Host "Installation log contents:"
            Get-Content -Path $logFile | Select-Object -Last 20
        }
        
        Write-Host "${YELLOW}Please install Java manually from: https://adoptium.net/$NC"
        Read-Host "Press Enter to exit"
        exit 1
    }
    
    # Now that Java is installed successfully, clean up the temp directory
    Remove-Item -Path $tempDir -Recurse -Force -ErrorAction SilentlyContinue
}

# Check if Java is installed and has the correct version
Write-Host "${YELLOW}Checking Java installation...$NC"
try {
    $javaInfo = Get-Command java -ErrorAction Stop
    $javaPath = $javaInfo.Source
    Write-Host "Java found at: $javaPath"
    
    $versionOutput = & java -version 2>&1
    $versionString = $versionOutput -join "`n"
    Write-Host "Java version output: $versionString"
    
    if ($versionString -match "version `"(\d+)\.(\d+)") {
        $majorVersion = if ($Matches[1] -eq "1") { $Matches[2] } else { $Matches[1] }
        
        if ([int]$majorVersion -lt 8) {
            Write-Host "${YELLOW}Java version $majorVersion is too old. Java 8 or higher is required.$NC"
            $installJava = Read-Host "Would you like to install Java 17 automatically? (Y/N)"
            if ($installJava -eq "Y" -or $installJava -eq "y") {
                Install-Java
            } else {
                Write-Host "${RED}Error: Java 8 or higher is required. Found version: $majorVersion$NC"
                Write-Host "${YELLOW}Please install Java 8 or higher to run TableMorph.$NC"
                Write-Host "${YELLOW}You can download it from: https://adoptium.net/$NC"
                Write-Host ""
                Read-Host "Press Enter to exit"
                exit 1
            }
        } else {
            Write-Host "${GREEN}Java $majorVersion detected!$NC"
        }
    } else {
        throw "Could not parse Java version"
    }
} catch {
    Write-Host "${YELLOW}Java not found! Error: $_$NC"
    $installJava = Read-Host "Would you like to install Java 17 automatically? (Y/N)"
    if ($installJava -eq "Y" -or $installJava -eq "y") {
        Install-Java
    } else {
        Write-Host "${RED}Error: Java not found!$NC"
        Write-Host "${YELLOW}Please install Java 8 or higher to run TableMorph.$NC"
        Write-Host "${YELLOW}You can download it from: https://adoptium.net/$NC"
        Write-Host ""
        Read-Host "Press Enter to exit"
        exit 1
    }
}

# Create wavetables directory if it doesn't exist
if (-not (Test-Path -Path "wavetables" -PathType Container)) {
    Write-Host "${YELLOW}Creating wavetables directory...$NC"
    New-Item -Path "wavetables" -ItemType Directory | Out-Null
    Write-Host "${GREEN}Wavetables directory created!$NC"
}

# Check if the JAR file exists, build if not
$jarFile = Join-Path -Path $PSScriptRoot -ChildPath "target\tablemorph-1.0-SNAPSHOT-jar-with-dependencies.jar"
if (-not (Test-Path -Path $jarFile -PathType Leaf)) {
    Write-Host "${YELLOW}Building TableMorph...$NC"
    
    # Check if Maven Wrapper exists
    $mvnwPath = Join-Path -Path $PSScriptRoot -ChildPath "mvnw.cmd"
    if (Test-Path -Path $mvnwPath -PathType Leaf) {
        Push-Location $PSScriptRoot
        
        # Create a progress indicator for the build
        $buildJob = Start-Job -ScriptBlock {
            $i = 0
            $chars = '|', '/', '-', '\'
            while ($true) {
                Write-Host "`rBuilding TableMorph... $($chars[$i % 4])" -NoNewline
                Start-Sleep -Milliseconds 250
                $i++
            }
        }
        
        try {
            # Run the build
            & $mvnwPath clean package assembly:single
            
            # Stop the progress indicator
            Stop-Job -Job $buildJob
            Remove-Job -Job $buildJob
            Write-Host "`rBuild complete!                     "
            
            if (-not (Test-Path -Path $jarFile -PathType Leaf)) {
                throw "Build completed but JAR file not found"
            }
            
            Write-Host "${GREEN}TableMorph built successfully!$NC"
        } catch {
            # Stop the progress indicator if it's still running
            if (Get-Job -Id $buildJob.Id -ErrorAction SilentlyContinue) {
                Stop-Job -Job $buildJob
                Remove-Job -Job $buildJob
            }
            
            Write-Host "`r${RED}Error: Build failed: $_$NC"
            Read-Host "Press Enter to exit"
            exit 1
        } finally {
            Pop-Location
        }
    } else {
        Write-Host "${RED}Error: Maven Wrapper (mvnw.cmd) not found at $mvnwPath!$NC"
        Write-Host "${YELLOW}Please ensure you've cloned the complete repository.$NC"
        Read-Host "Press Enter to exit"
        exit 1
    }
}

# Launch the application
Write-Host "${YELLOW}Launching TableMorph...$NC"
try {
    & java -jar $jarFile
} catch {
    Write-Host "${RED}Error launching TableMorph: $_$NC"
    Read-Host "Press Enter to exit"
    exit 1
} 