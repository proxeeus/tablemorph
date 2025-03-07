#!/bin/bash

# ANSI color codes
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Print header
echo -e "${BLUE}"
echo "╔════════════════════════════════════════════════════════════╗"
echo "║            TableMorph Launcher - macOS/Linux               ║"
echo "╚════════════════════════════════════════════════════════════╝"
echo -e "${NC}"

# Check if running with sudo/root and warn if needed, but don't require it
if [ "$EUID" -ne 0 ]; then
    echo -e "${YELLOW}Note: Not running with root privileges. Some installation steps may require your password if needed.${NC}"
    echo -e "${YELLOW}If you experience permission issues, you can run the script with sudo.${NC}"
    echo
fi

# Function to display a spinner during a time-consuming process
display_spinner() {
    local pid=$1
    local message=$2
    local spin='-\|/'
    local i=0
    
    echo -ne "${YELLOW}$message ${NC}"
    
    while kill -0 $pid 2>/dev/null; do
        i=$(( (i+1) % 4 ))
        echo -ne "\r${YELLOW}$message [${spin:$i:1}]${NC}"
        sleep 0.5
    done
    
    echo -e "\r${GREEN}$message [Done]${NC}"
}

# Function to install Java
install_java() {
    echo -e "${YELLOW}Java not found or version too old. Installing Java 17...${NC}"
    
    if [[ "$OSTYPE" == "darwin"* ]]; then
        # macOS installation using Homebrew
        if command -v brew >/dev/null 2>&1; then
            echo -e "${YELLOW}Installing Java using Homebrew...${NC}"
            brew install openjdk@17 &
            display_spinner $! "Installing Java 17 via Homebrew"
            
            # Create symlink to make Java available
            if [ -d "/opt/homebrew/opt/openjdk@17/bin" ]; then
                echo -e "${YELLOW}Creating symlink for Java...${NC}"
                sudo ln -sfn /opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk /Library/Java/JavaVirtualMachines/openjdk-17.jdk
                export PATH="/opt/homebrew/opt/openjdk@17/bin:$PATH"
            elif [ -d "/usr/local/opt/openjdk@17/bin" ]; then
                echo -e "${YELLOW}Creating symlink for Java...${NC}"
                sudo ln -sfn /usr/local/opt/openjdk@17/libexec/openjdk.jdk /Library/Java/JavaVirtualMachines/openjdk-17.jdk
                export PATH="/usr/local/opt/openjdk@17/bin:$PATH"
            fi
        else
            echo -e "${YELLOW}Homebrew not found. Installing Homebrew first...${NC}"
            /bin/bash -c "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh)" &
            display_spinner $! "Installing Homebrew"
            
            # Add Homebrew to PATH
            if [[ $(uname -m) == 'arm64' ]]; then
                echo 'eval "$(/opt/homebrew/bin/brew shellenv)"' >> ~/.zprofile
                eval "$(/opt/homebrew/bin/brew shellenv)"
            else
                echo 'eval "$(/usr/local/bin/brew shellenv)"' >> ~/.zprofile
                eval "$(/usr/local/bin/brew shellenv)"
            fi
            
            # Now install Java
            echo -e "${YELLOW}Installing Java using Homebrew...${NC}"
            brew install openjdk@17 &
            display_spinner $! "Installing Java 17 via Homebrew"
            
            # Create symlink to make Java available
            if [ -d "/opt/homebrew/opt/openjdk@17/bin" ]; then
                echo -e "${YELLOW}Creating symlink for Java...${NC}"
                sudo ln -sfn /opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk /Library/Java/JavaVirtualMachines/openjdk-17.jdk
                export PATH="/opt/homebrew/opt/openjdk@17/bin:$PATH"
            elif [ -d "/usr/local/opt/openjdk@17/bin" ]; then
                echo -e "${YELLOW}Creating symlink for Java...${NC}"
                sudo ln -sfn /usr/local/opt/openjdk@17/libexec/openjdk.jdk /Library/Java/JavaVirtualMachines/openjdk-17.jdk
                export PATH="/usr/local/opt/openjdk@17/bin:$PATH"
            fi
        fi
    elif [[ "$OSTYPE" == "linux-gnu"* ]]; then
        # Detect Linux distribution
        if [ -f /etc/os-release ]; then
            . /etc/os-release
            OS=$NAME
        elif type lsb_release >/dev/null 2>&1; then
            OS=$(lsb_release -si)
        elif [ -f /etc/lsb-release ]; then
            . /etc/lsb-release
            OS=$DISTRIB_ID
        else
            OS=$(uname -s)
        fi
        
        # Install Java based on distribution
        case $OS in
            *Ubuntu*|*Debian*)
                echo -e "${YELLOW}Installing Java on Ubuntu/Debian...${NC}"
                sudo apt update & 
                display_spinner $! "Updating package lists"
                sudo apt install -y openjdk-17-jdk &
                display_spinner $! "Installing OpenJDK 17"
                ;;
            *Fedora*|"Red Hat"*|*CentOS*)
                echo -e "${YELLOW}Installing Java on Fedora/RHEL/CentOS...${NC}"
                sudo dnf install -y java-17-openjdk &
                display_spinner $! "Installing OpenJDK 17"
                ;;
            *Arch*)
                echo -e "${YELLOW}Installing Java on Arch Linux...${NC}"
                sudo pacman -Sy jdk17-openjdk &
                display_spinner $! "Installing OpenJDK 17"
                ;;
            *)
                echo -e "${RED}Unsupported Linux distribution: $OS${NC}"
                echo -e "${YELLOW}Please install Java 17 manually and try again.${NC}"
                exit 1
                ;;
        esac
    else
        echo -e "${RED}Unsupported operating system: $OSTYPE${NC}"
        echo -e "${YELLOW}Please install Java 17 manually and try again.${NC}"
        exit 1
    fi
    
    # Verify installation
    if command -v java >/dev/null 2>&1; then
        JAVA_VERSION=$(java -version 2>&1 | head -1 | cut -d'"' -f2 | sed 's/^1\.//' | cut -d'.' -f1)
        if [ "$JAVA_VERSION" -lt 8 ]; then
            echo -e "${RED}Error: Java installation failed or version is still too old.${NC}"
            echo -e "${YELLOW}Please install Java 17 manually and try again.${NC}"
            exit 1
        fi
        echo -e "${GREEN}Java $JAVA_VERSION installed successfully!${NC}"
    else
        echo -e "${RED}Error: Java installation failed.${NC}"
        echo -e "${YELLOW}Please install Java 17 manually and try again.${NC}"
        exit 1
    fi
}

# Check if Java is installed and has the correct version
echo -e "${YELLOW}Checking Java installation...${NC}"
if command -v java >/dev/null 2>&1; then
    JAVA_VERSION=$(java -version 2>&1 | head -1 | cut -d'"' -f2 | sed 's/^1\.//' | cut -d'.' -f1)
    if [ "$JAVA_VERSION" -lt 8 ]; then
        echo -e "${YELLOW}Java version $JAVA_VERSION is too old. Java 8 or higher is required.${NC}"
        install_java
    else
        echo -e "${GREEN}Java $JAVA_VERSION detected!${NC}"
    fi
else
    echo -e "${YELLOW}Java not found!${NC}"
    install_java
fi

# Define target directory and JAR file path
TARGET_DIR="target"
JAR_NAME="tablemorph-1.0-SNAPSHOT-jar-with-dependencies.jar"
JAR_PATH="$TARGET_DIR/$JAR_NAME"

# Create target directory if it doesn't exist
if [ ! -d "$TARGET_DIR" ]; then
    echo -e "${YELLOW}Creating target directory...${NC}"
    mkdir -p "$TARGET_DIR"
    echo -e "${GREEN}Target directory created!${NC}"
fi

# Check if the JAR file exists, build if not
if [ ! -f "$JAR_PATH" ]; then
    echo -e "${YELLOW}JAR file not found. Building TableMorph...${NC}"
    
    # Check if Maven wrapper exists
    if [ ! -f "./mvnw" ]; then
        echo -e "${RED}Error: Maven wrapper (mvnw) not found!${NC}"
        echo -e "${YELLOW}Please ensure you've cloned the complete repository.${NC}"
        exit 1
    fi
    
    # Check if mvnw is executable, make it executable if not
    if [ ! -x "./mvnw" ]; then
        echo -e "${YELLOW}Making Maven wrapper executable...${NC}"
        chmod +x ./mvnw
    fi
    
    # Build the project using Maven Wrapper
    echo -e "${YELLOW}Running Maven build...${NC}"
    ./mvnw clean package assembly:single
    
    # Check if build was successful
    if [ $? -ne 0 ]; then
        echo -e "${RED}Error: Maven build failed!${NC}"
        echo -e "${YELLOW}Please check the build output for errors.${NC}"
        exit 1
    fi
    
    # Verify JAR was created
    if [ ! -f "$JAR_PATH" ]; then
        echo -e "${RED}Error: Build completed but JAR file was not created.${NC}"
        echo -e "${YELLOW}Please check the build output for errors.${NC}"
        exit 1
    fi
    
    echo -e "${GREEN}TableMorph built successfully!${NC}"
fi

# Launch the application
echo
echo -e "${YELLOW}Launching TableMorph...${NC}"
java -jar "$JAR_PATH"

# Check if application launched successfully
if [ $? -ne 0 ]; then
    echo
    echo -e "${RED}Error: Failed to launch TableMorph.${NC}"
    echo
    exit 1
fi

# Exit with success
exit 0 