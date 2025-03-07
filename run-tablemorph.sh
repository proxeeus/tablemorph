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

# Function to install Java
install_java() {
    echo -e "${YELLOW}Java not found or version too old. Attempting to install Java 17...${NC}"
    
    if [[ "$OSTYPE" == "darwin"* ]]; then
        # macOS installation using Homebrew
        if command -v brew >/dev/null 2>&1; then
            echo -e "${YELLOW}Installing Java using Homebrew...${NC}"
            brew install openjdk@17
            
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
            /bin/bash -c "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh)"
            
            # Add Homebrew to PATH
            if [[ $(uname -m) == 'arm64' ]]; then
                echo 'eval "$(/opt/homebrew/bin/brew shellenv)"' >> ~/.zprofile
                eval "$(/opt/homebrew/bin/brew shellenv)"
            else
                echo 'eval "$(/usr/local/bin/brew shellenv)"' >> ~/.zprofile
                eval "$(/usr/local/bin/brew shellenv)"
            fi
            
            # Now install Java
            brew install openjdk@17
            
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
                sudo apt update
                sudo apt install -y openjdk-17-jdk
                ;;
            *Fedora*|*Red Hat*|*CentOS*)
                echo -e "${YELLOW}Installing Java on Fedora/RHEL/CentOS...${NC}"
                sudo dnf install -y java-17-openjdk
                ;;
            *Arch*)
                echo -e "${YELLOW}Installing Java on Arch Linux...${NC}"
                sudo pacman -Sy jdk17-openjdk
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
        read -p "Would you like to install Java 17 automatically? (y/n): " -n 1 -r
        echo
        if [[ $REPLY =~ ^[Yy]$ ]]; then
            install_java
        else
            echo -e "${YELLOW}Please install Java 8 or higher manually and try again.${NC}"
            exit 1
        fi
    else
        echo -e "${GREEN}Java $JAVA_VERSION detected!${NC}"
    fi
else
    echo -e "${YELLOW}Java not found!${NC}"
    read -p "Would you like to install Java 17 automatically? (y/n): " -n 1 -r
    echo
    if [[ $REPLY =~ ^[Yy]$ ]]; then
        install_java
    else
        echo -e "${YELLOW}Please install Java 8 or higher manually and try again.${NC}"
        
        # Suggest installation commands based on OS
        if [[ "$OSTYPE" == "darwin"* ]]; then
            echo -e "${YELLOW}For macOS, you can install Java using Homebrew:${NC}"
            echo "  brew install openjdk@17"
        elif [[ "$OSTYPE" == "linux-gnu"* ]]; then
            echo -e "${YELLOW}For Ubuntu/Debian, you can install Java using:${NC}"
            echo "  sudo apt update && sudo apt install openjdk-17-jdk"
            echo -e "${YELLOW}For Fedora/RHEL/CentOS:${NC}"
            echo "  sudo dnf install java-17-openjdk"
        fi
        
        exit 1
    fi
fi

# Create wavetables directory if it doesn't exist
if [ ! -d "wavetables" ]; then
    echo -e "${YELLOW}Creating wavetables directory...${NC}"
    mkdir -p wavetables
    echo -e "${GREEN}Wavetables directory created!${NC}"
fi

# Check if the JAR file exists, build if not
JAR_FILE="target/tablemorph-1.0-SNAPSHOT-jar-with-dependencies.jar"
if [ ! -f "$JAR_FILE" ]; then
    echo -e "${YELLOW}Building TableMorph...${NC}"
    
    # Check if mvnw is executable, make it executable if not
    if [ ! -x "./mvnw" ]; then
        chmod +x ./mvnw
    fi
    
    # Build the project using Maven Wrapper
    ./mvnw clean package assembly:single
    
    if [ ! -f "$JAR_FILE" ]; then
        echo -e "${RED}Error: Build failed!${NC}"
        exit 1
    fi
    
    echo -e "${GREEN}TableMorph built successfully!${NC}"
fi

# Launch the application
echo -e "${YELLOW}Launching TableMorph...${NC}"
java -jar "$JAR_FILE" 