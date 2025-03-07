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

# Check if Java is installed and has the correct version
echo -e "${YELLOW}Checking Java installation...${NC}"
if command -v java >/dev/null 2>&1; then
    JAVA_VERSION=$(java -version 2>&1 | head -1 | cut -d'"' -f2 | sed 's/^1\.//' | cut -d'.' -f1)
    if [ "$JAVA_VERSION" -lt 8 ]; then
        echo -e "${RED}Error: Java 8 or higher is required. Found version: $JAVA_VERSION${NC}"
        echo -e "${YELLOW}Please install Java 8 or higher to run TableMorph.${NC}"
        exit 1
    fi
    echo -e "${GREEN}Java $JAVA_VERSION detected!${NC}"
else
    echo -e "${RED}Error: Java not found!${NC}"
    echo -e "${YELLOW}Please install Java 8 or higher to run TableMorph.${NC}"
    
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