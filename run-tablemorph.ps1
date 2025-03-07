# TableMorph Launcher - PowerShell Edition
# To run this script, you may need to set the execution policy:
# Open PowerShell as Administrator and run: Set-ExecutionPolicy -ExecutionPolicy Bypass -Scope Process

# ANSI color codes
$GREEN = "`e[0;32m"
$YELLOW = "`e[1;33m"
$RED = "`e[0;31m"
$BLUE = "`e[0;34m"
$NC = "`e[0m" # No Color

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
    
    # Method 1: .NET WebClient (works on older PowerShell versions)
    if (-not $downloadSuccess) {
        try {
            Write-Host "Trying WebClient download method..."
            $webClient = New-Object System.Net.WebClient
            $webClient.DownloadFile($installerUrl, $installerPath)
            if (Test-Path -Path $installerPath -PathType Leaf) {
                $downloadSuccess = $true
            }
        } catch {
            Write-Host "WebClient download failed. Trying another method..."
        }
    }
    
    # Method 2: Invoke-WebRequest (PowerShell 3.0+)
    if (-not $downloadSuccess) {
        try {
            Write-Host "Trying Invoke-WebRequest download method..."
            Invoke-WebRequest -Uri $installerUrl -OutFile $installerPath -UseBasicParsing
            if (Test-Path -Path $installerPath -PathType Leaf) {
                $downloadSuccess = $true
            }
        } catch {
            Write-Host "Invoke-WebRequest download failed. Trying another method..."
        }
    }
    
    # Method 3: BITS Transfer (Windows only)
    if (-not $downloadSuccess) {
        try {
            Write-Host "Trying BITS transfer download method..."
            Start-BitsTransfer -Source $installerUrl -Destination $installerPath
            if (Test-Path -Path $installerPath -PathType Leaf) {
                $downloadSuccess = $true
            }
        } catch {
            Write-Host "BITS transfer download failed."
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
    
    # Install Java silently
    Write-Host "${YELLOW}Installing Java 17...$NC"
    try {
        $process = Start-Process -FilePath "msiexec.exe" -ArgumentList "/i", "`"$installerPath`"", "/quiet", "/qn", "/norestart" -Wait -PassThru
        if ($process.ExitCode -ne 0) {
            throw "MSI installer returned exit code: $($process.ExitCode)"
        }
    } catch {
        Write-Host "${RED}Error during Java installation: $_$NC"
        Write-Host "${YELLOW}Please install Java manually from: https://adoptium.net/$NC"
        Remove-Item -Path $tempDir -Recurse -Force -ErrorAction SilentlyContinue
        Read-Host "Press Enter to exit"
        exit 1
    }
    
    # Clean up
    Remove-Item -Path $tempDir -Recurse -Force -ErrorAction SilentlyContinue
    
    # Verify Java installation
    Write-Host "${YELLOW}Verifying Java installation...$NC"
    
    # Update PATH to include Java
    $env:Path = [System.Environment]::GetEnvironmentVariable("Path", "Machine") + ";" + [System.Environment]::GetEnvironmentVariable("Path", "User")
    
    # Add potential Java paths
    $javaLocations = @(
        "${env:ProgramFiles}\Eclipse Adoptium\jdk-17.0.8.7-hotspot\bin",
        "${env:ProgramFiles(x86)}\Eclipse Adoptium\jdk-17.0.8.7-hotspot\bin"
    )
    
    foreach ($location in $javaLocations) {
        if (Test-Path -Path $location) {
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
        Write-Host "${YELLOW}Please install Java manually from: https://adoptium.net/$NC"
        Read-Host "Press Enter to exit"
        exit 1
    }
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
        & $mvnwPath clean package assembly:single
        Pop-Location
        
        if (-not (Test-Path -Path $jarFile -PathType Leaf)) {
            Write-Host "${RED}Error: Build failed!$NC"
            Read-Host "Press Enter to exit"
            exit 1
        }
        
        Write-Host "${GREEN}TableMorph built successfully!$NC"
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