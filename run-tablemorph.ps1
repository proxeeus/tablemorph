# TableMorph Launcher - PowerShell Edition

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
    $tempDir = "temp"
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
    
    try {
        Invoke-WebRequest -Uri $installerUrl -OutFile $installerPath
    } catch {
        Write-Host "${RED}Error: Failed to download Java installer.$NC"
        Write-Host "${YELLOW}Please install Java manually from: https://adoptium.net/$NC"
        Remove-Item -Path $tempDir -Recurse -Force -ErrorAction SilentlyContinue
        Read-Host "Press Enter to exit"
        exit 1
    }
    
    # Check if download was successful
    if (-not (Test-Path -Path $installerPath -PathType Leaf)) {
        Write-Host "${RED}Error: Failed to download Java installer.$NC"
        Write-Host "${YELLOW}Please install Java manually from: https://adoptium.net/$NC"
        Remove-Item -Path $tempDir -Recurse -Force -ErrorAction SilentlyContinue
        Read-Host "Press Enter to exit"
        exit 1
    }
    
    # Install Java silently
    Write-Host "${YELLOW}Installing Java 17...$NC"
    Start-Process -FilePath "msiexec.exe" -ArgumentList "/i", $installerPath, "/quiet", "/qn", "/norestart" -Wait
    
    # Clean up
    Remove-Item -Path $tempDir -Recurse -Force -ErrorAction SilentlyContinue
    
    # Verify Java installation
    Write-Host "${YELLOW}Verifying Java installation...$NC"
    
    # Update PATH to include Java
    $env:Path = [System.Environment]::GetEnvironmentVariable("Path", "Machine") + ";" + [System.Environment]::GetEnvironmentVariable("Path", "User")
    
    # Check if Java is now installed
    try {
        $javaVersion = (Get-Command java -ErrorAction Stop | Select-Object -ExpandProperty Version).Major
        
        # Handle different version formats
        if ($javaVersion -eq 1) {
            # For Java 1.8 style version
            $versionOutput = java -version 2>&1
            if ($versionOutput -match "version `"(\d+)\.(\d+)") {
                if ($Matches[1] -eq "1") {
                    $javaVersion = $Matches[2]
                }
            }
        }
        
        if ($javaVersion -lt 8) {
            Write-Host "${RED}Error: Java installation failed or version is still too old.$NC"
            Write-Host "${YELLOW}Please install Java 8 or higher manually from: https://adoptium.net/$NC"
            Read-Host "Press Enter to exit"
            exit 1
        }
        
        Write-Host "${GREEN}Java $javaVersion installed successfully!$NC"
    } catch {
        Write-Host "${RED}Error: Java installation failed.$NC"
        Write-Host "${YELLOW}Please install Java manually from: https://adoptium.net/$NC"
        Read-Host "Press Enter to exit"
        exit 1
    }
}

# Check if Java is installed and has the correct version
Write-Host "${YELLOW}Checking Java installation...$NC"
try {
    $javaVersion = (Get-Command java -ErrorAction Stop | Select-Object -ExpandProperty Version).Major
    
    # Handle different version formats
    if ($javaVersion -eq 1) {
        # For Java 1.8 style version
        $versionOutput = java -version 2>&1
        if ($versionOutput -match "version `"(\d+)\.(\d+)") {
            if ($Matches[1] -eq "1") {
                $javaVersion = $Matches[2]
            }
        }
    }
    
    if ($javaVersion -lt 8) {
        Write-Host "${YELLOW}Java version $javaVersion is too old. Java 8 or higher is required.$NC"
        $installJava = Read-Host "Would you like to install Java 17 automatically? (Y/N)"
        if ($installJava -eq "Y" -or $installJava -eq "y") {
            Install-Java
        } else {
            Write-Host "${RED}Error: Java 8 or higher is required. Found version: $javaVersion$NC"
            Write-Host "${YELLOW}Please install Java 8 or higher to run TableMorph.$NC"
            Write-Host "${YELLOW}You can download it from: https://adoptium.net/$NC"
            Write-Host ""
            Read-Host "Press Enter to exit"
            exit 1
        }
    } else {
        Write-Host "${GREEN}Java $javaVersion detected!$NC"
    }
} catch {
    Write-Host "${YELLOW}Java not found!$NC"
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
$jarFile = "target\tablemorph-1.0-SNAPSHOT-jar-with-dependencies.jar"
if (-not (Test-Path -Path $jarFile -PathType Leaf)) {
    Write-Host "${YELLOW}Building TableMorph...$NC"
    
    # Check if Maven Wrapper exists
    if (Test-Path -Path "mvnw.cmd" -PathType Leaf) {
        & .\mvnw.cmd clean package assembly:single
        
        if (-not (Test-Path -Path $jarFile -PathType Leaf)) {
            Write-Host "${RED}Error: Build failed!$NC"
            Read-Host "Press Enter to exit"
            exit 1
        }
        
        Write-Host "${GREEN}TableMorph built successfully!$NC"
    } else {
        Write-Host "${RED}Error: Maven Wrapper (mvnw.cmd) not found!$NC"
        Write-Host "${YELLOW}Please ensure you've cloned the complete repository.$NC"
        Read-Host "Press Enter to exit"
        exit 1
    }
}

# Launch the application
Write-Host "${YELLOW}Launching TableMorph...$NC"
java -jar $jarFile 